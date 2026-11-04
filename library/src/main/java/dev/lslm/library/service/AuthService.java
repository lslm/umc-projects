package dev.lslm.library.service;

import dev.lslm.library.domain.Sessao;
import dev.lslm.library.domain.TokenRecuperacao;
import dev.lslm.library.domain.Usuario;
import dev.lslm.library.domain.Usuario.Perfil;
import dev.lslm.library.dto.AuthDtos.PerfilResponse;
import dev.lslm.library.dto.AuthDtos.TokenResponse;
import dev.lslm.library.dto.AuthDtos.UsuarioResponse;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.SessaoRepository;
import dev.lslm.library.repository.TokenRecuperacaoRepository;
import dev.lslm.library.repository.UsuarioRepository;
import dev.lslm.library.support.SenhaHasher;
import dev.lslm.library.support.Validacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Login, sessao com refresh token revogavel e recuperacao de senha (RF02). */
@Service
@Transactional
public class AuthService {

    static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    static final Duration REFRESH_TTL = Duration.ofDays(7);
    static final Duration RECUPERACAO_TTL = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository;
    private final TokenRecuperacaoRepository tokenRepository;
    private final BibliotecaRepository bibliotecaRepository;
    private final EmailService emailService;
    private final AuditService audit;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarioRepository, SessaoRepository sessaoRepository,
                       TokenRecuperacaoRepository tokenRepository, BibliotecaRepository bibliotecaRepository,
                       EmailService emailService, AuditService audit, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.tokenRepository = tokenRepository;
        this.bibliotecaRepository = bibliotecaRepository;
        this.emailService = emailService;
        this.audit = audit;
        this.clock = clock;
    }

    public TokenResponse login(String email, String senha) {
        Usuario u = usuarioRepository.findByEmailIgnoreCase(Validacao.texto(email, "email"))
                .filter(Usuario::isAtivo)
                .filter(x -> senha != null && SenhaHasher.confere(senha, x.getSenhaHash()))
                .orElseThrow(() -> ApiException.unauthorized("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos."));
        if (u.getBibliotecaId() != null && !bibliotecaRepository.findById(u.getBibliotecaId())
                .map(b -> b.isAtiva()).orElse(false)) {
            throw ApiException.forbidden("BIBLIOTECA_INATIVA", "Biblioteca desativada.");
        }
        TokenResponse token = novaSessao(u);
        audit.registrarPara(u.getBibliotecaId(), "LOGIN", "Usuario", u.getId(), null);
        return token;
    }

    public TokenResponse renovar(String refreshToken) {
        Sessao atual = sessaoRepository.findByRefreshToken(Validacao.texto(refreshToken, "refreshToken"))
                .filter(s -> !s.isRevogada() && s.getRefreshExpiraEm().isAfter(clock.instant()))
                .orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALIDO",
                        "Refresh token inválido, expirado ou revogado."));
        Usuario u = usuarioRepository.findById(atual.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> ApiException.unauthorized("USUARIO_INATIVO", "Usuário inativo."));
        atual.revogar(); // rotacao: o refresh token anterior deixa de valer
        return novaSessao(u);
    }

    public void logout(String refreshToken) {
        sessaoRepository.findByRefreshToken(Validacao.texto(refreshToken, "refreshToken")).ifPresent(s -> {
            s.revogar();
            audit.registrar("LOGOUT", "Usuario", s.getUsuarioId(), null);
        });
    }

    @Transactional(readOnly = true)
    public UsuarioResponse sessaoAtual(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ApiException.unauthorized("TOKEN_AUSENTE", "Informe Authorization: Bearer <access_token>.");
        }
        Sessao s = sessaoRepository.findByAccessToken(authorization.substring(7).trim())
                .filter(x -> !x.isRevogada() && x.getAccessExpiraEm().isAfter(clock.instant()))
                .orElseThrow(() -> ApiException.unauthorized("TOKEN_INVALIDO", "Token inválido ou expirado."));
        return usuarioRepository.findById(s.getUsuarioId()).map(UsuarioResponse::from)
                .orElseThrow(() -> ApiException.unauthorized("TOKEN_INVALIDO", "Token inválido ou expirado."));
    }

    /** Responde sempre igual, exista ou nao o e-mail, para nao revelar cadastros. */
    public void esqueciSenha(String email) {
        usuarioRepository.findByEmailIgnoreCase(Validacao.texto(email, "email")).filter(Usuario::isAtivo).ifPresent(u -> {
            String token = SenhaHasher.tokenAleatorio();
            tokenRepository.save(new TokenRecuperacao(u.getId(), token, clock.instant().plus(RECUPERACAO_TTL)));
            emailService.enviar(u.getEmail(), "Recuperação de senha",
                    "Use este token para redefinir sua senha (válido por 1h): " + token);
            audit.registrarPara(u.getBibliotecaId(), "SENHA_RECUPERACAO_SOLICITADA", "Usuario", u.getId(), null);
        });
    }

    public void redefinirSenha(String token, String novaSenha) {
        UsuarioService.validarSenha(novaSenha);
        TokenRecuperacao t = tokenRepository.findByToken(Validacao.texto(token, "token"))
                .filter(x -> !x.isUsado() && x.getExpiraEm().isAfter(clock.instant()))
                .orElseThrow(() -> ApiException.badRequest("TOKEN_RECUPERACAO_INVALIDO",
                        "Token de recuperação inválido ou expirado."));
        Usuario u = usuarioRepository.findById(t.getUsuarioId()).orElseThrow();
        u.setSenhaHash(SenhaHasher.hash(novaSenha));
        t.marcarUsado();
        sessaoRepository.findByUsuarioIdAndRevogadaFalse(u.getId()).forEach(Sessao::revogar);
        audit.registrarPara(u.getBibliotecaId(), "SENHA_REDEFINIDA", "Usuario", u.getId(), null);
    }

    @Transactional(readOnly = true)
    public List<PerfilResponse> perfis() {
        return List.of(
                new PerfilResponse(Perfil.ADMIN_PLATAFORMA, "Gerencia bibliotecas contratantes, planos e suporte operacional.",
                        List.of("Gerenciar bibliotecas", "Gerenciar planos", "Suporte operacional")),
                new PerfilResponse(Perfil.ADMIN_BIBLIOTECA, "Gerencia usuários internos, regras, catálogo e assinatura da própria biblioteca.",
                        List.of("Gerenciar usuários", "Configurar regras", "Gerenciar catálogo", "Gerenciar assinatura", "Relatórios")),
                new PerfilResponse(Perfil.BIBLIOTECARIO, "Opera acervo, exemplares, empréstimos, devoluções e reservas.",
                        List.of("Manter acervo", "Controlar exemplares", "Emprestar e devolver", "Gerenciar reservas e multas", "Relatórios")),
                new PerfilResponse(Perfil.LEITOR, "Consulta catálogo, gerencia próprio perfil, empréstimos e reservas.",
                        List.of("Consultar catálogo", "Reservar obras", "Ver empréstimos e notificações")));
    }

    private TokenResponse novaSessao(Usuario u) {
        Instant agora = clock.instant();
        Sessao s = sessaoRepository.save(new Sessao(u.getId(), SenhaHasher.tokenAleatorio(),
                SenhaHasher.tokenAleatorio(), agora.plus(ACCESS_TTL), agora.plus(REFRESH_TTL)));
        return new TokenResponse(s.getAccessToken(), s.getRefreshToken(), "Bearer", ACCESS_TTL.toSeconds(),
                UsuarioResponse.from(u));
    }
}
