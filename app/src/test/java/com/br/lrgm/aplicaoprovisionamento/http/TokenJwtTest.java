package com.br.lrgm.aplicaoprovisionamento.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

public class TokenJwtTest {

    private static String token(String payloadJson) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8))
                + ".assinatura";
    }

    @Test
    public void leExpiracao() {
        String t = token("{\"sub\":\"1\",\"username\":\"tecnico\",\"iat\":1000,\"exp\":4600}");
        assertEquals(4600, TokenJwt.expiracao(t));
    }

    @Test
    public void CT03_tokenExpiradoOuPertoDeExpirarNaoVale() {
        String t = token("{\"username\":\"tecnico\",\"exp\":4600}");
        assertTrue(TokenJwt.valido(t, 1000));
        assertFalse("Vencido", TokenJwt.valido(t, 4601));
        assertFalse("Dentro da margem de 30 s", TokenJwt.valido(t, 4580));
    }

    @Test
    public void tokenInvalidoOuAusente() {
        assertFalse(TokenJwt.valido("", 0));
        assertFalse(TokenJwt.valido(null, 0));
        assertFalse(TokenJwt.valido("abc.def", 0));
        assertFalse(TokenJwt.valido("a.%%%.c", 0));
        assertFalse(TokenJwt.valido(token("{\"username\":\"sem exp\"}"), 0));
    }
}
