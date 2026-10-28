package br.lucas.agenda;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Expande RRULE básica (dia, semana, mês e ano, com COUNT, UNTIL e BYDAY). */
final class Recorrencia {
    private static final int PASSOS = 20000;

    private Recorrencia() { }

    static List<Ocorrencia> naJanela(List<Evento> eventos, ZonedDateTime de, ZonedDateTime ate) {
        Map<String, List<ZonedDateTime>> substituicoes = new HashMap<>();
        for (int i = 0; i < eventos.size(); i++) {
            Evento evento = eventos.get(i);
            if (evento.recorrenciaId == null) continue;
            List<ZonedDateTime> lista = substituicoes.get(evento.uid);
            if (lista == null) {
                lista = new ArrayList<>();
                substituicoes.put(evento.uid, lista);
            }
            lista.add(evento.recorrenciaId);
        }
        List<Ocorrencia> saida = new ArrayList<>();
        for (int i = 0; i < eventos.size(); i++) {
            Evento evento = eventos.get(i);
            List<ZonedDateTime> pulos = new ArrayList<>(evento.exdatas);
            if (evento.recorrenciaId == null) {
                List<ZonedDateTime> extras = substituicoes.get(evento.uid);
                if (extras != null) pulos.addAll(extras);
            }
            saida.addAll(expandir(evento, de, ate, pulos));
        }
        saida.sort(Comparator.comparing((Ocorrencia o) -> o.inicio).thenComparing(o -> o.titulo, String.CASE_INSENSITIVE_ORDER));
        return saida;
    }

    static List<Ocorrencia> expandir(Evento evento, ZonedDateTime de, ZonedDateTime ate) {
        return expandir(evento, de, ate, evento.exdatas);
    }

    private static List<Ocorrencia> expandir(Evento evento, ZonedDateTime de, ZonedDateTime ate,
                                             List<ZonedDateTime> pulos) {
        List<ZonedDateTime> inicios = inicios(evento, de, ate, pulos);
        List<Ocorrencia> saida = new ArrayList<>();
        for (int i = 0; i < inicios.size(); i++) {
            ZonedDateTime inicio = inicios.get(i);
            ZonedDateTime fim = fimDe(evento, inicio);
            saida.add(new Ocorrencia(evento.uid, evento.titulo, evento.local, evento.descricao,
                    inicio, fim, evento.diaInteiro));
        }
        return saida;
    }

    private static List<ZonedDateTime> inicios(Evento evento, ZonedDateTime de, ZonedDateTime ate,
                                               List<ZonedDateTime> pulos) {
        if (evento.regra == null) {
            List<ZonedDateTime> unico = new ArrayList<>();
            if (entra(evento, evento.inicio, de, ate) && !pulado(evento, evento.inicio, pulos)) {
                unico.add(evento.inicio);
            }
            return unico;
        }
        int[] contados = new int[] {0};
        int maximo = evento.regra.contagem == null ? Integer.MAX_VALUE : evento.regra.contagem;
        List<ZonedDateTime> saida = new ArrayList<>();
        int salto = indiceInicial(evento, de);
        switch (evento.regra.frequencia) {
            case DIARIA:
                diaria(evento, de, ate, salto, contados, maximo, saida, pulos);
                break;
            case SEMANAL:
                semanal(evento, de, ate, salto, contados, maximo, saida, pulos);
                break;
            case MENSAL:
                mensal(evento, de, ate, salto, contados, maximo, saida, pulos);
                break;
            case ANUAL:
                anual(evento, de, ate, salto, contados, maximo, saida, pulos);
                break;
            default:
                break;
        }
        return saida;
    }

