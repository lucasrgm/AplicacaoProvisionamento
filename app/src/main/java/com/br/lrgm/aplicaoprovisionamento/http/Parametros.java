package com.br.lrgm.aplicaoprovisionamento.http;

import com.br.lrgm.aplicaoprovisionamento.Internet;
import com.br.lrgm.aplicaoprovisionamento.Wifi;

import java.io.Serializable;
import java.util.ArrayList;

/** Parâmetros lidos do roteador e exibidos na tela principal. */
public class Parametros implements Serializable {
    private static final long serialVersionUID = 1L;

    public ArrayList<Wifi> listaWifi = new ArrayList<>();
    public Internet internet = new Internet();
    public String ip = "", porta = "";

    public Internet getInternet() {
        return internet;
    }

    /** Retorna a rede da faixa pedida, ou null se o roteador não tiver essa faixa. */
    public Wifi obterWifi(boolean cincoGHz) {
        for (Wifi wifi : listaWifi) {
            if (wifi.eh5GHz() == cincoGHz) return wifi;
        }
        return null;
    }

    public int indiceDe(Wifi wifi) {
        return listaWifi.indexOf(wifi);
    }
}
