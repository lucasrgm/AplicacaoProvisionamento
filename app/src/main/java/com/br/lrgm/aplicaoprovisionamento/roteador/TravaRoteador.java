package com.br.lrgm.aplicaoprovisionamento.roteador;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Impede duas operações ao mesmo tempo no mesmo roteador (o TL-WR840N aceita uma
 * sessão de administração por vez). A chave é "ip:porta".
 */
public final class TravaRoteador {
    private static final Set<String> EM_USO = ConcurrentHashMap.newKeySet();

    private TravaRoteador() {
    }

    public static String chave(String ip, String porta) {
        return ip + ":" + porta;
    }

    /** Retorna false se o roteador já estiver em uso. */
    public static boolean travar(String chave) {
        return EM_USO.add(chave);
    }

    public static void liberar(String chave) {
        EM_USO.remove(chave);
    }

    public static boolean emUso(String chave) {
        return EM_USO.contains(chave);
    }
}
