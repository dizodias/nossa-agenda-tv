package br.lucas.agenda;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Compromisso mestre, com a regra de repetição quando houver. */
final class Evento {
    final String uid;
    final String titulo;
    final String local;
    final String descricao;
    final ZonedDateTime inicio;
    final ZonedDateTime fim;
    final boolean diaInteiro;
    final Regra regra;
    /** Instante original que esta ocorrência substitui. Nulo no evento mestre. */
    final ZonedDateTime recorrenciaId;
    final List<ZonedDateTime> exdatas;

    Evento(String uid, String titulo, String local, String descricao,
           ZonedDateTime inicio, ZonedDateTime fim, boolean diaInteiro, Regra regra,
           ZonedDateTime recorrenciaId, List<ZonedDateTime> exdatas) {
        this.uid = uid;
        this.titulo = titulo;
        this.local = local;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.diaInteiro = diaInteiro;
        this.regra = regra;
        this.recorrenciaId = recorrenciaId;
        this.exdatas = Collections.unmodifiableList(new ArrayList<>(exdatas));
    }

    enum Frequencia { DIARIA, SEMANAL, MENSAL, ANUAL }

    static final class Regra {
        final Frequencia frequencia;
        final int intervalo;
        final Integer contagem;
        final ZonedDateTime ate;
        final List<Dia> dias;
        final List<Integer> meses;
        final List<Integer> diasDoMes;
        final DayOfWeek inicioSemana;

        Regra(Frequencia frequencia, int intervalo, Integer contagem, ZonedDateTime ate,
              List<Dia> dias, List<Integer> meses, List<Integer> diasDoMes, DayOfWeek inicioSemana) {
            this.frequencia = frequencia;
            this.intervalo = intervalo;
            this.contagem = contagem;
            this.ate = ate;
            this.dias = Collections.unmodifiableList(new ArrayList<>(dias));
            this.meses = Collections.unmodifiableList(new ArrayList<>(meses));
            this.diasDoMes = Collections.unmodifiableList(new ArrayList<>(diasDoMes));
            this.inicioSemana = inicioSemana;
        }
    }

    /** Dia da semana do BYDAY. Ordinal 0 vale para todos; 1 é o primeiro e -1 o último. */
    static final class Dia {
        final DayOfWeek dia;
        final int ordinal;

        Dia(DayOfWeek dia, int ordinal) {
            this.dia = dia;
            this.ordinal = ordinal;
        }
    }
}
