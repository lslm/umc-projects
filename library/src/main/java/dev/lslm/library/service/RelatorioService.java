package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Book;
import dev.lslm.library.domain.Emprestimo;
import dev.lslm.library.domain.Emprestimo.Status;
import dev.lslm.library.domain.Leitor;
import dev.lslm.library.dto.RelatorioDtos.Atraso;
import dev.lslm.library.dto.RelatorioDtos.Circulacao;
import dev.lslm.library.dto.RelatorioDtos.LeitorAtivo;
import dev.lslm.library.dto.RelatorioDtos.ObraEmprestada;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.EmprestimoRepository;
import dev.lslm.library.repository.LeitorRepository;
import dev.lslm.library.support.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Relatorios gerenciais (RF15). Periodo padrao: ultimos 30 dias; "ate" e exclusivo. */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final EmprestimoRepository emprestimoRepository;
    private final BookRepository bookRepository;
    private final LeitorRepository leitorRepository;
    private final BibliotecaRepository bibliotecaRepository;
    private final Clock clock;

    public RelatorioService(EmprestimoRepository emprestimoRepository, BookRepository bookRepository,
                            LeitorRepository leitorRepository, BibliotecaRepository bibliotecaRepository,
                            Clock clock) {
        this.emprestimoRepository = emprestimoRepository;
        this.bookRepository = bookRepository;
        this.leitorRepository = leitorRepository;
        this.bibliotecaRepository = bibliotecaRepository;
        this.clock = clock;
    }

    public Circulacao circulacao(Instant de, Instant ate) {
        UUID bib = RequestContext.bibliotecaId();
        Instant fim = fim(ate);
        Instant ini = inicio(de, fim);
        List<Emprestimo> doPeriodo = emprestimosNoPeriodo(bib, ini, fim);
        Instant agora = clock.instant();
        List<Emprestimo> ativos = emprestimoRepository.findByBibliotecaIdAndStatus(bib, Status.ATIVO);

        Map<LocalDate, Long> porDia = doPeriodo.stream().collect(Collectors.groupingBy(
                e -> e.getEmprestadoEm().atOffset(ZoneOffset.UTC).toLocalDate(), TreeMap::new, Collectors.counting()));
        return new Circulacao(ini, fim, doPeriodo.size(),
                emprestimoRepository.countByBibliotecaIdAndStatusAndDevolvidoEmGreaterThanEqualAndDevolvidoEmLessThan(
                        bib, Status.DEVOLVIDO, ini, fim),
                doPeriodo.stream().mapToLong(Emprestimo::getRenovacoes).sum(),
                ativos.size(), ativos.stream().filter(e -> e.getVencimentoEm().isBefore(agora)).count(), porDia);
    }

    public List<Atraso> atrasos() {
        UUID bib = RequestContext.bibliotecaId();
        Instant agora = clock.instant();
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        List<Emprestimo> atrasados = emprestimoRepository.findByBibliotecaIdAndStatus(bib, Status.ATIVO).stream()
                .filter(e -> e.getVencimentoEm().isBefore(agora)).toList();
        Map<UUID, String> leitores = nomesLeitores(atrasados.stream().map(Emprestimo::getLeitorId).toList());
        Map<UUID, Book> livros = livros(atrasados.stream().map(Emprestimo::getLivroId).toList());
        return atrasados.stream().map(e -> {
            long dias = Emprestimo.calcularDiasAtraso(e.getVencimentoEm(), agora);
            return new Atraso(e.getId(), e.getLeitorId(), leitores.get(e.getLeitorId()), e.getLivroId(),
                    livros.containsKey(e.getLivroId()) ? livros.get(e.getLivroId()).getTitle() : null,
                    e.getExemplarId(), e.getVencimentoEm(), dias,
                    cfg.getMultaDiaria().multiply(BigDecimal.valueOf(dias)));
        }).sorted(Comparator.comparingLong(Atraso::diasAtraso).reversed()).toList();
    }

    public List<LeitorAtivo> leitoresAtivos(Instant de, Instant ate) {
        UUID bib = RequestContext.bibliotecaId();
        Instant fim = fim(ate);
        List<Emprestimo> lista = emprestimosNoPeriodo(bib, inicio(de, fim), fim);
        Map<UUID, Long> contagem = lista.stream()
                .collect(Collectors.groupingBy(Emprestimo::getLeitorId, Collectors.counting()));
        Map<UUID, String> nomes = nomesLeitores(contagem.keySet());
        return contagem.entrySet().stream()
                .map(en -> new LeitorAtivo(en.getKey(), nomes.get(en.getKey()), en.getValue()))
                .sorted(Comparator.comparingLong(LeitorAtivo::emprestimos).reversed()).toList();
    }

    public List<ObraEmprestada> obrasMaisEmprestadas(Instant de, Instant ate, int limite) {
        UUID bib = RequestContext.bibliotecaId();
        Instant fim = fim(ate);
        List<Emprestimo> lista = emprestimosNoPeriodo(bib, inicio(de, fim), fim);
        Map<UUID, Long> contagem = lista.stream()
                .collect(Collectors.groupingBy(Emprestimo::getLivroId, Collectors.counting()));
        Map<UUID, Book> livros = livros(contagem.keySet());
        return contagem.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(Math.max(1, limite))
                .map(en -> {
                    Book b = livros.get(en.getKey());
                    return new ObraEmprestada(en.getKey(), b != null ? b.getTitle() : null,
                            b != null ? b.getAuthor() : null, en.getValue());
                }).toList();
    }

    private List<Emprestimo> emprestimosNoPeriodo(UUID bib, Instant ini, Instant fim) {
        return emprestimoRepository.findByBibliotecaIdAndEmprestadoEmGreaterThanEqualAndEmprestadoEmLessThan(bib, ini, fim);
    }

    private Instant fim(Instant ate) {
        return ate != null ? ate : clock.instant().plusSeconds(1);
    }

    private Instant inicio(Instant de, Instant fim) {
        return de != null ? de : fim.minus(30, ChronoUnit.DAYS);
    }

    private Map<UUID, String> nomesLeitores(java.util.Collection<UUID> ids) {
        return leitorRepository.findAllById(ids).stream().collect(Collectors.toMap(Leitor::getId, Leitor::getNome));
    }

    private Map<UUID, Book> livros(java.util.Collection<UUID> ids) {
        return bookRepository.findAllById(ids).stream().collect(Collectors.toMap(Book::getId, Function.identity()));
    }
}
