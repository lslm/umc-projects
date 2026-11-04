package dev.lslm.library.controller;

import dev.lslm.library.dto.NotificacaoDtos.ComunicadoRequest;
import dev.lslm.library.dto.NotificacaoDtos.NotificacaoResponse;
import dev.lslm.library.dto.NotificacaoDtos.VerificacaoPrazosResponse;
import dev.lslm.library.service.NotificacaoService;
import dev.lslm.library.service.PrazoService;
import dev.lslm.library.support.RequestContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** RF13 - comunicados, verificacao de prazos e leitura de notificacoes. */
@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;
    private final PrazoService prazoService;

    public NotificacaoController(NotificacaoService service, PrazoService prazoService) {
        this.service = service;
        this.prazoService = prazoService;
    }

    /** Sem leitorId, o comunicado vai para todos os leitores da biblioteca. */
    @PostMapping("/comunicados")
    public ResponseEntity<Map<String, Object>> comunicado(@RequestBody ComunicadoRequest req) {
        int destinatarios = service.comunicado(req);
        return ResponseEntity.accepted().body(Map.of("destinatarios", destinatarios));
    }

    /** Executa agora a mesma verificacao que a rotina diaria faz (vencimentos, atrasos, reservas expiradas). */
    @PostMapping("/verificar-prazos")
    public VerificacaoPrazosResponse verificarPrazos() {
        return prazoService.verificar(RequestContext.bibliotecaId());
    }

    @PostMapping("/{id}/leitura")
    public NotificacaoResponse marcarLida(@PathVariable UUID id) {
        return NotificacaoResponse.from(service.marcarLida(id));
    }
}
