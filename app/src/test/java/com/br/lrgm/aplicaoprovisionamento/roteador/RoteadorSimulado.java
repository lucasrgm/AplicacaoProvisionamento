package com.br.lrgm.aplicaoprovisionamento.roteador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simula a interface web do TL-WR840N para os testes: login com MD5 + "ss", token na
 * página principal, leitura (/cgi?5) e escrita (/cgi?2) no formato do TP-Link.
 */
class RoteadorSimulado implements TransporteHttp {

    static final String SS = "5a7b9c";
    static final String TOKEN = "f98f31db8fcbc2d68c0d17eaf73ee8";

    static class Chamada {
        final String metodo, url, corpo;
        final Map<String, String> cabecalhos;

        Chamada(String metodo, String url, String corpo, Map<String, String> cabecalhos) {
            this.metodo = metodo;
            this.url = url;
            this.corpo = corpo;
            this.cabecalhos = cabecalhos;
        }
    }

    // Configuração do equipamento
    String senhaAdmin = "12345678";
    boolean usuarioEmBase64 = true;
    String ssid = "TP-LINK_A1B2";
    String senhaWifi = "senhaAntiga";
    String usuarioPppoe = "cliente01";
    String senhaPppoe = "x=y123";

    // Comportamentos para os cenários de teste
    boolean responderComoTpLink = true;
    boolean semConexao = false;
    boolean ignorarEscrita = false;
    boolean derrubarConexaoAposWifi = false;
    int codigoErroEscrita = 0;

    final List<Chamada> chamadas = new ArrayList<>();
    private boolean logado = false;
    private boolean conexaoCaiu = false;

    @Override
    public String get(String url, Map<String, String> cabecalhos) throws IOException {
        chamadas.add(new Chamada("GET", url, null, cabecalhos));
        verificarConexao();
        if (url.endsWith("/")) {
            return logado
                    ? "<html><script type=\"text/javascript\">var token=\"" + TOKEN + "\";</script></html>"
                    : "<html><title>Login</title></html>";
        }
        throw new IOException("404");
    }

    @Override
    public String post(String url, String corpo, Map<String, String> cabecalhos) throws IOException {
        chamadas.add(new Chamada("POST", url, corpo, cabecalhos));
        verificarConexao();

        String caminho = url.substring(url.indexOf('/', "http://".length()));

        if (caminho.equals("/cgi/getParm")) {
            return responderComoTpLink
                    ? "var ee=\"10001\";\nvar nn=\"C2B5...\";\nvar ss=\"" + SS + "\";\n$.ret=0;\n"
                    : "<html>outro equipamento</html>";
        }

        if (caminho.startsWith("/cgi/login?")) {
            String usuario = parametro(caminho, "UserName");
            String senha = parametro(caminho, "Passwd");
            String usuarioEsperado = TLWR840N.md5(SS
                    + (usuarioEmBase64 ? TLWR840N.base64("admin") : "admin") + SS);
            String senhaEsperada = TLWR840N.md5(SS + TLWR840N.base64(senhaAdmin) + SS);
            logado = usuario.equals(usuarioEsperado) && senha.equals(senhaEsperada);
            return "$.ret=" + (logado ? 0 : 71233) + ";";
        }

        if (!logado || !TOKEN.equals(cabecalhos.get("Tokenid"))) {
            return "[error]71233";
        }

        switch (caminho) {
            case "/cgi?5":
                return "[1,1,0,0,0,0]0\r\nname=wlan0\r\nStandard=bgn\r\nSSID=" + ssid
                        + "\r\nX_TP_Band=2.4GHz\r\nEnable=1\r\nX_TP_PreSharedKey=" + senhaWifi
                        + "\r\n[error]0\r\n";
            case "/cgi?5&5&5&5&5&1":
                return "[1,0,0,0,0,0]0\nWANAccessType=Ethernet\n"
                        + "[1,1,1,0,0,0]2\nname=pppoe_eth_0_0_d\ntransportType=PPPoE\nusername="
                        + usuarioPppoe + "\npassword=" + senhaPppoe + "\nX_TP_SecondConnection=None\n"
                        + "[0,0,0,0,0,0]5\n__ifAliasName=pppoe_eth_0_0_d\n[error]0";
            case "/cgi?2":
                if (codigoErroEscrita != 0) return "[error]" + codigoErroEscrita;
                if (!ignorarEscrita && corpo.startsWith("[LAN_WLAN#1,1,0,0,0,0#")) {
                    ssid = campo(corpo, "SSID");
                    senhaWifi = campo(corpo, "X_TP_PreSharedKey");
                }
                if (derrubarConexaoAposWifi) conexaoCaiu = true;
                return "[error]0";
            case "/cgi?2&2":
                if (codigoErroEscrita != 0) return "[error]" + codigoErroEscrita;
                if (!ignorarEscrita) {
                    usuarioPppoe = campo(corpo, "username");
                    senhaPppoe = campo(corpo, "password");
                }
                return "[error]0";
            case "/cgi?8":
                logado = false;
                return "[error]0";
            default:
                return "[error]1";
        }
    }

    int quantidade(String trechoUrl) {
        int n = 0;
        for (Chamada c : chamadas) if (c.url.contains(trechoUrl)) n++;
        return n;
    }

    private void verificarConexao() throws IOException {
        if (semConexao || conexaoCaiu) throw new IOException("failed to connect to /192.168.0.1");
    }

    private static String parametro(String caminho, String nome) {
        Matcher m = Pattern.compile("[?&]" + nome + "=([^&]*)").matcher(caminho);
        return m.find() ? m.group(1) : "";
    }

    private static String campo(String corpo, String nome) {
        for (String linha : corpo.split("\r\n")) {
            if (linha.startsWith(nome + "=")) return linha.substring(nome.length() + 1);
        }
        return null;
    }
}
