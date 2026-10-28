package br.lucas.agenda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Cache local do ICS para a agenda abrir sem rede. */
final class AgendaRepository {
    static final long INTERVALO_MS = 15L * 60L * 1000L;
    private final File ics;
    private final File quando;

    AgendaRepository(File pasta) {
        this.ics = new File(pasta, "agenda.ics");
        this.quando = new File(pasta, "agenda.quando");
    }

    /** Nulo quando ainda não há cache. */
    List<Evento> ler() {
        if (!ics.exists()) return null;
        try (FileInputStream entrada = new FileInputStream(ics)) {
            return Ics.ler(Texto.ler(entrada));
        } catch (IOException e) {
            return null;
        }
    }

    long quando() {
        if (!quando.exists()) return ics.exists() ? ics.lastModified() : 0L;
        try (FileInputStream entrada = new FileInputStream(quando)) {
            return Long.parseLong(Texto.ler(entrada).trim());
        } catch (Exception e) {
            return ics.lastModified();
        }
    }

    void gravar(String corpo) throws IOException {
        File pasta = ics.getParentFile();
        if (pasta != null && !pasta.exists() && !pasta.mkdirs()) {
            throw new IOException("sem pasta de cache");
        }
        escrever(ics, corpo.getBytes(StandardCharsets.UTF_8));
        escrever(quando, Long.toString(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
    }

    private static void escrever(File destino, byte[] bytes) throws IOException {
        File pasta = destino.getParentFile();
        File temporario = new File(pasta, destino.getName() + ".tmp");
        try (FileOutputStream saida = new FileOutputStream(temporario)) {
            saida.write(bytes);
        }
        if (destino.exists() && !destino.delete()) {
            throw new IOException("não apagou o cache antigo");
        }
        if (!temporario.renameTo(destino)) {
            throw new IOException("não gravou o cache");
        }
    }
}
