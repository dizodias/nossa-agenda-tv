package br.lucas.agenda;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Baixa o ICS. O endereço não entra em mensagem de erro. */
final class IcalClient {
    private IcalClient() { }

    static String baixar(String endereco) throws IOException {
        if (endereco == null || endereco.trim().isEmpty()) {
            throw new IOException("agenda sem endereço");
        }
        HttpURLConnection conexao;
        try {
            conexao = (HttpURLConnection) new URL(endereco.trim()).openConnection();
        } catch (IOException e) {
            throw new IOException("não abriu a agenda");
        }
        conexao.setConnectTimeout(15_000);
        conexao.setReadTimeout(20_000);
        conexao.setInstanceFollowRedirects(true);
        conexao.setRequestProperty("Accept", "text/calendar");
        conexao.setRequestProperty("User-Agent", "NossaAgenda/1.0");
        try {
            int codigo = conexao.getResponseCode();
            if (codigo < 200 || codigo >= 300) throw new IOException("HTTP " + codigo);
            InputStream entrada = conexao.getInputStream();
            if (entrada == null) throw new IOException("resposta vazia");
            return Texto.ler(entrada);
        } catch (IOException e) {
            String mensagem = e.getMessage();
            if (mensagem != null && mensagem.startsWith("HTTP ")) throw new IOException(mensagem);
            throw new IOException("falha ao baixar a agenda");
        } finally {
            conexao.disconnect();
        }
    }
}
