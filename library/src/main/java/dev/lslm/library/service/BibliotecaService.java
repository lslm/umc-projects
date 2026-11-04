package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Biblioteca.Plano;
import dev.lslm.library.dto.BibliotecaDtos.BibliotecaRequest;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.support.Validacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** Gestao das bibliotecas contratantes (RF01). */
@Service
@Transactional
public class BibliotecaService {

    private final BibliotecaRepository repository;
    private final AuditService audit;

    public BibliotecaService(BibliotecaRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    public Biblioteca criar(BibliotecaRequest req) {
        Biblioteca b = new Biblioteca(Validacao.texto(req.nome(), "nome"),
                req.plano() != null ? req.plano() : Plano.BASICO);
        aplicar(b, req);
        b = repository.save(b);
        audit.registrarPara(b.getId(), "BIBLIOTECA_CRIADA", "Biblioteca", b.getId(),
                "nome=" + b.getNome() + "; plano=" + b.getPlano());
        return b;
    }

    @Transactional(readOnly = true)
    public List<Biblioteca> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Biblioteca buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("BIBLIOTECA_NAO_ENCONTRADA", "Biblioteca não encontrada."));
    }

    public Biblioteca atualizar(UUID id, BibliotecaRequest req) {
        Biblioteca b = buscar(id);
        if (req.nome() != null) {
            b.setNome(Validacao.texto(req.nome(), "nome"));
        }
        if (req.plano() != null && req.plano() != b.getPlano()) {
            b.aplicarPlano(req.plano());
        }
        aplicar(b, req);
        audit.registrarPara(b.getId(), "BIBLIOTECA_ATUALIZADA", "Biblioteca", b.getId(), "plano=" + b.getPlano());
        return b;
    }

    public Biblioteca alterarAtivacao(UUID id, boolean ativa) {
        Biblioteca b = buscar(id);
        b.setAtiva(ativa);
        audit.registrarPara(b.getId(), ativa ? "BIBLIOTECA_ATIVADA" : "BIBLIOTECA_DESATIVADA", "Biblioteca",
                b.getId(), null);
        return b;
    }

    private void aplicar(Biblioteca b, BibliotecaRequest req) {
        if (req.email() != null) {
            b.setEmail(req.email());
        }
        if (req.limiteLeitores() != null) {
            b.setLimiteLeitores(positivo(req.limiteLeitores(), "limiteLeitores"));
        }
        if (req.limiteExemplares() != null) {
            b.setLimiteExemplares(positivo(req.limiteExemplares(), "limiteExemplares"));
        }
        if (req.diasEmprestimo() != null) {
            b.setDiasEmprestimo(positivo(req.diasEmprestimo(), "diasEmprestimo"));
        }
        if (req.limiteItensEmprestimo() != null) {
            b.setLimiteItensEmprestimo(positivo(req.limiteItensEmprestimo(), "limiteItensEmprestimo"));
        }
        if (req.limiteRenovacoes() != null) {
            b.setLimiteRenovacoes(naoNegativo(req.limiteRenovacoes(), "limiteRenovacoes"));
        }
        if (req.multaDiaria() != null) {
            if (req.multaDiaria().signum() < 0) {
                throw new IllegalArgumentException("multaDiaria não pode ser negativa.");
            }
            b.setMultaDiaria(req.multaDiaria().setScale(2, RoundingMode.HALF_UP));
        }
        if (req.diasAvisoVencimento() != null) {
            b.setDiasAvisoVencimento(naoNegativo(req.diasAvisoVencimento(), "diasAvisoVencimento"));
        }
        if (req.diasRetiradaReserva() != null) {
            b.setDiasRetiradaReserva(positivo(req.diasRetiradaReserva(), "diasRetiradaReserva"));
        }
    }

    private static int positivo(int v, String campo) {
        if (v < 1) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
        return v;
    }

    private static int naoNegativo(int v, String campo) {
        if (v < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
        return v;
    }
}
