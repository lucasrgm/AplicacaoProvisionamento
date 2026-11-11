package com.br.lrgm.aplicaoprovisionamento.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê a data de expiração do token JWT emitido pelo backend. Assim o app sabe, mesmo sem
 * internet (celular na rede do roteador), se a sessão do técnico ainda é válida.
 */
public final class TokenJwt {

    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    /** Margem para não iniciar uma operação com o token prestes a vencer. */
    static final long MARGEM_SEGUNDOS = 30;

    private TokenJwt() {
    }

    /** Segundos desde 1970 em que o token expira, ou -1 se o token for inválido. */
    public static long expiracao(String token) {
        if (token == null) return -1;
        String[] partes = token.split("\\.");
        if (partes.length != 3) return -1;
        try {
            byte[] json = Base64.getUrlDecoder().decode(partes[1]);
            Matcher m = EXP.matcher(new String(json, StandardCharsets.UTF_8));
            return m.find() ? Long.parseLong(m.group(1)) : -1;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    public static boolean valido(String token, long agoraEmSegundos) {
        long exp = expiracao(token);
        return exp > 0 && exp - MARGEM_SEGUNDOS > agoraEmSegundos;
    }

    public static boolean valido(String token) {
        return valido(token, System.currentTimeMillis() / 1000);
    }
}
