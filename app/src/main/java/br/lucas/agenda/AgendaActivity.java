package br.lucas.agenda;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Lista dos próximos compromissos. Sem endereço, mostra a agenda de demonstração. */
public final class AgendaActivity extends Activity implements Sincronia.Callback {
    private Sincronia sincronia;
    private ListaAdapter adapter;
    private View conteudo;
    private View painel;
    private View vazio;
    private RecyclerView lista;
    private TextView atualizado;
    private TextView mensagemTitulo;
    private TextView mensagemCorpo;
    private boolean jaFocou;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_agenda);
        conteudo = findViewById(R.id.conteudo);
        painel = findViewById(R.id.painel_mensagem);
        vazio = findViewById(R.id.vazio);
        lista = findViewById(R.id.lista);
        atualizado = findViewById(R.id.atualizado);
        mensagemTitulo = findViewById(R.id.mensagem_titulo);
        mensagemCorpo = findViewById(R.id.mensagem_corpo);

        if (!Sincronia.configurado(BuildConfig.ICAL_URL)) {
            findViewById(R.id.selo_demo).setVisibility(View.VISIBLE);
        }

        adapter = new ListaAdapter(ocorrencia -> {
            Intent destino = new Intent(this, SemanaActivity.class);
            destino.putExtra(SemanaActivity.EXTRA_UID, ocorrencia.uid);
            destino.putExtra(SemanaActivity.EXTRA_INICIO, ocorrencia.inicioMillis());
            startActivity(destino);
        });
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        lista.setItemAnimator(null);
        sincronia = Sincronia.abrir(getFilesDir(), BuildConfig.ICAL_URL, getAssets());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sincronia != null) sincronia.iniciar(this);
    }

    @Override
    protected void onPause() {
        if (sincronia != null) sincronia.parar();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (sincronia != null) sincronia.encerrar();
        super.onDestroy();
    }

    @Override
    public void aoCarregar() {
        if (conteudo.getVisibility() == View.VISIBLE) return;
        mostrarMensagem(R.string.carregando, 0);
    }

    @Override
    public void aoPronto(List<Evento> eventos, long quandoMillis, boolean offline) {
        List<ListaAdapter.Linha> linhas = linhas(eventos);
        painel.setVisibility(View.GONE);
        conteudo.setVisibility(View.VISIBLE);
        String hora = quandoMillis > 0 ? Horario.hora(quandoMillis) : "";
        if (hora.isEmpty()) atualizado.setText("");
        else atualizado.setText(getString(offline ? R.string.sem_rede_cache : R.string.atualizado, hora));
        if (linhas.isEmpty()) {
            lista.setVisibility(View.GONE);
            vazio.setVisibility(View.VISIBLE);
            return;
        }
        vazio.setVisibility(View.GONE);
        lista.setVisibility(View.VISIBLE);
        adapter.definir(linhas);
        if (lista.findFocus() == null) {
            jaFocou = false;
            lista.post(this::focarPrimeiro);
        }
    }

    @Override
    public void aoFalharSemCache() {
        mostrarMensagem(R.string.sem_rede, R.string.sem_rede_ajuda);
    }

    private void mostrarMensagem(int titulo, int corpo) {
        conteudo.setVisibility(View.GONE);
        painel.setVisibility(View.VISIBLE);
        mensagemTitulo.setText(titulo);
        if (corpo == 0) {
            mensagemCorpo.setVisibility(View.GONE);
        } else {
            mensagemCorpo.setVisibility(View.VISIBLE);
            mensagemCorpo.setText(corpo);
        }
    }

    private void focarPrimeiro() {
        if (jaFocou || adapter == null) return;
        int posicao = -1;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (!adapter.ehDia(i)) {
                posicao = i;
                break;
            }
        }
        if (posicao < 0) return;
        RecyclerView.ViewHolder holder = lista.findViewHolderForAdapterPosition(posicao);
        if (holder == null) return;
        jaFocou = true;
        holder.itemView.requestFocus();
    }

    private static List<ListaAdapter.Linha> linhas(List<Evento> eventos) {
        ZonedDateTime agora = ZonedDateTime.now(Horario.SAO_PAULO);
        LocalDate hoje = agora.toLocalDate();
        ZonedDateTime de = hoje.atStartOfDay(Horario.SAO_PAULO).minusDays(40);
        ZonedDateTime ate = agora.plusDays(240);
        List<Ocorrencia> ocorrencias = Recorrencia.naJanela(eventos, de, ate);
        List<ListaAdapter.Linha> linhas = new ArrayList<>();
        LocalDate anterior = null;
        for (int i = 0; i < ocorrencias.size(); i++) {
            Ocorrencia o = ocorrencias.get(i);
            if (!o.fim.isAfter(agora)) continue;
            LocalDate dia = o.inicio.toLocalDate();
            if (dia.isBefore(hoje)) dia = hoje;
            if (anterior == null || !anterior.equals(dia)) {
                linhas.add(ListaAdapter.Linha.dia(Horario.grupo(dia, hoje)));
                anterior = dia;
            }
            linhas.add(ListaAdapter.Linha.item(o));
        }
        return linhas;
    }
}
