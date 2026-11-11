package com.br.lrgm.aplicaoprovisionamento.roteador;

import com.br.lrgm.aplicaoprovisionamento.Internet;
import com.br.lrgm.aplicaoprovisionamento.Wifi;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comunicação direta com a interface de gerenciamento do TP-Link TL-WR840N (v6).
 *
 * <p>Fluxo de uma operação:
 * <ol>
 *   <li>POST /cgi/getParm → valor "ss" usado no hash das credenciais;</li>
 *   <li>POST /cgi/login com usuário e senha em MD5 → cookie de sessão;</li>
 *   <li>GET / → página principal com o token (enviado no cabeçalho "Tokenid");</li>
 *   <li>POST /cgi?5 (leitura) ou /cgi?2 (escrita) com os objetos de configuração;</li>
 *   <li>POST /cgi?8 → encerra a sessão (o roteador aceita uma sessão por vez).</li>
 * </ol>
 */
public class TLWR840N implements Roteador {

    public static final String USUARIO_PADRAO = "admin";
    public static final String PILHA_WIFI_PADRAO = "1,1,0,0,0,0";

    // Objetos consultados na leitura das redes Wi-Fi (rota /cgi?5).
    static final String FORM_LER_WIFI = "[LAN_WLAN#0,0,0,0,0,0#0,0,0,0,0,0]0,19\r\n"
            + "name\r\nStandard\r\nSSID\r\nX_TP_Band\r\nEnable\r\nRegulatoryDomain\r\n"
            + "SSIDAdvertisementEnabled\r\nBeaconType\r\nBasicEncryptionModes\r\n"
            + "WPAEncryptionModes\r\nIEEE11iEncryptionModes\r\nbeaconType\r\n"
            + "BasicEncryptionModes\r\nBasicAuthenticationMode\r\nWPAEncryptionModes\r\n"
            + "WPAAuthenticationMode\r\nIEEE11iEncryptionModes\r\nIEEE11iAuthenticationMode\r\n"
            + "X_TP_PreSharedKey\r\n";

    // Objetos consultados na leitura do acesso à internet (rota /cgi?5&5&5&5&5&1).
    static final String FORM_LER_INTERNET = "[WAN_COMMON_INTF_CFG#0,0,0,0,0,0#0,0,0,0,0,0]0,1\r\n"
            + "WANAccessType\r\n"
            + "[WAN_IP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]1,7\r\n"
            + "name\r\nconnectionType\r\naddressingType\r\nexternalIPAddress\r\nsubnetMask\r\n"
            + "defaultGateway\r\nDNSServers\r\n"
            + "[WAN_PPP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]2,9\r\n"
            + "name\r\ntransportType\r\nusername\r\npassword\r\nX_TP_SecondConnection\r\n"
            + "X_TP_SecondConnectionIPAddress\r\nX_TP_SecondConnectionSubnetMask\r\n"
            + "X_TP_SecondConnectionDefaultGateway\r\nX_TP_SecondConnectionDNSServers\r\n"
            + "[WAN_L2TP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]3,11\r\n"
            + "enable\r\nname\r\naddressingType\r\ntransportType\r\nUsername\r\nPassword\r\n"
            + "ServerIpOrName\r\nExternalIPAddressIP\r\nSubnetMaskIP\r\nDefaultGatewayIP\r\n"
            + "DNSServersIP\r\n"
            + "[WAN_PPTP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]4,11\r\n"
            + "enable\r\nname\r\naddressingType\r\ntransportType\r\nUsername\r\nPassword\r\n"
            + "ServerIpOrName\r\nExternalIPAddressIP\r\nSubnetMaskIP\r\nDefaultGatewayIP\r\n"
            + "DNSServersIP\r\n"
            + "[L3_FORWARDING#0,0,0,0,0,0#0,0,0,0,0,0]5,1\r\n"
            + "__ifAliasName\r\n";

    static final String FORM_LOGOUT = "[/cgi/logout#0,0,0,0,0,0#0,0,0,0,0,0]0,0\r\n";