    private static void diaria(Evento evento, ZonedDateTime de, ZonedDateTime ate, int salto,
                               int[] contados, int maximo, List<ZonedDateTime> saida, List<ZonedDateTime> pulos) {
        for (int n = salto; n < salto + PASSOS && contados[0] < maximo; n++) {
            ZonedDateTime cand = evento.inicio.plusDays((long) n * evento.regra.intervalo);
            if (!casaDia(evento.regra, cand)) {
                if (!cand.isBefore(ate)) break;
                continue;
            }
            if (!registrar(saida, cand, evento, de, ate, contados, maximo, pulos)) break;
        }
    }

    private static void semanal(Evento evento, ZonedDateTime de, ZonedDateTime ate, int salto,
                                int[] contados, int maximo, List<ZonedDateTime> saida, List<ZonedDateTime> pulos) {
        DayOfWeek wkst = evento.regra.inicioSemana;
        List<Evento.Dia> dias = diasDaSemana(evento);
        LocalDate ancora = inicioDaSemana(evento.inicio.toLocalDate(), wkst);
        for (int w = salto; w < salto + PASSOS && contados[0] < maximo; w++) {
            LocalDate semana = ancora.plusWeeks((long) w * evento.regra.intervalo);
            if (semana.isAfter(ate.toLocalDate().plusDays(7))) break;
            boolean seguir = true;
            for (int i = 0; i < dias.size(); i++) {
                Evento.Dia dia = dias.get(i);
                if (dia.ordinal != 0) continue;
                LocalDate data = semana.plusDays(ordem(dia.dia, wkst));
                ZonedDateTime cand = comHora(evento, data);
                if (!registrar(saida, cand, evento, de, ate, contados, maximo, pulos)) {
                    seguir = false;
                    break;
                }
            }
            if (!seguir) break;
        }
    }

    private static void mensal(Evento evento, ZonedDateTime de, ZonedDateTime ate, int salto,
                               int[] contados, int maximo, List<ZonedDateTime> saida, List<ZonedDateTime> pulos) {
        for (int n = salto; n < salto + PASSOS && contados[0] < maximo; n++) {
            YearMonth mes = YearMonth.from(evento.inicio).plusMonths((long) n * evento.regra.intervalo);
            if (mes.atDay(1).isAfter(ate.toLocalDate().plusMonths(1))) break;
            if (!mesPermitido(evento.regra, mes.getMonthValue())) continue;
            if (!aplicarDatas(datasDoMes(evento, mes), evento, de, ate, contados, maximo, saida, pulos)) break;
        }
    }

    private static void anual(Evento evento, ZonedDateTime de, ZonedDateTime ate, int salto,
                              int[] contados, int maximo, List<ZonedDateTime> saida, List<ZonedDateTime> pulos) {
        for (int n = salto; n < salto + PASSOS && contados[0] < maximo; n++) {
            int ano = evento.inicio.getYear() + n * evento.regra.intervalo;
            if (ano > ate.getYear() + 1) break;
            List<Integer> meses = mesesDoAno(evento);
            List<LocalDate> datas = new ArrayList<>();
            for (int i = 0; i < meses.size(); i++) {
                YearMonth mes = YearMonth.of(ano, meses.get(i));
                datas.addAll(datasDoMes(evento, mes));
            }
            Collections.sort(datas);
            if (!aplicarDatas(datas, evento, de, ate, contados, maximo, saida, pulos)) break;
        }
    }

    private static boolean aplicarDatas(List<LocalDate> datas, Evento evento, ZonedDateTime de, ZonedDateTime ate,
                                        int[] contados, int maximo, List<ZonedDateTime> saida, List<ZonedDateTime> pulos) {
        for (int i = 0; i < datas.size(); i++) {
            if (!registrar(saida, comHora(evento, datas.get(i)), evento, de, ate, contados, maximo, pulos)) {
                return false;
            }
        }
        return true;
    }

