package com.br.lrgm.aplicaoprovisionamento.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ValidacaoTest {

    @Test
    public void CT04_enderecosValidos() {
        assertNull(Validacao.validarEndereco("192.168.0.1", "80"));
        assertNull(Validacao.validarEndereco("10.0.0.254", "8080"));
    }

    @Test
    public void enderecosInvalidos() {
        assertEquals("Endereço IP inválido!", Validacao.validarEndereco("192.168.0", "80"));
        assertEquals("Endereço IP inválido!", Validacao.validarEndereco("192.168.0.256", "80"));
        assertEquals("Endereço IP inválido!", Validacao.validarEndereco("192x168x0x1", "80"));
        assertEquals("Endereço IP inválido!", Validacao.validarEndereco("", "80"));
        assertEquals("Porta inválida!", Validacao.validarEndereco("192.168.0.1", ""));
        assertEquals("Porta inválida!", Validacao.validarEndereco("192.168.0.1", "0"));
        assertEquals("Porta inválida!", Validacao.validarEndereco("192.168.0.1", "70000"));
        assertEquals("Porta inválida!", Validacao.validarEndereco("192.168.0.1", "8a"));
    }
}
