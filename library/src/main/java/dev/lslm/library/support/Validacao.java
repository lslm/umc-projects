package dev.lslm.library.support;

public final class Validacao {

    private Validacao() {
    }

    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }

    public static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        return valor;
    }
}
