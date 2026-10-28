package br.lucas.agenda;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Lê a resposta inteira em bytes. No Fire OS 6, toString(Charset) derruba o app. */
final class Texto {
    private Texto() { }

    static String ler(InputStream entrada) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        byte[] bloco = new byte[8192];
        int lido;
        while ((lido = entrada.read(bloco)) >= 0) saida.write(bloco, 0, lido);
        return new String(saida.toByteArray(), StandardCharsets.UTF_8);
    }
}
