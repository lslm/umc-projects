package dev.lslm.library.service;

import dev.lslm.library.domain.Emprestimo.Status;
import dev.lslm.library.domain.Leitor;
import dev.lslm.library.dto.CirculacaoDtos.EmprestimoResponse;
import dev.lslm.library.dto.CirculacaoDtos.MultaResponse;
import dev.lslm.library.dto.LeitorDtos.AreaLeitorResponse;
import dev.lslm.library.dto.LeitorDtos.LeitorResponse;
import dev.lslm.library.dto.NotificacaoDtos.NotificacaoResponse;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Area do leitor (RF14): emprestimos atuais, historico, reservas, multas e notificacoes. */
@Service
@Transactional(readOnly = true)
public class AreaLeitorService {

    private final LeitorService leitorService;
    private final EmprestimoService emprestimoService;
    private final ReservaService reservaService;
    private final MultaService multaService;
    private final NotificacaoService notificacaoService;

    public AreaLeitorService(LeitorService leitorService, EmprestimoService emprestimoService,
                             ReservaService reservaService, MultaService multaService,
                             NotificacaoService notificacaoService) {
        this.leitorService = leitorService;
        this.emprestimoService = emprestimoService;
        this.reservaService = reservaService;
        this.multaService = multaService;
        this.notificacaoService = notificacaoService;
    }

    public AreaLeitorResponse area(UUID leitorId) {
        Leitor leitor = leitorService.buscar(leitorId);
        List<EmprestimoResponse> todos = emprestimoService.porLeitor(leitorId, null);
        return new AreaLeitorResponse(
                LeitorResponse.from(leitor),
                todos.stream().filter(e -> e.status() == Status.ATIVO).toList(),
                todos.stream().filter(e -> e.status() == Status.DEVOLVIDO).toList(),
                reservaService.reservasAtivasDoLeitor(leitorId),
                multaService.pendentesDoLeitor(leitorId).stream().map(MultaResponse::from).toList(),
                notificacaoService.listar(leitorId, Paginacao.de(1, 20, Sort.unsorted())).getContent().stream()
                        .map(NotificacaoResponse::from).toList(),
                notificacaoService.naoLidas(leitorId));
    }

    public List<EmprestimoResponse> emprestimos(UUID leitorId, Status status) {
        leitorService.buscar(leitorId);
        return emprestimoService.porLeitor(leitorId, status);
    }
}