    private static final Pattern PADRAO_SS = Pattern.compile("ss\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern PADRAO_TOKEN = Pattern.compile("token\\s*=\\s*\"([^\"]+)\"");

    private final String ip;
    private final String porta;
    private final String usuario;
    private final String senha;
    private final String base;
    private final TransporteHttp http;

    private String tokenId;

    public TLWR840N(String ip, String porta, String usuario, String senha, TransporteHttp http) {
        this.ip = ip;
        this.porta = porta;
        this.usuario = (usuario == null || usuario.isEmpty()) ? USUARIO_PADRAO : usuario;
        this.senha = senha == null ? "" : senha;
        this.http = http;
        this.base = "http://" + ip + ":" + porta;
    }

    // ------------------------------------------------------------------
    // Operações (interface Roteador)
    // ------------------------------------------------------------------

    @Override
    public Parametros obterParametros() throws RoteadorException {
        login();
        try {
            Parametros parametros = new Parametros();
            parametros.ip = ip;
            parametros.porta = porta;
            parametros.listaWifi = new ArrayList<>(lerWifi());
            parametros.internet = lerInternet();
            return parametros;
        } finally {
            logout();
        }
    }

    @Override
    public List<Wifi> obterWifi() throws RoteadorException {
        login();
        try {
            return lerWifi();
        } finally {
            logout();
        }
    }

    @Override
    public Internet obterInternet() throws RoteadorException {
        login();
        try {
            return lerInternet();
        } finally {
            logout();
        }
    }

    @Override
    public ResultadoOperacao alterarWifi(String pilha, String ssid, String senhaWifi)
            throws RoteadorException {
        validarSsid(ssid);
        validarSenhaWifi(senhaWifi);
        if (pilha == null || !pilha.matches("[0-9,]+")) pilha = PILHA_WIFI_PADRAO;

        String form = "[LAN_WLAN#" + pilha + "#0,0,0,0,0,0]0,5\r\n"
                + "enable=1\r\n"
                + "SSID=" + ssid + "\r\n"
                + "SSIDAdvertisementEnabled=1\r\n"
                + "X_TP_PreSharedKey=" + senhaWifi + "\r\n"
                + "__syncApStatus=1\r\n";

        login();
        try {
            executarCgi("2", form);

            // Confirma lendo de novo do roteador.
            List<Wifi> redes;
            try {
                redes = lerWifi();
            } catch (RoteadorException e) {
                if (e.getCause() instanceof IOException) {
                    // Com o celular no Wi-Fi alterado, a conexão cai assim que o roteador aplica.
                    return new ResultadoOperacao(true, false,
                            "Configuração enviada. A conexão com o roteador caiu, o que é esperado "
                                    + "quando o celular está no Wi-Fi alterado. Conecte-se à rede \""
                                    + ssid + "\" para conferir.");
                }
                throw e;
            }

            for (Wifi w : redes) {
                if (w.pilha.equals(pilha)) {
                    if (w.ssid.equals(ssid) && w.password.equals(senhaWifi)) {
                        return new ResultadoOperacao(true, true, "Wi-Fi alterado com sucesso!");
                    }
                    return new ResultadoOperacao(false, false,
                            "O roteador não gravou a nova configuração do Wi-Fi. Tente novamente.");
                }
            }
            return new ResultadoOperacao(true, false,
                    "Configuração enviada, mas não foi possível conferir a rede alterada.");
        } finally {
            logout();
        }
    }

