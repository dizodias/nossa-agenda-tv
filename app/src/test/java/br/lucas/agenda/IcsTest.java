package br.lucas.agenda;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IcsTest {
    private static final ZonedDateTime DESDE = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, Horario.SAO_PAULO);
    private static final ZonedDateTime ATE = ZonedDateTime.of(2028, 1, 1, 0, 0, 0, 0, Horario.SAO_PAULO);

    @Test
    public void leHorarioComFusoLocalEDescricaoDobrável() {
        String ics = ""
                + "BEGIN:VCALENDAR\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:jantar\r\n"
                + "SUMMARY:Reunião \r\n"
                + " de família\r\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261028T190000\r\n"
                + "DTEND;TZID=America/Sao_Paulo:20261028T210000\r\n"
                + "LOCATION:Vila Madalena\r\n"
                + "DESCRIPTION:Levar bolo\\, suco e\\nvelas\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";
        List<Evento> eventos = Ics.ler(ics);
        assertEquals(1, eventos.size());
        Evento evento = eventos.get(0);
        assertEquals("jantar", evento.uid);
        assertEquals("Reunião de família", evento.titulo);
        assertEquals("Vila Madalena", evento.local);
        assertEquals("Levar bolo, suco e\nvelas", evento.descricao);
        assertFalse(evento.diaInteiro);
        assertEquals(19, evento.inicio.getHour());
        assertEquals(28, evento.inicio.getDayOfMonth());
        assertEquals(21, evento.fim.getHour());
        assertNull(evento.regra);
    }

    @Test
    public void converteUtcParaSaoPaulo() {
        String ics = "BEGIN:VEVENT\n"
                + "UID:utc\n"
                + "SUMMARY:Cinema\n"
                + "DTSTART:20261028T220000Z\n"
                + "DTEND:20261029T000000Z\n"
                + "END:VEVENT\n";
        Evento evento = Ics.ler(ics).get(0);
        assertEquals(19, evento.inicio.getHour());
        assertEquals(28, evento.inicio.getDayOfMonth());
        assertEquals(21, evento.fim.getHour());
    }

    @Test
    public void diaInteiroNaoRecuaOFuso() {
        String ics = "BEGIN:VEVENT\n"
                + "UID:niver\n"
                + "SUMMARY:Aniversário\n"
                + "DTSTART;VALUE=DATE:20261030\n"
                + "DTEND;VALUE=DATE:20261031\n"
                + "RRULE:FREQ=YEARLY;COUNT=2\n"
                + "END:VEVENT\n";
        List<Ocorrencia> lista = Recorrencia.expandir(Ics.ler(ics).get(0), DESDE, ATE);
        assertEquals(2, lista.size());
        assertTrue(lista.get(0).diaInteiro);
        assertEquals(LocalDate.of(2026, 10, 30), lista.get(0).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 31), lista.get(0).fim.toLocalDate());
        assertEquals(LocalDate.of(2027, 10, 30), lista.get(1).inicio.toLocalDate());
    }

    @Test
    public void semanalComDiasEContagem() {
        String ics = "BEGIN:VEVENT\n"
                + "UID:academia\n"
                + "SUMMARY:Academia\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261026T080000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261026T090000\n"
                + "RRULE:FREQ=WEEKLY;BYDAY=WE,MO;COUNT=4\n"
                + "END:VEVENT\n";
        Evento evento = Ics.ler(ics).get(0);
        assertEquals(DayOfWeek.MONDAY, evento.inicio.getDayOfWeek());
        List<Ocorrencia> lista = Recorrencia.expandir(evento, DESDE, ATE);
        assertEquals(4, lista.size());
        assertEquals(LocalDate.of(2026, 10, 26), lista.get(0).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 28), lista.get(1).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 2), lista.get(2).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 4), lista.get(3).inicio.toLocalDate());
        assertEquals(8, lista.get(3).inicio.getHour());
    }

    @Test
    public void diariaComIntervaloEUntil() {
        String ics = "BEGIN:VEVENT\n"
                + "SUMMARY:Remédio\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261028T100000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261028T101500\n"
                + "RRULE:FREQ=DAILY;INTERVAL=2;UNTIL=20261101T130000Z\n"
                + "END:VEVENT\n";
        List<Ocorrencia> lista = Recorrencia.expandir(Ics.ler(ics).get(0), DESDE, ATE);
        assertEquals(3, lista.size());
        assertEquals(LocalDate.of(2026, 10, 28), lista.get(0).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 30), lista.get(1).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 1), lista.get(2).inicio.toLocalDate());
    }

    @Test
    public void mensalNaPrimeiraQuarta() {
        String ics = "BEGIN:VEVENT\n"
                + "SUMMARY:Conselho\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261007T090000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261007T100000\n"
                + "RRULE:FREQ=MONTHLY;BYDAY=1WE;COUNT=3\n"
                + "END:VEVENT\n";
        List<Ocorrencia> lista = Recorrencia.expandir(Ics.ler(ics).get(0), DESDE, ATE);
        assertEquals(3, lista.size());
        assertEquals(LocalDate.of(2026, 10, 7), lista.get(0).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 11, 4), lista.get(1).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 12, 2), lista.get(2).inicio.toLocalDate());
    }

    @Test
    public void ignoraCanceladoERepeteDesdeOPassado() {
        String ics = "BEGIN:VEVENT\n"
                + "SUMMARY:Cancelado\n"
                + "STATUS:CANCELLED\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261028T120000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261028T130000\n"
                + "END:VEVENT\n"
                + "BEGIN:VEVENT\n"
                + "SUMMARY:Todo dia\n"
                + "DTSTART;TZID=America/Sao_Paulo:20200101T070000\n"
                + "DTEND;TZID=America/Sao_Paulo:20200101T073000\n"
                + "RRULE:FREQ=DAILY\n"
                + "END:VEVENT\n";
        List<Evento> eventos = Ics.ler(ics);
        assertEquals(1, eventos.size());
        ZonedDateTime de = LocalDate.of(2026, 10, 28).atStartOfDay(Horario.SAO_PAULO);
        ZonedDateTime ate = LocalDate.of(2026, 10, 29).atStartOfDay(Horario.SAO_PAULO);
        List<Ocorrencia> lista = Recorrencia.naJanela(eventos, de, ate);
        assertEquals(1, lista.size());
        assertEquals(LocalDate.of(2026, 10, 28), lista.get(0).inicio.toLocalDate());
        assertEquals(7, lista.get(0).inicio.getHour());
    }

    @Test
    public void excecaoSubstituiEExdateRemove() {
        String ics = "BEGIN:VEVENT\n"
                + "UID:serie\n"
                + "SUMMARY:Academia\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261026T080000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261026T090000\n"
                + "RRULE:FREQ=WEEKLY;COUNT=3\n"
                + "EXDATE;TZID=America/Sao_Paulo:20261102T080000\n"
                + "END:VEVENT\n"
                + "BEGIN:VEVENT\n"
                + "UID:serie\n"
                + "SUMMARY:Academia especial\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261026T150000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261026T160000\n"
                + "RECURRENCE-ID;TZID=America/Sao_Paulo:20261026T080000\n"
                + "END:VEVENT\n";
        List<Ocorrencia> lista = Recorrencia.naJanela(Ics.ler(ics), DESDE, ATE);
        assertEquals(2, lista.size());
        assertEquals("Academia especial", lista.get(0).titulo);
        assertEquals(15, lista.get(0).inicio.getHour());
        assertEquals(LocalDate.of(2026, 10, 26), lista.get(0).inicio.toLocalDate());
        assertEquals("Academia", lista.get(1).titulo);
        assertEquals(LocalDate.of(2026, 11, 9), lista.get(1).inicio.toLocalDate());
        assertEquals(8, lista.get(1).inicio.getHour());
    }

    @Test
    public void mensalNoDiaDoMesComIntervalo() {
        String ics = "BEGIN:VEVENT\n"
                + "SUMMARY:Conta\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261015T090000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261015T093000\n"
                + "RRULE:FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=15;COUNT=3\n"
                + "END:VEVENT\n";
        List<Ocorrencia> lista = Recorrencia.expandir(Ics.ler(ics).get(0), DESDE, ATE);
        assertEquals(3, lista.size());
        assertEquals(LocalDate.of(2026, 10, 15), lista.get(0).inicio.toLocalDate());
        assertEquals(LocalDate.of(2026, 12, 15), lista.get(1).inicio.toLocalDate());
        assertEquals(LocalDate.of(2027, 2, 15), lista.get(2).inicio.toLocalDate());
    }

    @Test
    public void semanaAbreNaOcorrenciaClicadaENaoNaPrimeira() {
        String ics = "BEGIN:VEVENT\n"
                + "UID:reunioes\n"
                + "SUMMARY:Reunião semanal\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261026T090000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261026T100000\n"
                + "END:VEVENT\n"
                + "BEGIN:VEVENT\n"
                + "UID:cabelo\n"
                + "SUMMARY:Cortar Cabelo\n"
                + "DTSTART;TZID=America/Sao_Paulo:20261026T154000\n"
                + "DTEND;TZID=America/Sao_Paulo:20261026T163000\n"
                + "RRULE:FREQ=WEEKLY;BYDAY=MO,TH;COUNT=2\n"
                + "END:VEVENT\n";
        LocalDate segunda = LocalDate.of(2026, 10, 26);
        List<Ocorrencia> semana = Recorrencia.naJanela(
                Ics.ler(ics),
                segunda.atStartOfDay(Horario.SAO_PAULO),
                segunda.plusDays(7).atStartOfDay(Horario.SAO_PAULO));
        assertEquals(3, semana.size());
        assertEquals("Reunião semanal", semana.get(0).titulo);

        Ocorrencia clicada = null;
        for (int i = 0; i < semana.size(); i++) {
            Ocorrencia o = semana.get(i);
            if (o.inicio.toLocalDate().equals(LocalDate.of(2026, 10, 29))) clicada = o;
        }
        assertEquals("Cortar Cabelo", clicada.titulo);
        assertEquals(15, clicada.inicio.getHour());
        assertEquals(40, clicada.inicio.getMinute());
        assertEquals(semana.get(1).uid, clicada.uid);
        assertTrue(semana.get(1).inicioMillis() != clicada.inicioMillis());

        int indice = Selecao.indice(semana, clicada.uid, clicada.inicioMillis());
        assertEquals(2, indice);
        assertEquals("Cortar Cabelo", semana.get(indice).titulo);
        assertEquals(LocalDate.of(2026, 10, 29), semana.get(indice).inicio.toLocalDate());
        assertEquals(-1, Selecao.indice(semana, clicada.uid, semana.get(0).inicioMillis()));
    }

    @Test
    public void grupoDoDia() {
        LocalDate hoje = LocalDate.of(2026, 10, 28);
        assertEquals(DayOfWeek.WEDNESDAY, hoje.getDayOfWeek());
        assertEquals("Hoje", Horario.grupo(hoje, hoje));
        assertEquals("Amanhã", Horario.grupo(hoje.plusDays(1), hoje));
        assertEquals("sex 30/10", Horario.grupo(LocalDate.of(2026, 10, 30), hoje));
        assertEquals("26 out – 1 nov", Horario.faixaSemana(LocalDate.of(2026, 10, 26)));
    }
}
