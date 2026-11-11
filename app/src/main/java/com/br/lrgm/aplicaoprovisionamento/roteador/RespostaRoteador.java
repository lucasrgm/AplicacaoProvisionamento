package com.br.lrgm.aplicaoprovisionamento.roteador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta as respostas da interface de gerenciamento (rotas /cgi) do TP-Link.
 *
 * <p>Formato da resposta:
 * <pre>
 * [1,1,0,0,0,0]0        → início de uma seção: pilha da instância e índice da consulta
 * SSID=MinhaRede        → campo=valor
 * X_TP_Band=2.4GHz
 * [error]0              → código de retorno (0 = sucesso)
 * </pre>
 */
public class RespostaRoteador {

    private static final Pattern CABECALHO_SECAO = Pattern.compile("^\\[([0-9,]+)\\](\\d+)$");
    private static final Pattern CODIGO_ERRO = Pattern.compile("^\\[error\\](-?\\d+)$");

    /** Uma instância de objeto devolvida pelo roteador. */
    public static class Secao {
        public final String pilha;
        public final int indice;
        public final Map<String, String> valores = new LinkedHashMap<>();

        Secao(String pilha, int indice) {
            this.pilha = pilha;
            this.indice = indice;
        }

        public String valor(String campo) {
            String v = valores.get(campo);
            return v == null ? "" : v;
        }
    }

    private final List<Secao> secoes;
    private final int codigoErro;

    private RespostaRoteador(List<Secao> secoes, int codigoErro) {
        this.secoes = secoes;
        this.codigoErro = codigoErro;
    }

    public static RespostaRoteador interpretar(String texto) {
        List<Secao> secoes = new ArrayList<>();
        Integer codigo = null;
        Secao atual = null;

        if (texto == null) texto = "";

        for (String linhaBruta : texto.split("\n")) {
            String linha = linhaBruta.replace("\r", "");
            if (linha.isEmpty()) continue;

            Matcher erro = CODIGO_ERRO.matcher(linha.trim());
            if (erro.matches()) {
                codigo = Integer.parseInt(erro.group(1));
                atual = null;
                continue;
            }

            Matcher cabecalho = CABECALHO_SECAO.matcher(linha.trim());
            if (cabecalho.matches()) {
                atual = new Secao(cabecalho.group(1), Integer.parseInt(cabecalho.group(2)));
                secoes.add(atual);
                continue;
            }

            int igual = linha.indexOf('=');
            if (atual != null && igual > 0) {
                // Só a primeira "=" separa campo e valor: senhas podem conter "=".
                atual.valores.put(linha.substring(0, igual), linha.substring(igual + 1));
            }
        }

        // Sem a linha [error], a resposta não é do formato esperado (ex.: página de login).
        return new RespostaRoteador(secoes, codigo == null ? -1 : codigo);
    }

    public boolean sucesso() {
        return codigoErro == 0;
    }

    public int getCodigoErro() {
        return codigoErro;
    }

    public List<Secao> getSecoes() {
        return Collections.unmodifiableList(secoes);
    }

    /** Seções devolvidas para a consulta de índice informado. */
    public List<Secao> secoes(int indice) {
        List<Secao> resultado = new ArrayList<>();
        for (Secao s : secoes) {
            if (s.indice == indice) resultado.add(s);
        }
        return resultado;
    }
}
