package dev.lslm.library.support;

import java.util.UUID;

/**
 * Helper de teste: fica no mesmo pacote de RequestContext para acessar seus metodos
 * package-private sem alterar a visibilidade no codigo de producao.
 */
public final class TenantTestSupport {

    private TenantTestSupport() {
    }

    public static void definirTenant(UUID bibliotecaId) {
        RequestContext.set(new RequestContext.Data(bibliotecaId, null, "TESTE", "req-teste"));
    }

    public static void limpar() {
        RequestContext.clear();
    }
}
