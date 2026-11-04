package dev.lslm.library.service;

import dev.lslm.library.repository.BibliotecaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Rotina diaria que roda a verificacao de prazos de todas as bibliotecas ativas. */
@Component
public class PrazoScheduler {

    private final BibliotecaRepository bibliotecaRepository;
    private final PrazoService prazoService;

    public PrazoScheduler(BibliotecaRepository bibliotecaRepository, PrazoService prazoService) {
        this.bibliotecaRepository = bibliotecaRepository;
        this.prazoService = prazoService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    public void verificarTodas() {
        bibliotecaRepository.findByAtivaTrue().forEach(b -> {
            try {
                prazoService.verificar(b.getId());
            } catch (RuntimeException e) {
                System.err.println("Falha ao verificar prazos da biblioteca " + b.getId() + ": " + e.getMessage());
            }
        });
    }
}