    /** Continua enquanto o retorno for verdadeiro. */
    private static boolean registrar(List<ZonedDateTime> saida, ZonedDateTime cand, Evento evento,
                                     ZonedDateTime de, ZonedDateTime ate, int[] contados, int maximo,
                                     List<ZonedDateTime> pulos) {
        if (cand.toInstant().isBefore(evento.inicio.toInstant())) return true;
        if (evento.regra != null && evento.regra.ate != null
                && cand.toInstant().isAfter(evento.regra.ate.toInstant())) {
            return false;
        }
        if (!cand.isBefore(ate)) return false;
        contados[0]++;
        if (contados[0] > maximo) return false;
        if (!terminaAntes(evento, cand, de) && !pulado(evento, cand, pulos)) saida.add(cand);
        return contados[0] < maximo;
    }

    private static boolean pulado(Evento evento, ZonedDateTime cand, List<ZonedDateTime> pulos) {
        for (int i = 0; i < pulos.size(); i++) {
            ZonedDateTime pulo = pulos.get(i);
            if (evento.diaInteiro) {
                if (cand.toLocalDate().equals(pulo.toLocalDate())) return true;
            } else if (cand.toInstant().equals(pulo.toInstant())) {
                return true;
            }
        }
        return false;
    }

    private static boolean entra(Evento evento, ZonedDateTime inicio, ZonedDateTime de, ZonedDateTime ate) {
        if (!inicio.isBefore(ate)) return false;
        return !terminaAntes(evento, inicio, de);
    }

    private static boolean terminaAntes(Evento evento, ZonedDateTime inicio, ZonedDateTime de) {
        return !fimDe(evento, inicio).isAfter(de);
    }

    private static ZonedDateTime fimDe(Evento evento, ZonedDateTime inicio) {
        if (evento.diaInteiro) {
            long dias = ChronoUnit.DAYS.between(evento.inicio.toLocalDate(), evento.fim.toLocalDate());
            if (dias < 1) dias = 1;
            return inicio.toLocalDate().plusDays(dias).atStartOfDay(inicio.getZone());
        }
        Duration duracao = Duration.between(evento.inicio, evento.fim);
        if (duracao.isZero() || duracao.isNegative()) duracao = Duration.ofHours(1);
        return inicio.plus(duracao);
    }

    private static boolean casaDia(Evento.Regra regra, ZonedDateTime cand) {
        if (regra.dias.isEmpty()) return true;
        for (int i = 0; i < regra.dias.size(); i++) {
            if (regra.dias.get(i).dia == cand.getDayOfWeek()) return true;
        }
        return false;
    }

    private static boolean mesPermitido(Evento.Regra regra, int mes) {
        if (regra.meses.isEmpty()) return true;
        for (int i = 0; i < regra.meses.size(); i++) {
            if (regra.meses.get(i) == mes) return true;
        }
        return false;
    }

    private static List<Evento.Dia> diasDaSemana(Evento evento) {
        List<Evento.Dia> dias = new ArrayList<>();
        if (evento.regra.dias.isEmpty()) {
            dias.add(new Evento.Dia(evento.inicio.getDayOfWeek(), 0));
        } else {
            dias.addAll(evento.regra.dias);
        }
        dias.sort(Comparator.comparingInt(d -> ordem(d.dia, evento.regra.inicioSemana)));
        return dias;
    }

    private static int ordem(DayOfWeek dia, DayOfWeek wkst) {
        return (dia.getValue() - wkst.getValue() + 7) % 7;
    }

    private static LocalDate inicioDaSemana(LocalDate data, DayOfWeek wkst) {
        int diff = (data.getDayOfWeek().getValue() - wkst.getValue() + 7) % 7;
        return data.minusDays(diff);
    }

    private static List<Integer> mesesDoAno(Evento evento) {
        if (!evento.regra.meses.isEmpty()) return evento.regra.meses;
        if (!evento.regra.dias.isEmpty()) {
            return Collections.singletonList(evento.inicio.getMonthValue());
        }
        return Collections.singletonList(evento.inicio.getMonthValue());
    }

