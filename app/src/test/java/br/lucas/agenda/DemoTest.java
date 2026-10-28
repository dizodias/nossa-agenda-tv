package br.lucas.agenda;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DemoTest {
    @Test
    public void arquivoDemoCabeNaSemanaCorrente() throws Exception {
        File arquivo = new File("src/main/assets/demo.ics");
        if (!arquivo.isFile()) arquivo = new File("app/src/main/assets/demo.ics");
        assertTrue(arquivo.isFile());
        String ics;
        try (FileInputStream entrada = new FileInputStream(arquivo)) {
            ics = Texto.ler(entrada);
        }
        String baixo = ics.toLowerCase(Locale.ROOT);
        assertFalse(baixo.contains("lucas"));
        assertFalse(baixo.contains("gmail"));

        Matcher marca = Pattern.compile("X-DEMO-ANCORA:(\\d{8})").matcher(ics);
        assertTrue(marca.find());
        LocalDate ancora = LocalDate.parse(marca.group(1), DateTimeFormatter.BASIC_ISO_DATE);
        assertEquals(DayOfWeek.MONDAY, ancora.getDayOfWeek());

        List<Evento> modelo = Ics.ler(ics);
        assertEquals(15, modelo.size());
        conferirSemana(modelo, ancora);
        assertTrue(titulos(modelo).contains("Aniversário da vó"));
        assertTrue(titulos(modelo).contains("Veterinário do Thor"));

        LocalDate hoje = LocalDate.of(2026, 10, 28);
        List<Evento> deslocados = Ics.ler(Demo.deslocar(ics, hoje));
        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        conferirSemana(deslocados, segunda);
        boolean academiaNaSegunda = false;
        for (int i = 0; i < deslocados.size(); i++) {
            Evento evento = deslocados.get(i);
            if (!"Academia".equals(evento.titulo)) continue;
            if (!evento.inicio.toLocalDate().equals(segunda)) continue;
            assertEquals(6, evento.inicio.getHour());
            assertEquals(30, evento.inicio.getMinute());
            academiaNaSegunda = true;
        }
        assertTrue(academiaNaSegunda);

        List<Evento> atual = Ics.ler(Demo.paraSemanaAtual(ics));
        LocalDate estaSemana = LocalDate.now(Horario.SAO_PAULO)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        conferirSemana(atual, estaSemana);
    }

    private static void conferirSemana(List<Evento> eventos, LocalDate segunda) {
        Set<String> esperados = new HashSet<>();
        esperados.add("Academia");
        esperados.add("Futebol das crianças");
        esperados.add("Mercado");
        esperados.add("Dentista");
        esperados.add("Aniversário da vó");
        esperados.add("Reunião de pais");
        esperados.add("Cinema em família");
        esperados.add("Veterinário do Thor");
        esperados.add("Aula de inglês");
        esperados.add("Jantar com amigos");
        boolean local = false;
        boolean descricao = false;
        boolean diaInteiro = false;
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < eventos.size(); i++) {
            Evento evento = eventos.get(i);
            LocalDate dia = evento.inicio.toLocalDate();
            assertFalse(dia.isBefore(segunda));
            assertTrue(dia.isBefore(segunda.plusDays(7)));
            if (!evento.local.isEmpty()) local = true;
            if (!evento.descricao.isEmpty()) descricao = true;
            if (evento.diaInteiro) {
                diaInteiro = true;
                assertEquals("Aniversário da vó", evento.titulo);
            }
            vistos.add(evento.titulo);
        }
        assertTrue(vistos.containsAll(esperados));
        assertTrue(local);
        assertTrue(descricao);
        assertTrue(diaInteiro);
    }

    private static Set<String> titulos(List<Evento> eventos) {
        Set<String> nomes = new HashSet<>();
        for (int i = 0; i < eventos.size(); i++) nomes.add(eventos.get(i).titulo);
        return nomes;
    }
}
