package com.br.lrgm.aplicaoprovisionamento.roteador;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TravaRoteadorTest {

    @Test
    public void CT08_segundaOperacaoNoMesmoRoteadorEhRecusada() {
        String chave = TravaRoteador.chave("192.168.0.1", "80");
        assertTrue(TravaRoteador.travar(chave));
        try {
            assertFalse("Roteador ocupado", TravaRoteador.travar(chave));
            assertTrue("Outro roteador não é afetado",
                    TravaRoteador.travar(TravaRoteador.chave("192.168.1.1", "80")));
        } finally {
            TravaRoteador.liberar(chave);
            TravaRoteador.liberar(TravaRoteador.chave("192.168.1.1", "80"));
        }
        assertTrue("Liberado após a operação", TravaRoteador.travar(chave));
        TravaRoteador.liberar(chave);
    }
}
