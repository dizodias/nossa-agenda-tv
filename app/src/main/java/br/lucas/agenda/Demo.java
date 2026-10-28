package br.lucas.agenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A agenda de demonstração usa a semana de 5 de janeiro de 2026 como modelo.
 * Ao carregar, as datas caminham até a semana corrente.
 */
final class Demo {
    private static final LocalDate ANCORA = LocalDate.of(2026, 1, 5);
    private static final DateTimeFormatter DATA = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern MARCA = Pattern.compile("(?m)^X-DEMO-ANCORA:(\\d{8})\\s*$");
    private static final Pattern INSTANTE = Pattern.compile("(\\d{8})(T\\d{6}Z?)?");

    private Demo() { }

    static String paraSemanaAtual(String ics) {
        return deslocar(ics, LocalDate.now(Horario.SAO_PAULO));
    }

    static String deslocar(String ics, LocalDate hoje) {
        if (ics == null || ics.isEmpty() || hoje == null) return "";
        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long dias = ChronoUnit.DAYS.between(ancora(ics), segunda);
        if (dias == 0) return ics;
        String[] linhas = ics.split("\n", -1);
        StringBuilder saida = new StringBuilder(ics.length() + 64);
        for (int i = 0; i < linhas.length; i++) {
            if (i > 0) saida.append('\n');
            saida.append(deslocarLinha(linhas[i], dias));
        }
        return saida.toString();
    }

    private static LocalDate ancora(String ics) {
        Matcher matcher = MARCA.matcher(ics.replace("\r", ""));
        if (!matcher.find()) return ANCORA;
        try {
            return LocalDate.parse(matcher.group(1), DATA);
        } catch (DateTimeParseException e) {
            return ANCORA;
        }
    }

    private static String deslocarLinha(String linha, long dias) {
        if (!ehData(linha)) return linha;
        int sep = linha.indexOf(':');
        String valor = linha.substring(sep + 1);
        Matcher matcher = INSTANTE.matcher(valor);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String reposicao = matcher.group();
            try {
                LocalDate data = LocalDate.parse(matcher.group(1), DATA);
                String hora = matcher.group(2) == null ? "" : matcher.group(2);
                reposicao = data.plusDays(dias).format(DATA) + hora;
            } catch (DateTimeParseException ignorado) {
                reposicao = matcher.group();
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(reposicao));
        }
        matcher.appendTail(buffer);
        return linha.substring(0, sep + 1) + buffer;
    }

    private static boolean ehData(String linha) {
        int sep = linha.indexOf(':');
        if (sep <= 0) return false;
        String chave = linha.substring(0, sep);
        int ponto = chave.indexOf(';');
        if (ponto >= 0) chave = chave.substring(0, ponto);
        chave = chave.trim().toUpperCase(Locale.US);
        return "DTSTART".equals(chave) || "DTEND".equals(chave)
                || "EXDATE".equals(chave) || "RECURRENCE-ID".equals(chave);
    }
}
