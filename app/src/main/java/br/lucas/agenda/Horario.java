package br.lucas.agenda;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Horário de exibição em America/Sao_Paulo, com dia da semana em português. */
final class Horario {
    static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Horario() { }

    static String hora(ZonedDateTime quando) {
        return quando.withZoneSameInstant(SAO_PAULO).format(HORA);
    }

    static String hora(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(SAO_PAULO).format(HORA);
    }

    /** "Hoje", "Amanhã" ou "qui 30/10". */
    static String grupo(LocalDate dia, LocalDate hoje) {
        if (dia.equals(hoje)) return "Hoje";
        if (dia.equals(hoje.plusDays(1))) return "Amanhã";
        return dataCurta(dia);
    }

    static String dataCurta(LocalDate dia) {
        return siglaMinuscula(dia.getDayOfWeek()) + " " + dois(dia.getDayOfMonth()) + "/" + dois(dia.getMonthValue());
    }

    static String sigla(LocalDate dia) {
        switch (dia.getDayOfWeek()) {
            case MONDAY: return "SEG";
            case TUESDAY: return "TER";
            case WEDNESDAY: return "QUA";
            case THURSDAY: return "QUI";
            case FRIDAY: return "SEX";
            case SATURDAY: return "SÁB";
            case SUNDAY: return "DOM";
            default: return "";
        }
    }

    /** "26 out – 1 nov", da segunda ao domingo. */
    static String faixaSemana(LocalDate segunda) {
        return mesCurto(segunda) + " – " + mesCurto(segunda.plusDays(6));
    }

    private static String mesCurto(LocalDate dia) {
        return dia.getDayOfMonth() + " " + mes(dia.getMonthValue());
    }

    private static String siglaMinuscula(DayOfWeek dia) {
        switch (dia) {
            case MONDAY: return "seg";
            case TUESDAY: return "ter";
            case WEDNESDAY: return "qua";
            case THURSDAY: return "qui";
            case FRIDAY: return "sex";
            case SATURDAY: return "sáb";
            case SUNDAY: return "dom";
            default: return "";
        }
    }

    private static String mes(int mes) {
        switch (mes) {
            case 1: return "jan";
            case 2: return "fev";
            case 3: return "mar";
            case 4: return "abr";
            case 5: return "mai";
            case 6: return "jun";
            case 7: return "jul";
            case 8: return "ago";
            case 9: return "set";
            case 10: return "out";
            case 11: return "nov";
            default: return "dez";
        }
    }

    private static String dois(int valor) {
        return valor < 10 ? "0" + valor : Integer.toString(valor);
    }
}
