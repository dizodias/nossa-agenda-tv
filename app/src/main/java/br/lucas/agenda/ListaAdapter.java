package br.lucas.agenda;

import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Próximos compromissos, com o dia como rótulo e o cartão focado pelo D-pad. */
final class ListaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final int TIPO_DIA = 0;
    static final int TIPO_ITEM = 1;

    interface AoAbrir {
        void abrir(Ocorrencia ocorrencia);
    }

    static final class Linha {
        final String rotulo;
        final Ocorrencia ocorrencia;

        private Linha(String rotulo, Ocorrencia ocorrencia) {
            this.rotulo = rotulo;
            this.ocorrencia = ocorrencia;
        }

        static Linha dia(String rotulo) {
            return new Linha(rotulo, null);
        }

        static Linha item(Ocorrencia ocorrencia) {
            return new Linha(null, ocorrencia);
        }

        boolean ehDia() {
            return ocorrencia == null;
        }
    }

    private final List<Linha> linhas = new ArrayList<>();
    private final AoAbrir aoAbrir;
    private String assinatura = "";

    ListaAdapter(AoAbrir aoAbrir) {
        this.aoAbrir = aoAbrir;
    }

    void definir(List<Linha> novas) {
        String nova = assinatura(novas);
        if (nova.equals(assinatura)) return;
        assinatura = nova;
        linhas.clear();
        linhas.addAll(novas);
        notifyDataSetChanged();
    }

    boolean ehDia(int posicao) {
        return posicao >= 0 && posicao < linhas.size() && linhas.get(posicao).ehDia();
    }

    @Override
    public int getItemViewType(int posicao) {
        return linhas.get(posicao).ehDia() ? TIPO_DIA : TIPO_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        LayoutInflater inflater = LayoutInflater.from(pai.getContext());
        if (tipo == TIPO_DIA) {
            return new Dia(inflater.inflate(R.layout.item_dia, pai, false));
        }
        View view = inflater.inflate(R.layout.item_compromisso, pai, false);
        if (Build.VERSION.SDK_INT >= 26) view.setDefaultFocusHighlightEnabled(false);
        view.setOnFocusChangeListener((v, focado) -> {
            float escala = focado ? 1.05f : 1f;
            v.animate().scaleX(escala).scaleY(escala).setDuration(140).start();
        });
        return new Item(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int posicao) {
        Linha linha = linhas.get(posicao);
        if (holder instanceof Dia) {
            ((Dia) holder).rotulo.setText(linha.rotulo);
            return;
        }
        ((Item) holder).preencher(linha.ocorrencia, aoAbrir);
    }

    @Override
    public int getItemCount() {
        return linhas.size();
    }

    private static String assinatura(List<Linha> novas) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < novas.size(); i++) {
            Linha linha = novas.get(i);
            if (linha.ehDia()) {
                texto.append('D').append(linha.rotulo);
            } else {
                Ocorrencia o = linha.ocorrencia;
                texto.append('I').append(o.uid).append('|').append(o.inicioMillis())
                        .append('|').append(o.titulo).append('|').append(o.local);
            }
            texto.append('\n');
        }
        return texto.toString();
    }

    static final class Dia extends RecyclerView.ViewHolder {
        final TextView rotulo;

        Dia(View view) {
            super(view);
            rotulo = (TextView) view;
        }
    }

    static final class Item extends RecyclerView.ViewHolder {
        private final TextView hora;
        private final TextView titulo;
        private final TextView local;

        Item(View view) {
            super(view);
            hora = view.findViewById(R.id.hora);
            titulo = view.findViewById(R.id.titulo);
            local = view.findViewById(R.id.local);
        }

        void preencher(Ocorrencia ocorrencia, AoAbrir aoAbrir) {
            if (ocorrencia.diaInteiro) {
                hora.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                hora.setText(R.string.dia_todo);
            } else {
                hora.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                hora.setText(Horario.hora(ocorrencia.inicio));
            }
            titulo.setText(ocorrencia.titulo);
            if (ocorrencia.local.isEmpty()) {
                local.setVisibility(View.GONE);
            } else {
                local.setVisibility(View.VISIBLE);
                local.setText(ocorrencia.local);
            }
            itemView.setOnClickListener(v -> aoAbrir.abrir(ocorrencia));
        }
    }
}
