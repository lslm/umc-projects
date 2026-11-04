package dev.lslm.library.support;

import dev.lslm.library.exception.ApiException;

import java.util.UUID;

/**
 * Contexto da requisicao corrente: tenant (biblioteca), usuario, origem e id da requisicao.
 * Como todas as rotas sao publicas, o tenant e informado pelo cabecalho X-Biblioteca-Id.
 */
public final class RequestContext {

    public record Data(UUID bibliotecaId, UUID usuarioId, String origem, String requestId) {
    }

    private static final ThreadLocal<Data> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    static void set(Data data) {
        CURRENT.set(data);
    }

    static void clear() {
        CURRENT.remove();
    }

    public static UUID bibliotecaIdOrNull() {
        Data d = CURRENT.get();
        return d == null ? null : d.bibliotecaId();
    }

    public static UUID bibliotecaId() {
        UUID id = bibliotecaIdOrNull();
        if (id == null) {
            throw ApiException.badRequest("BIBLIOTECA_NAO_INFORMADA",
                    "Informe o cabeçalho X-Biblioteca-Id com o identificador da biblioteca.");
        }
        return id;
    }

    public static UUID usuarioId() {
        Data d = CURRENT.get();
        return d == null ? null : d.usuarioId();
    }

    public static String origem() {
        Data d = CURRENT.get();
        return d == null ? "SISTEMA" : d.origem();
    }

    public static String requestId() {
        Data d = CURRENT.get();
        return d == null ? null : d.requestId();
    }
}
