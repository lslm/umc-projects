package dev.lslm.library.service;

import dev.lslm.library.domain.Usuario;
import dev.lslm.library.domain.Usuario.Perfil;
import dev.lslm.library.dto.AuthDtos.UsuarioRequest;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.LeitorRepository;
import dev.lslm.library.repository.UsuarioRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.SenhaHasher;
import dev.lslm.library.support.Validacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Usuarios e perfis (RF03). Administrador da plataforma nao pertence a nenhuma biblioteca. */
@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository repository;
    private final LeitorRepository leitorRepository;
    private final AuditService audit;

    public UsuarioService(UsuarioRepository repository, LeitorRepository leitorRepository, AuditService audit) {
        this.repository = repository;
        this.leitorRepository = leitorRepository;
        this.audit = audit;
    }

    public Usuario criar(UsuarioRequest req) {
        String nome = Validacao.texto(req.nome(), "nome");
        String email = Validacao.texto(req.email(), "email").toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("O campo 'email' é inválido.");
        }
        validarSenha(req.senha());
        Perfil perfil = Validacao.obrigatorio(req.perfil(), "perfil");
        if (repository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_JA_CADASTRADO", "Já existe um usuário com o e-mail " + email + ".");
        }

        UUID bib = perfil == Perfil.ADMIN_PLATAFORMA ? null : RequestContext.bibliotecaId();
        UUID leitorId = null;
        if (perfil == Perfil.LEITOR && req.leitorId() != null) {
            leitorId = leitorRepository.findByIdAndBibliotecaId(req.leitorId(), bib)
                    .orElseThrow(() -> ApiException.notFound("LEITOR_NAO_ENCONTRADO", "Leitor não encontrado."))
                    .getId();
        }
        Usuario u = repository.save(new Usuario(bib, nome, email, SenhaHasher.hash(req.senha()), perfil, leitorId));
        audit.registrarPara(bib, "USUARIO_CRIADO", "Usuario", u.getId(), "perfil=" + perfil + "; email=" + email);
        return u;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.findByBibliotecaIdOrderByNome(RequestContext.bibliotecaId());
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    public Usuario atualizar(UUID id, UsuarioRequest req) {
        Usuario u = buscar(id);
        if (req.nome() != null) {
            u.setNome(Validacao.texto(req.nome(), "nome"));
        }
        if (req.perfil() != null) {
            if (req.perfil() == Perfil.ADMIN_PLATAFORMA) {
                throw ApiException.badRequest("PERFIL_INVALIDO",
                        "O perfil de administrador da plataforma não pode ser atribuído dentro de uma biblioteca.");
            }
            u.setPerfil(req.perfil());
        }
        if (req.ativo() != null) {
            u.setAtivo(req.ativo());
        }
        if (req.senha() != null) {
            validarSenha(req.senha());
            u.setSenhaHash(SenhaHasher.hash(req.senha()));
        }
        audit.registrar("USUARIO_ATUALIZADO", "Usuario", u.getId(), "perfil=" + u.getPerfil() + "; ativo=" + u.isAtivo());
        return u;
    }

    static void validarSenha(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }
    }
}
