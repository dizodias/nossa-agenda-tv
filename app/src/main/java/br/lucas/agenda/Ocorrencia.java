package br.lucas.agenda;

import java.time.ZonedDateTime;

/** Uma ocorrência já expandida, pronta para a lista e para a grade. */
final class Ocorrencia {
    final String uid;
    final String titulo;
    final String local;
    final String descricao;
    final ZonedDateTime inicio;
    final ZonedDateTime fim;
    final boolean diaInteiro;

    Ocorrencia(String uid, String titulo, String local, String descricao,
               ZonedDateTime inicio, ZonedDateTime fim, boolean diaInteiro) {
        this.uid = uid;
        this.titulo = titulo;
        this.local = local;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.diaInteiro = diaInteiro;
    }

    long inicioMillis() {
        return inicio.toInstant().toEpochMilli();
    }

    boolean mesma(String outroUid, long outroInicio) {
        return uid.equals(outroUid) && inicioMillis() == outroInicio;
    }
}
