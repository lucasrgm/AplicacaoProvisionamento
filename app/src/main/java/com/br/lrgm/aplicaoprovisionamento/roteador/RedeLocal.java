package com.br.lrgm.aplicaoprovisionamento.roteador;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import javax.net.SocketFactory;

/**
 * Durante a instalação, a rede do roteador ainda não tem internet. Nesse caso o Android
 * pode usar os dados móveis como rede padrão e as requisições a 192.168.0.1 não chegam
 * ao roteador. Esta classe devolve os sockets da rede local (cabo Ethernet ou Wi-Fi),
 * para que o app fale com o roteador pela rede certa.
 */
public final class RedeLocal {

    private RedeLocal() {
    }

    public static SocketFactory socketFactory(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm == null) return null;

        Network rede = procurar(cm, NetworkCapabilities.TRANSPORT_ETHERNET);
        if (rede == null) rede = procurar(cm, NetworkCapabilities.TRANSPORT_WIFI);
        return rede == null ? null : rede.getSocketFactory();
    }

    @SuppressWarnings("deprecation")
    private static Network procurar(ConnectivityManager cm, int transporte) {
        for (Network rede : cm.getAllNetworks()) {
            NetworkCapabilities cap = cm.getNetworkCapabilities(rede);
            if (cap != null && cap.hasTransport(transporte)) return rede;
        }
        return null;
    }
}
