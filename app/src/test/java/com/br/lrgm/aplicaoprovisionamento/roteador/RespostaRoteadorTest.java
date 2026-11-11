package com.br.lrgm.aplicaoprovisionamento.roteador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RespostaRoteadorTest {

    // Exemplo de resposta real anotado no wr840.js
    private static final String RESPOSTA_INTERNET = "[1,0,0,0,0,0]0\n"
            + "WANAccessType=Ethernet\n"
            + "[1,1,1,0,0,0]2\n"
            + "name=pppoe_eth_0_0_d\n"
            + "transportType=PPPoE\n"
            + "username=adfgfgd\n"
            + "password=1235fdgfg\n"
            + "X_TP_SecondConnection=None\n"
            + "X_TP_SecondConnectionIPAddress=0.0.0.0\n"
            + "X_TP_SecondConnectionDefaultGateway=\n"
            + "[0,0,0,0,0,0]5\n"
            + "__ifAliasName=pppoe_eth_0_0_d\n"
            + "[error]0";

    @Test
    public void interpretaSecoesEValores() {
        RespostaRoteador r = RespostaRoteador.interpretar(RESPOSTA_INTERNET);
        assertTrue(r.sucesso());
        assertEquals(3, r.getSecoes().size());

        RespostaRoteador.Secao ppp = r.secoes(2).get(0);
        assertEquals("1,1,1,0,0,0", ppp.pilha);
        assertEquals("adfgfgd", ppp.valor("username"));
        assertEquals("1235fdgfg", ppp.valor("password"));
        assertEquals("", ppp.valor("X_TP_SecondConnectionDefaultGateway"));
        assertEquals("", ppp.valor("campoInexistente"));
        assertEquals("Ethernet", r.secoes(0).get(0).valor("WANAccessType"));
    }

    @Test
    public void aceitaQuebraDeLinhaDoWindowsESenhaComIgual() {
        RespostaRoteador r = RespostaRoteador.interpretar(
                "[1,1,0,0,0,0]0\r\nSSID=Casa\r\nX_TP_PreSharedKey=a=b==c\r\n[error]0\r\n");
        assertTrue(r.sucesso());
        assertEquals("a=b==c", r.secoes(0).get(0).valor("X_TP_PreSharedKey"));
        assertEquals("Casa", r.secoes(0).get(0).valor("SSID"));
    }

    @Test
    public void codigoDeErroDiferenteDeZero() {
        RespostaRoteador r = RespostaRoteador.interpretar("[error]71233");
        assertFalse(r.sucesso());
        assertEquals(71233, r.getCodigoErro());
    }

    @Test
    public void respostaForaDoFormatoNaoEhSucesso() {
        assertFalse(RespostaRoteador.interpretar("<html>login</html>").sucesso());
        assertFalse(RespostaRoteador.interpretar("").sucesso());
        assertFalse(RespostaRoteador.interpretar(null).sucesso());
    }
}
