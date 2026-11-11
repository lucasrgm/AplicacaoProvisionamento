package com.br.lrgm.aplicaoprovisionamento.roteador;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.br.lrgm.aplicaoprovisionamento.Wifi;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;

import org.junit.Before;
import org.junit.Test;

public class TLWR840NTest {

    private RoteadorSimulado equipamento;
    private TLWR840N roteador;

    @Before
    public void preparar() {
        equipamento = new RoteadorSimulado();
        roteador = new TLWR840N("192.168.0.1", "80", "admin", "12345678", equipamento);
    }

    private static void esperarErro(String trechoMensagem, Executavel acao) {
        try {
            acao.executar();
            fail("Era esperada RoteadorException com: " + trechoMensagem);
        } catch (RoteadorException e) {
            assertTrue("Mensagem: " + e.getMessage(), e.getMessage().contains(trechoMensagem));
        }
    }

    private interface Executavel {
        void executar() throws RoteadorException;
    }

    // --- Hash do login -------------------------------------------------

    @Test
    public void hashDoLoginIgualAoDoWr840Js() {
        // Valores calculados com o wr840.js original: md5(ss + btoa(valor) + ss)
        assertEquals("e94c4e4735ae4a83d4639a91989b3cb9",
                TLWR840N.md5Encrypt(TLWR840N.base64("admin"), "5a7b9c"));
        assertEquals("9ab8a6202d030dcd27db9ba59b93aadc",
                TLWR840N.md5Encrypt(TLWR840N.base64("12345678"), "5a7b9c"));
    }

    // --- RF03 / RF04: identificar o roteador e obter parâmetros --------

    @Test
    public void CT04_obterParametrosLeWifiEPppoe() throws Exception {
        Parametros p = roteador.obterParametros();

        assertEquals("192.168.0.1", p.ip);
        assertEquals("80", p.porta);
        assertEquals(1, p.listaWifi.size());

        Wifi rede = p.listaWifi.get(0);
        assertEquals("TP-LINK_A1B2", rede.ssid);
        assertEquals("senhaAntiga", rede.password);
        assertEquals("2.4GHz", rede.band);
        assertEquals("1,1,0,0,0,0", rede.pilha);
        assertNull("O TL-WR840N não tem 5 GHz", p.obterWifi(true));

        assertEquals("PPPoE", p.internet.transportType);
        assertEquals("cliente01", p.internet.pppoe);
        assertEquals("x=y123", p.internet.password);

        assertEquals("Uma única sessão para as duas leituras", 1, equipamento.quantidade("/cgi/login"));
        assertEquals("A sessão é encerrada no fim", 1, equipamento.quantidade("/cgi?8"));
    }

    @Test
    public void requisicoesLevamTokenEReferer() throws Exception {
        roteador.obterWifi();
        for (RoteadorSimulado.Chamada c : equipamento.chamadas) {
            assertEquals("http://192.168.0.1:80/", c.cabecalhos.get("Referer"));
            if (c.url.contains("/cgi?")) {
                assertEquals(RoteadorSimulado.TOKEN, c.cabecalhos.get("Tokenid"));
            }
        }
    }

    @Test
    public void loginFuncionaQuandoFirmwareNaoCodificaUsuario() throws Exception {
        equipamento.usuarioEmBase64 = false;
        assertEquals("cliente01", roteador.obterInternet().pppoe);
        assertEquals(2, equipamento.quantidade("/cgi/login"));
    }

    @Test
    public void senhaDoRoteadorErrada() {
        roteador = new TLWR840N("192.168.0.1", "80", "admin", "errada", equipamento);
        esperarErro("Confira a senha do roteador", roteador::obterParametros);
        assertEquals(0, equipamento.quantidade("/cgi?5"));
    }

    @Test
    public void equipamentoQueNaoEhTpLink() {
        equipamento.responderComoTpLink = false;
        esperarErro("não respondeu como um TP-Link TL-WR840N", roteador::obterParametros);
    }

    @Test
    public void roteadorForaDeAlcance() {
        equipamento.semConexao = true;
        esperarErro("Não foi possível conectar ao roteador em 192.168.0.1:80", roteador::obterParametros);
    }

    // --- RF05: alterar Wi-Fi ---------------------------------------------

