package dev.lslm.library.support;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash forte de senha (PBKDF2-HMAC-SHA256 com salt aleatorio). Nunca guarda senha em texto puro. */
public final class SenhaHasher {

    private static final int ITERACOES = 120_000;
    private static final int BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaHasher() {
    }

    public static String hash(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(derivar(senha, salt));
    }

    public static boolean confere(String senha, String armazenado) {
        String[] partes = armazenado.split(":");
        if (partes.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(partes[0]);
        byte[] esperado = Base64.getDecoder().decode(partes[1]);
        return MessageDigest.isEqual(esperado, derivar(senha, salt));
    }

    public static String tokenAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] derivar(String senha, byte[] salt) {
        try {
            var spec = new PBEKeySpec(senha.toCharArray(), salt, ITERACOES, BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
