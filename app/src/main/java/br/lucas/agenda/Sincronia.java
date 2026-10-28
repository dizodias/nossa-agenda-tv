package br.lucas.agenda;

import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Atualiza ao abrir e a cada 15 minutos. Sem endereço, usa o demo.ics. */
final class Sincronia {
    interface Callback {
        void aoCarregar();

        void aoPronto(List<Evento> eventos, long quandoMillis, boolean offline);

        void aoFalharSemCache();
    }

    private final AgendaRepository repositorio;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String endereco;
    private final String modeloDemo;
    private final Runnable periodo = new Runnable() {
        @Override
        public void run() {
            if (!ativo) return;
            atualizar();
            main.postDelayed(this, AgendaRepository.INTERVALO_MS);
        }
    };
    private Callback callback;
    private boolean ocupado;
    private boolean pendente;
    private boolean ativo;

    private Sincronia(File pasta, String endereco, String modeloDemo) {
        this.repositorio = new AgendaRepository(pasta);
        this.endereco = endereco == null ? "" : endereco.trim();
        this.modeloDemo = modeloDemo;
    }

    /** Remoto quando há endereço. Sem endereço, lê o calendário embutido. */
    static Sincronia abrir(File pasta, String endereco, AssetManager assets) {
        if (configurado(endereco)) return new Sincronia(pasta, endereco, null);
        return new Sincronia(pasta, "", lerModelo(assets));
    }

    static boolean configurado(String endereco) {
        return endereco != null && !endereco.trim().isEmpty();
    }

    private static String lerModelo(AssetManager assets) {
        try (InputStream entrada = assets.open("demo.ics")) {
            return Texto.ler(entrada);
        } catch (IOException e) {
            return "";
        }
    }

    void iniciar(Callback callback) {
        this.callback = callback;
        ativo = true;
        main.removeCallbacks(periodo);
        main.postDelayed(periodo, AgendaRepository.INTERVALO_MS);
        if (ocupado) {
            pendente = true;
            return;
        }
        atualizar();
    }

    void parar() {
        ativo = false;
        pendente = false;
        callback = null;
        main.removeCallbacks(periodo);
    }

    void encerrar() {
        parar();
        io.shutdownNow();
    }

    private void atualizar() {
        if (ocupado) return;
        if (modeloDemo != null) {
            atualizarDemo();
            return;
        }
        if (!configurado(endereco)) return;
        ocupado = true;
        pendente = false;
        io.execute(() -> {
            List<Evento> cache = repositorio.ler();
            long quandoCache = repositorio.quando();
            boolean temCache = cache != null;
            main.post(() -> {
                if (!ativo || callback == null) return;
                if (temCache) callback.aoPronto(cache, quandoCache, false);
                else callback.aoCarregar();
            });
            try {
                String corpo = IcalClient.baixar(endereco);
                repositorio.gravar(corpo);
                List<Evento> novos = Ics.ler(corpo);
                long agora = System.currentTimeMillis();
                main.post(() -> entregar(novos, agora, false));
            } catch (IOException e) {
                // A mensagem da rede pode citar o endereço. Não repassar o erro.
                main.post(() -> {
                    if (temCache) entregar(cache, quandoCache, true);
                    else entregarFalha();
                });
            }
        });
    }

    private void atualizarDemo() {
        ocupado = true;
        pendente = false;
        io.execute(() -> {
            List<Evento> eventos;
            try {
                eventos = Ics.ler(Demo.paraSemanaAtual(modeloDemo));
            } catch (RuntimeException e) {
                eventos = Collections.emptyList();
            }
            List<Evento> prontos = eventos;
            main.post(() -> entregar(prontos, 0L, false));
        });
    }

    private void entregar(List<Evento> eventos, long quando, boolean offline) {
        ocupado = false;
        Callback ouvinte = callback;
        boolean deNovo = pendente && ativo;
        pendente = false;
        if (ativo && ouvinte != null) ouvinte.aoPronto(eventos, quando, offline);
        if (deNovo) atualizar();
    }

    private void entregarFalha() {
        ocupado = false;
        Callback ouvinte = callback;
        boolean deNovo = pendente && ativo;
        pendente = false;
        if (ativo && ouvinte != null) ouvinte.aoFalharSemCache();
        if (deNovo) atualizar();
    }
}