    @Override
    public ResultadoOperacao alterarPppoe(String usuarioPppoe, String senhaPppoe)
            throws RoteadorException {
        validarCampoPppoe(usuarioPppoe, "O usuário PPPoE");
        validarCampoPppoe(senhaPppoe, "A senha PPPoE");

        String form = "[WAN_PPP_CONN#1,1,1,0,0,0#0,0,0,0,0,0]0,5\r\n"
                + "enable=1\r\n"
                + "transportType=PPPoE\r\n"
                + "username=" + usuarioPppoe + "\r\n"
                + "password=" + senhaPppoe + "\r\n"
                + "X_TP_SecondConnection=None\r\n"
                + "[L3_FORWARDING#0,0,0,0,0,0#0,0,0,0,0,0]1,3\r\n"
                + "__ifAliasName=pppoe_eth_0_0_d\r\n"
                + "__ifName=\r\n"
                + "defaultConnectionService=\r\n";

        login();
        try {
            executarCgi("2&2", form);

            Internet internet = lerInternet();
            if (usuarioPppoe.equals(internet.pppoe) && senhaPppoe.equals(internet.password)) {
                return new ResultadoOperacao(true, true, "PPPoE alterado com sucesso!");
            }
            return new ResultadoOperacao(false, false,
                    "O roteador não gravou as novas credenciais PPPoE. Tente novamente.");
        } finally {
            logout();
        }
    }

    // ------------------------------------------------------------------
    // Sessão
    // ------------------------------------------------------------------

    /** Faz login no roteador e guarda o token da sessão. */
    void login() throws RoteadorException {
        tokenId = null;

        // A página de login do TP-Link calcula o hash das credenciais com o valor "ss".
        // A senha é codificada em Base64 antes do MD5; o usuário é testado nas duas formas
        // (com e sem Base64), porque isso varia entre versões de firmware.
        boolean[] usuarioEmBase64 = {true, false};

        for (boolean codificarUsuario : usuarioEmBase64) {
            String ss = obterSs();
            String usuarioHash = md5Encrypt(codificarUsuario ? base64(usuario) : usuario, ss);
            String senhaHash = md5Encrypt(base64(senha), ss);

            postSemCorpo("/cgi/login?UserName=" + usuarioHash + "&Passwd=" + senhaHash
                    + "&Action=1&LoginStatus=0");

            String pagina = get("/");
            Matcher m = PADRAO_TOKEN.matcher(pagina);
            if (m.find()) {
                tokenId = m.group(1);
                return;
            }
        }

        throw new RoteadorException("Não foi possível entrar no roteador. Confira a senha do roteador.");
    }

    /** Encerra a sessão. Falhas são ignoradas: a operação principal já terminou. */
    void logout() {
        if (tokenId == null) return;
        try {
            http.post(base + "/cgi?8", FORM_LOGOUT, cabecalhos(true));
        } catch (IOException ignorada) {
            // Sem conexão: a sessão expira sozinha no roteador.
        } finally {
            tokenId = null;
        }
    }

    private String obterSs() throws RoteadorException {
        String resposta = postSemCorpo("/cgi/getParm");
        Matcher m = PADRAO_SS.matcher(resposta);
        if (!m.find()) {
            throw new RoteadorException("O equipamento em " + ip + ":" + porta
                    + " não respondeu como um TP-Link TL-WR840N.");
        }
        return m.group(1);
    }

    // ------------------------------------------------------------------
    // Leitura (exige sessão aberta)
    // ------------------------------------------------------------------

    private List<Wifi> lerWifi() throws RoteadorException {
        RespostaRoteador resposta = executarCgi("5", FORM_LER_WIFI);
        List<Wifi> redes = new ArrayList<>();

        for (RespostaRoteador.Secao s : resposta.secoes(0)) {
            Wifi w = new Wifi();
            w.pilha = s.pilha;
            w.ssid = s.valor("SSID");
            w.band = s.valor("X_TP_Band").isEmpty() ? "2.4GHz" : s.valor("X_TP_Band");
            w.password = s.valor("X_TP_PreSharedKey");
            redes.add(w);
        }

        if (redes.isEmpty()) {
            throw new RoteadorException("O roteador não informou nenhuma rede Wi-Fi.");
        }
        return redes;
    }

