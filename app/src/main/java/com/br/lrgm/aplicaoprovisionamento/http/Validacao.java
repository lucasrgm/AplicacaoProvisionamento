package com.br.lrgm.aplicaoprovisionamento.http;

/** Validações de entrada que não dependem do Android (testadas em src/test). */
public final class Validacao {

    private static final String OCTETO = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final String IPV4 = "^(" + OCTETO + "\\.){3}" + OCTETO + "$";

    private Validacao() {
    }

    /** Retorna a mensagem de erro, ou null se o endereço for válido. */
    public static String validarEndereco(String ip, String porta) {
        if (ip == null || !ip.matches(IPV4)) {
            return "Endereço IP inválido!";
        }
        if (porta == null || !porta.matches("^\\d{1,5}$")) {
            return "Porta inválida!";
        }
        int numero = Integer.parseInt(porta);
        if (numero < 1 || numero > 65535) {
            return "Porta inválida!";
        }
        return null;
    }
}
