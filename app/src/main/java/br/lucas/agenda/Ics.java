package br.lucas.agenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lê um ICS enxuto: VEVENT com horário, dia inteiro, fuso e RRULE básica. */
final class Ics {
    private static final DateTimeFormatter DATA = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATA_HORA_CURTA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");

    private Ics() { }

    static List<Evento> ler(String ics) {
        List<Evento> eventos = new ArrayList<>();
        if (ics == null || ics.isEmpty()) return eventos;
        String[] linhas = desdobrar(ics).split("\n");
        boolean dentro = false;
        int alarme = 0;
        Rascunho rascunho = null;
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            if (linha.isEmpty()) continue;
            if (igual(linha, "BEGIN:VEVENT")) {
                dentro = true;
                alarme = 0;
                rascunho = new Rascunho();
                continue;
            }
            if (igual(linha, "END:VEVENT")) {
                if (rascunho != null && !rascunho.cancelado) {
                    Evento evento = rascunho.montar();
                    if (evento != null) eventos.add(evento);
                }
                dentro = false;
                rascunho = null;
                continue;
            }
            if (!dentro || rascunho == null) continue;
            if (igual(linha, "BEGIN:VALARM")) {
                alarme++;
                continue;
            }
            if (igual(linha, "END:VALARM")) {
                if (alarme > 0) alarme--;
                continue;
            }
            if (alarme > 0) continue;
            aplicar(rascunho, linha);
        }
        return eventos;
    }

    private static void aplicar(Rascunho rascunho, String linha) {
        int sep = linha.indexOf(':');
        if (sep <= 0) return;
        String esquerda = linha.substring(0, sep);
        String valor = desescapar(linha.substring(sep + 1));
        String[] partes = esquerda.split(";");
        String nome = partes[0].toUpperCase(Locale.US);
        Map<String, String> params = parametros(partes);
        try {
            switch (nome) {
                case "UID":
                    rascunho.uid = valor.trim();
                    break;
                case "SUMMARY":
                    rascunho.titulo = valor.trim();
                    break;
                case "LOCATION":
                    rascunho.local = valor.trim();
                    break;
                case "DESCRIPTION":
                    rascunho.descricao = valor.trim();
                    break;
                case "STATUS":
                    rascunho.cancelado = "CANCELLED".equalsIgnoreCase(valor.trim());
                    break;
                case "DTSTART":
                    rascunho.inicio = instante(valor.trim(), params, true);
                    rascunho.diaInteiro = ehData(valor.trim(), params);
                    break;
                case "DTEND":
                    rascunho.fim = instante(valor.trim(), params, false);
                    break;
                case "RRULE":
                    rascunho.regra = regra(valor.trim());
                    break;
                case "RECURRENCE-ID":
                    rascunho.recorrenciaId = instante(valor.trim(), params, true);
                    break;
                case "EXDATE":
                    for (String parte : valor.split(",")) {
                        String item = parte.trim();
                        if (!item.isEmpty()) rascunho.exdatas.add(instante(item, params, true));
                    }
                    break;
                default:
                    break;
            }
        } catch (DateTimeParseException | IllegalArgumentException ignorado) {
            // Propriedade ilegível: o restante do evento ainda pode servir.
        }
    }

    static String desdobrar(String texto) {
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder saida = new StringBuilder(normalizado.length());
        for (int i = 0; i < normalizado.length(); i++) {
            char c = normalizado.charAt(i);
            if (c == '\n' && i + 1 < normalizado.length()) {
                char proximo = normalizado.charAt(i + 1);
                if (proximo == ' ' || proximo == '\t') {
                    i++;
                    continue;
                }
            }
            saida.append(c);
        }
        return saida.toString();
    }

    static String desescapar(String valor) {
        StringBuilder saida = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '\\' && i + 1 < valor.length()) {
                char proximo = valor.charAt(++i);
                if (proximo == 'n' || proximo == 'N') saida.append('\n');
                else saida.append(proximo);
            } else {
                saida.append(c);
            }
        }
        return saida.toString();
    }

    private static Map<String, String> parametros(String[] partes) {
        Map<String, String> mapa = new HashMap<>();
        for (int i = 1; i < partes.length; i++) {
            int eq = partes[i].indexOf('=');
            if (eq <= 0) continue;
            String chave = partes[i].substring(0, eq).trim().toUpperCase(Locale.US);
            String valor = partes[i].substring(eq + 1).trim();
            if (valor.length() >= 2 && valor.charAt(0) == '"' && valor.charAt(valor.length() - 1) == '"') {
                valor = valor.substring(1, valor.length() - 1);
            }
            mapa.put(chave, valor);
        }
        return mapa;
    }

    private static boolean ehData(String valor, Map<String, String> params) {
        return "DATE".equalsIgnoreCase(params.get("VALUE")) || (valor.length() == 8 && valor.indexOf('T') < 0);
    }

    static ZonedDateTime instante(String valor, Map<String, String> params, boolean inicio) {
        if (ehData(valor, params)) {
            LocalDate data = LocalDate.parse(valor.substring(0, 8), DATA);
            ZonedDateTime dia = data.atStartOfDay(Horario.SAO_PAULO);
            if (!inicio && valor.length() == 8) return dia;
            return dia;
        }
        boolean utc = valor.endsWith("Z");
        String corpo = utc ? valor.substring(0, valor.length() - 1) : valor;
        LocalDateTime local = dataHora(corpo);
        if (utc) return local.atZone(ZoneOffset.UTC).withZoneSameInstant(Horario.SAO_PAULO);
        return local.atZone(fuso(params.get("TZID"))).withZoneSameInstant(Horario.SAO_PAULO);
    }

    private static LocalDateTime dataHora(String corpo) {
        if (corpo.length() >= 15) return LocalDateTime.parse(corpo.substring(0, 15), DATA_HORA);
        if (corpo.length() >= 13) return LocalDateTime.parse(corpo.substring(0, 13), DATA_HORA_CURTA);
        throw new DateTimeParseException("data", corpo, 0);
    }

    private static ZoneId fuso(String tzid) {
        if (tzid == null || tzid.isEmpty()) return Horario.SAO_PAULO;
        try {
            return ZoneId.of(tzid);
        } catch (RuntimeException e) {
            return Horario.SAO_PAULO;
        }
    }

    static Evento.Regra regra(String valor) {
        Evento.Frequencia frequencia = null;
        int intervalo = 1;
        Integer contagem = null;
        ZonedDateTime ate = null;
        List<Evento.Dia> dias = new ArrayList<>();
        List<Integer> meses = new ArrayList<>();
        List<Integer> diasDoMes = new ArrayList<>();
        DayOfWeek inicioSemana = DayOfWeek.MONDAY;
        String[] partes = valor.split(";");
        for (int i = 0; i < partes.length; i++) {
            int eq = partes[i].indexOf('=');
            if (eq <= 0) continue;
            String chave = partes[i].substring(0, eq).trim().toUpperCase(Locale.US);
            String item = partes[i].substring(eq + 1).trim();
            switch (chave) {
                case "FREQ":
                    frequencia = frequencia(item);
                    break;
                case "INTERVAL":
                    intervalo = inteiro(item, 1);
                    break;
                case "COUNT":
                    int total = inteiro(item, 0);
                    contagem = total > 0 ? total : null;
                    break;
                case "UNTIL":
                    ate = ate(item);
                    break;
                case "BYDAY":
                    dias.addAll(dias(item));
                    break;
                case "BYMONTH":
                    meses.addAll(meses(item));
                    break;
                case "BYMONTHDAY":
                    diasDoMes.addAll(diasDoMes(item));
                    break;
                case "WKST":
                    DayOfWeek dia = diaSemana(item);
                    if (dia != null) inicioSemana = dia;
                    break;
                default:
                    break;
            }
        }
        if (frequencia == null) return null;
        if (intervalo < 1) intervalo = 1;
        return new Evento.Regra(frequencia, intervalo, contagem, ate, dias, meses, diasDoMes, inicioSemana);
    }

    private static Evento.Frequencia frequencia(String valor) {
        String item = valor.toUpperCase(Locale.US);
        if ("DAILY".equals(item)) return Evento.Frequencia.DIARIA;
        if ("WEEKLY".equals(item)) return Evento.Frequencia.SEMANAL;
        if ("MONTHLY".equals(item)) return Evento.Frequencia.MENSAL;
        if ("YEARLY".equals(item)) return Evento.Frequencia.ANUAL;
        return null;
    }

    private static ZonedDateTime ate(String valor) {
        Map<String, String> params = new HashMap<>();
        if (valor.length() == 8) {
            params.put("VALUE", "DATE");
            // O UNTIL de dia inteiro inclui o dia todo.
            return instante(valor, params, true).plusDays(1).minusSeconds(1);
        }
        return instante(valor, params, true);
    }

    private static List<Evento.Dia> dias(String valor) {
        List<Evento.Dia> lista = new ArrayList<>();
        String[] partes = valor.split(",");
        for (int i = 0; i < partes.length; i++) {
            Evento.Dia dia = dia(partes[i].trim());
            if (dia != null) lista.add(dia);
        }
        return lista;
    }

    private static Evento.Dia dia(String token) {
        if (token.isEmpty()) return null;
        String texto = token.toUpperCase(Locale.US);
        int i = 0;
        int sinal = 1;
        if (texto.charAt(0) == '+') i++;
        else if (texto.charAt(0) == '-') {
            sinal = -1;
            i++;
        }
        int inicio = i;
        while (i < texto.length() && Character.isDigit(texto.charAt(i))) i++;
        int ordinal = 0;
        if (i > inicio) ordinal = sinal * Integer.parseInt(texto.substring(inicio, i));
        DayOfWeek dia = diaSemana(texto.substring(i));
        if (dia == null) return null;
        return new Evento.Dia(dia, ordinal);
    }

    private static DayOfWeek diaSemana(String token) {
        String texto = token.toUpperCase(Locale.US);
        if (texto.endsWith("MO")) return DayOfWeek.MONDAY;
        if (texto.endsWith("TU")) return DayOfWeek.TUESDAY;
        if (texto.endsWith("WE")) return DayOfWeek.WEDNESDAY;
        if (texto.endsWith("TH")) return DayOfWeek.THURSDAY;
        if (texto.endsWith("FR")) return DayOfWeek.FRIDAY;
        if (texto.endsWith("SA")) return DayOfWeek.SATURDAY;
        if (texto.endsWith("SU")) return DayOfWeek.SUNDAY;
        return null;
    }

    private static List<Integer> diasDoMes(String valor) {
        List<Integer> lista = new ArrayList<>();
        String[] partes = valor.split(",");
        for (int i = 0; i < partes.length; i++) {
            int dia = inteiro(partes[i].trim(), 0);
            if (dia >= -31 && dia <= 31 && dia != 0) lista.add(dia);
        }
        return lista;
    }

    private static List<Integer> meses(String valor) {
        List<Integer> lista = new ArrayList<>();
        String[] partes = valor.split(",");
        for (int i = 0; i < partes.length; i++) {
            int mes = inteiro(partes[i].trim(), 0);
            if (mes >= 1 && mes <= 12) lista.add(mes);
        }
        return lista;
    }

    private static int inteiro(String valor, int padrao) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private static boolean igual(String linha, String alvo) {
        return linha.equalsIgnoreCase(alvo);
    }

    private static final class Rascunho {
        String uid = "";
        String titulo = "";
        String local = "";
        String descricao = "";
        ZonedDateTime inicio;
        ZonedDateTime fim;
        boolean diaInteiro;
        boolean cancelado;
        Evento.Regra regra;
        ZonedDateTime recorrenciaId;
        final List<ZonedDateTime> exdatas = new ArrayList<>();

        Evento montar() {
            if (inicio == null) return null;
            ZonedDateTime termino = fim;
            if (termino == null || !termino.isAfter(inicio)) {
                termino = diaInteiro ? inicio.plusDays(1) : inicio.plusHours(1);
            }
            String chave = uid.isEmpty() ? titulo + "|" + inicio.toInstant().toEpochMilli() : uid;
            String nome = titulo.isEmpty() ? "Compromisso" : titulo;
            // A exceção substitui uma ocorrência. A repetição fica no evento mestre.
            Evento.Regra repeticao = recorrenciaId == null ? regra : null;
            return new Evento(chave, nome, local, descricao, inicio, termino, diaInteiro,
                    repeticao, recorrenciaId, exdatas);
        }
    }
}