    private Internet lerInternet() throws RoteadorException {
        RespostaRoteador resposta = executarCgi("5&5&5&5&5&1", FORM_LER_INTERNET);
        Internet internet = new Internet();

        for (RespostaRoteador.Secao s : resposta.secoes(2)) {
            if ("PPPoE".equalsIgnoreCase(s.valor("transportType"))) {
                internet.transportType = s.valor("transportType");
                internet.pppoe = s.valor("username");
                internet.password = s.valor("password");
                break;
            }
        }
        return internet;
    }

    // ------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------

    private RespostaRoteador executarCgi(String consulta, String form) throws RoteadorException {
        String texto;
        try {
            texto = http.post(base + "/cgi?" + consulta, form, cabecalhos(true));
        } catch (IOException e) {
            throw erroConexao(e);
        }

        RespostaRoteador resposta = RespostaRoteador.interpretar(texto);
        if (!resposta.sucesso()) {
            throw new RoteadorException("O roteador recusou a operação (código "
                    + resposta.getCodigoErro() + ").");
        }
        return resposta;
    }

    private String postSemCorpo(String caminho) throws RoteadorException {
        try {
            return http.post(base + caminho, "", cabecalhos(false));
        } catch (IOException e) {
            throw erroConexao(e);
        }
    }

    private String get(String caminho) throws RoteadorException {
        try {
            return http.get(base + caminho, cabecalhos(false));
        } catch (IOException e) {
            throw erroConexao(e);
        }
    }

    private Map<String, String> cabecalhos(boolean comToken) {
        Map<String, String> h = new LinkedHashMap<>();
        // A interface do TP-Link confere a origem das requisições.
        h.put("Origin", base);
        h.put("Referer", base + "/");
        if (comToken && tokenId != null) h.put("Tokenid", tokenId);
        return h;
    }

    private RoteadorException erroConexao(IOException e) {
        return new RoteadorException("Não foi possível conectar ao roteador em " + ip + ":" + porta
                + ". Verifique se o celular está na rede do roteador.", e);
    }

    // ------------------------------------------------------------------
    // Validações e utilitários
    // ------------------------------------------------------------------

    static void validarSsid(String ssid) throws RoteadorException {
        if (ssid == null || ssid.trim().isEmpty()) {
            throw new RoteadorException("O nome da rede (SSID) não pode ser vazio!");
        }
        if (ssid.getBytes(StandardCharsets.UTF_8).length > 32) {
            throw new RoteadorException("O nome da rede (SSID) pode ter no máximo 32 caracteres!");
        }
        if (temQuebraDeLinha(ssid)) {
            throw new RoteadorException("O nome da rede (SSID) não pode ter quebra de linha!");
        }
    }

    static void validarSenhaWifi(String senha) throws RoteadorException {
        if (senha == null || senha.length() < 8) {
            throw new RoteadorException("A senha do Wi-Fi precisa ter no mínimo 8 caracteres!");
        }
        if (senha.length() > 63) {
            throw new RoteadorException("A senha do Wi-Fi pode ter no máximo 63 caracteres!");
        }
        for (char c : senha.toCharArray()) {
            if (c < 32 || c > 126) {
                throw new RoteadorException(
                        "A senha do Wi-Fi aceita apenas letras sem acento, números e símbolos!");
            }
        }
    }

    static void validarCampoPppoe(String valor, String nome) throws RoteadorException {
        if (valor == null || valor.isEmpty()) {
            throw new RoteadorException(nome + " não pode ser vazio(a)!");
        }
        if (valor.length() > 64) {
            throw new RoteadorException(nome + " pode ter no máximo 64 caracteres!");
        }
        if (temQuebraDeLinha(valor)) {
            throw new RoteadorException(nome + " não pode ter quebra de linha!");
        }
    }

    private static boolean temQuebraDeLinha(String s) {
        return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
    }

    static String base64(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    /** Mesmo cálculo da página de login do roteador: MD5(ss + valor + ss) em hexadecimal. */
    static String md5Encrypt(String valor, String ss) {
        return md5(ss + valor + ss);
    }

    static String md5(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