    private static List<LocalDate> datasDoMes(Evento evento, YearMonth mes) {
        List<LocalDate> datas = new ArrayList<>();
        if (evento.regra.dias.isEmpty()) {
            List<Integer> dias = evento.regra.diasDoMes.isEmpty()
                    ? Collections.singletonList(evento.inicio.getDayOfMonth())
                    : evento.regra.diasDoMes;
            for (int i = 0; i < dias.size(); i++) {
                LocalDate data = diaNoMes(mes, dias.get(i));
                if (data != null) datas.add(data);
            }
            return datas;
        }
        for (int i = 0; i < evento.regra.dias.size(); i++) {
            Evento.Dia regra = evento.regra.dias.get(i);
            if (regra.ordinal == 0) datas.addAll(todos(mes, regra.dia));
            else {
                LocalDate data = enesimo(mes, regra.dia, regra.ordinal);
                if (data != null) datas.add(data);
            }
        }
        datas.sort(Comparator.naturalOrder());
        return datas;
    }

    private static LocalDate diaNoMes(YearMonth mes, int dia) {
        if (dia > 0) return dia <= mes.lengthOfMonth() ? mes.atDay(dia) : null;
        int alvo = mes.lengthOfMonth() + dia + 1;
        return alvo >= 1 && alvo <= mes.lengthOfMonth() ? mes.atDay(alvo) : null;
    }

    private static List<LocalDate> todos(YearMonth mes, DayOfWeek dia) {
        List<LocalDate> lista = new ArrayList<>();
        LocalDate primeiro = mes.atDay(1);
        int diff = (dia.getValue() - primeiro.getDayOfWeek().getValue() + 7) % 7;
        for (LocalDate c = primeiro.plusDays(diff); c.getMonthValue() == mes.getMonthValue(); c = c.plusWeeks(1)) {
            lista.add(c);
        }
        return lista;
    }

    private static LocalDate enesimo(YearMonth mes, DayOfWeek dia, int ordinal) {
        if (ordinal > 0) {
            LocalDate primeiro = todos(mes, dia).isEmpty() ? null : todos(mes, dia).get(0);
            if (primeiro == null) return null;
            LocalDate alvo = primeiro.plusWeeks(ordinal - 1L);
            return alvo.getMonthValue() == mes.getMonthValue() ? alvo : null;
        }
        List<LocalDate> lista = todos(mes, dia);
        if (lista.isEmpty()) return null;
        int indice = lista.size() + ordinal;
        if (indice < 0 || indice >= lista.size()) return null;
        return lista.get(indice);
    }

    private static ZonedDateTime comHora(Evento evento, LocalDate data) {
        if (evento.diaInteiro) return data.atStartOfDay(evento.inicio.getZone());
        return ZonedDateTime.of(data, evento.inicio.toLocalTime(), evento.inicio.getZone());
    }

    private static int indiceInicial(Evento evento, ZonedDateTime de) {
        if (evento.regra.contagem != null) return 0;
        LocalDate alvo = de.toLocalDate().minusDays(120);
        LocalDate inicio = evento.inicio.toLocalDate();
        if (!alvo.isAfter(inicio)) return 0;
        int intervalo = evento.regra.intervalo;
        long bruto;
        switch (evento.regra.frequencia) {
            case DIARIA:
                bruto = ChronoUnit.DAYS.between(inicio, alvo) / intervalo;
                break;
            case SEMANAL:
                bruto = ChronoUnit.WEEKS.between(inicioDaSemana(inicio, evento.regra.inicioSemana), alvo) / intervalo;
                break;
            case MENSAL:
                bruto = ChronoUnit.MONTHS.between(YearMonth.from(inicio), YearMonth.from(alvo)) / intervalo;
                break;
            case ANUAL:
                bruto = ChronoUnit.YEARS.between(inicio, alvo) / intervalo;
                break;
            default:
                bruto = 0;
                break;
        }
        if (bruto < 0) return 0;
        if (bruto > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) bruto;
    }
}