    @Test
    public void CT05_alterarWifiConfirmaLendoDeNovo() throws Exception {
        ResultadoOperacao r = roteador.alterarWifi("1,1,0,0,0,0", "Cliente Silva", "novaSenha123");

        assertTrue(r.sucesso);
        assertTrue(r.confirmado);
        assertEquals("Cliente Silva", equipamento.ssid);
        assertEquals("novaSenha123", equipamento.senhaWifi);

        RoteadorSimulado.Chamada escrita = null;
        for (RoteadorSimulado.Chamada c : equipamento.chamadas) {
            if (c.url.endsWith("/cgi?2")) escrita = c;
        }
        assertEquals("[LAN_WLAN#1,1,0,0,0,0#0,0,0,0,0,0]0,5\r\nenable=1\r\nSSID=Cliente Silva\r\n"
                        + "SSIDAdvertisementEnabled=1\r\nX_TP_PreSharedKey=novaSenha123\r\n__syncApStatus=1\r\n",
                escrita.corpo);
        assertEquals(1, equipamento.quantidade("/cgi?8"));
    }

    @Test
    public void alterarWifiComConexaoDerrubadaPeloRoteador() throws Exception {
        equipamento.derrubarConexaoAposWifi = true;
        ResultadoOperacao r = roteador.alterarWifi("1,1,0,0,0,0", "Nova Rede", "novaSenha123");
        assertTrue(r.sucesso);
        assertFalse(r.confirmado);
        assertTrue(r.mensagem.contains("Conecte-se à rede \"Nova Rede\""));
    }

    @Test
    public void alterarWifiQueORoteadorNaoGravou() throws Exception {
        equipamento.ignorarEscrita = true;
        ResultadoOperacao r = roteador.alterarWifi("1,1,0,0,0,0", "Nova Rede", "novaSenha123");
        assertFalse(r.sucesso);
    }

    @Test
    public void alterarWifiRecusadoPeloRoteador() {
        equipamento.codigoErroEscrita = 9002;
        esperarErro("código 9002", () -> roteador.alterarWifi("1,1,0,0,0,0", "Rede", "senha12345"));
        assertEquals("Sessão encerrada mesmo com erro", 1, equipamento.quantidade("/cgi?8"));
    }

    // --- RF07 / CT09: validação antes do envio ---------------------------

    @Test
    public void CT09_validacoesDoWifiNaoEnviamNada() {
        esperarErro("mínimo 8", () -> roteador.alterarWifi(null, "Rede", "1234567"));
        esperarErro("não pode ser vazio", () -> roteador.alterarWifi(null, "  ", "12345678"));
        esperarErro("máximo 32", () -> roteador.alterarWifi(null, "123456789012345678901234567890123", "12345678"));
        esperarErro("quebra de linha", () -> roteador.alterarWifi(null, "Rede\r\nX_TP=1", "12345678"));
        esperarErro("máximo 63", () -> roteador.alterarWifi(null, "Rede", "a".repeat(64)));
        esperarErro("sem acento", () -> roteador.alterarWifi(null, "Rede", "senhação123"));
        assertTrue("Nenhuma requisição ao roteador", equipamento.chamadas.isEmpty());
    }

    // --- RF06: alterar PPPoE ---------------------------------------------

    @Test
    public void CT07_alterarPppoeConfirmaLendoDeNovo() throws Exception {
        ResultadoOperacao r = roteador.alterarPppoe("joao.silva@provedor", "abc");
        assertTrue(r.sucesso);
        assertTrue(r.confirmado);
        assertEquals("joao.silva@provedor", equipamento.usuarioPppoe);
        assertEquals("abc", equipamento.senhaPppoe);
    }

    @Test
    public void alterarPppoeQueORoteadorNaoGravou() throws Exception {
        equipamento.ignorarEscrita = true;
        assertFalse(roteador.alterarPppoe("novo", "nova").sucesso);
    }

    @Test
    public void validacoesDoPppoe() {
        esperarErro("usuário PPPoE", () -> roteador.alterarPppoe("", "senha"));
        esperarErro("senha PPPoE", () -> roteador.alterarPppoe("usuario", ""));
        esperarErro("quebra de linha", () -> roteador.alterarPppoe("usuario\n[X]", "senha"));
        assertTrue(equipamento.chamadas.isEmpty());
    }
}
