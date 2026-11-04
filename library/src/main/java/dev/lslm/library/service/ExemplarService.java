package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Book;
import dev.lslm.library.domain.Exemplar;
import dev.lslm.library.domain.Exemplar.Estado;
import dev.lslm.library.domain.Exemplar.Situacao;
import dev.lslm.library.dto.AcervoDtos.ExemplarRequest;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.EmprestimoRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import dev.lslm.library.support.Validacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Controle de exemplares fisicos (RF05). */
@Service
@Transactional
public class ExemplarService {

    private final ExemplarRepository repository;
    private final BookRepository bookRepository;
    private final BibliotecaRepository bibliotecaRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaService reservaService;
    private final AuditService audit;

    public ExemplarService(ExemplarRepository repository, BookRepository bookRepository,
                           BibliotecaRepository bibliotecaRepository, EmprestimoRepository emprestimoRepository,
                           ReservaService reservaService, AuditService audit) {
        this.repository = repository;
        this.bookRepository = bookRepository;
        this.bibliotecaRepository = bibliotecaRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.reservaService = reservaService;
        this.audit = audit;
    }

    public Exemplar criar(ExemplarRequest req) {
        UUID bib = RequestContext.bibliotecaId();
        UUID livroId = Validacao.obrigatorio(req.livroId(), "livroId");
        String codigo = Validacao.texto(req.codigoBarras(), "codigoBarras");

        Book livro = bookRepository.findByIdAndBibliotecaId(livroId, bib)
                .orElseThrow(() -> ApiException.notFound("LIVRO_NAO_ENCONTRADO", "Livro não encontrado."));
        if (livro.isArchived()) {
            throw ApiException.conflict("LIVRO_ARQUIVADO", "Não é possível adicionar exemplares a uma obra arquivada.");
        }
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        if (repository.countByBibliotecaId(bib) >= cfg.getLimiteExemplares()) {
            throw ApiException.conflict("LIMITE_PLANO_EXCEDIDO",
                    "Limite de " + cfg.getLimiteExemplares() + " exemplares do plano atingido.");
        }
        if (repository.existsByBibliotecaIdAndCodigoBarras(bib, codigo)) {
            throw ApiException.conflict("CODIGO_BARRAS_DUPLICADO", "Já existe um exemplar com o código " + codigo + ".");
        }
        Situacao inicial = req.situacao() != null ? req.situacao() : Situacao.DISPONIVEL;
        validarSituacaoManual(inicial);

        Exemplar ex = new Exemplar(bib, livroId, codigo, req.localizacao(),
                req.estado() != null ? req.estado() : Estado.BOM);
        ex.setSituacao(inicial);
        ex = repository.save(ex);
        if (inicial == Situacao.DISPONIVEL) {
            reservaService.atribuirExemplar(ex); // pode ser separado para a fila de reservas
        }
        audit.registrar("EXEMPLAR_CRIADO", "Exemplar", ex.getId(), "codigo=" + codigo + "; livro=" + livroId);
        return ex;
    }

    @Transactional(readOnly = true)
    public Page<Exemplar> listar(UUID livroId, Situacao situacao, Pageable pageable) {
        Specification<Exemplar> spec = Specs.<Exemplar>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("livroId", livroId))
                .and(Specs.eq("situacao", situacao));
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Exemplar buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("EXEMPLAR_NAO_ENCONTRADO", "Exemplar não encontrado."));
    }

    public Exemplar atualizar(UUID id, ExemplarRequest req) {
        Exemplar ex = repository.bloquear(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("EXEMPLAR_NAO_ENCONTRADO", "Exemplar não encontrado."));
        if (req.localizacao() != null) {
            ex.setLocalizacao(req.localizacao());
        }
        if (req.estado() != null) {
            ex.setEstado(req.estado());
        }
        Situacao nova = req.situacao();
        if (nova != null && nova != ex.getSituacao()) {
            validarSituacaoManual(nova);
            if (ex.getSituacao() == Situacao.EMPRESTADO || ex.getSituacao() == Situacao.RESERVADO) {
                throw ApiException.conflict("EXEMPLAR_EM_USO",
                        "Exemplar emprestado ou reservado: registre a devolução ou cancele a reserva antes.");
            }
            ex.setSituacao(nova);
            if (nova == Situacao.DISPONIVEL) {
                reservaService.atribuirExemplar(ex);
            }
        }
        audit.registrar("EXEMPLAR_ATUALIZADO", "Exemplar", id,
                "situacao=" + ex.getSituacao() + "; estado=" + ex.getEstado());
        return ex;
    }

    public void remover(UUID id) {
        Exemplar ex = buscar(id);
        if (emprestimoRepository.existsByBibliotecaIdAndExemplarId(ex.getBibliotecaId(), id)
                || ex.getSituacao() == Situacao.RESERVADO) {
            throw ApiException.conflict("EXEMPLAR_COM_HISTORICO",
                    "O exemplar possui histórico de empréstimos; altere a situação para BAIXADO.");
        }
        repository.delete(ex);
        audit.registrar("EXEMPLAR_REMOVIDO", "Exemplar", id, "codigo=" + ex.getCodigoBarras());
    }

    private static void validarSituacaoManual(Situacao s) {
        if (s == Situacao.EMPRESTADO || s == Situacao.RESERVADO) {
            throw ApiException.badRequest("SITUACAO_INVALIDA",
                    "As situações EMPRESTADO e RESERVADO são controladas pelo sistema.");
        }
    }
}
