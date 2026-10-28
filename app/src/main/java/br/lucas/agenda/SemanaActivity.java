package br.lucas.agenda;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Grade da semana. O foco abre no compromisso clicado; esquerda e direita trocam a semana. */
public final class SemanaActivity extends Activity implements Sincronia.Callback {
    static final String EXTRA_UID = "uid";
    static final String EXTRA_INICIO = "inicio";

    private final List<Evento> eventos = new ArrayList<>();
    private final List<Ocorrencia> daSemana = new ArrayList<>();
    private final TextView[] rotulos = new TextView[7];
    private final TextView[] numeros = new TextView[7];
    private final LinearLayout[] colunas = new LinearLayout[7];
    private final ScrollView[] rolagens = new ScrollView[7];
    private Sincronia sincronia;
    private TextView intervalo;
    private TextView detalheTitulo;
    private TextView detalheQuando;
    private TextView detalheLocal;
    private TextView detalheDescricao;
    private LocalDate segunda;
    private String uidPedido = "";
    private long inicioPedido = Long.MIN_VALUE;
    private int selecionado = -1;
    private boolean montando;
    private long ultimoToque;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_semana);
        intervalo = findViewById(R.id.intervalo);
        detalheTitulo = findViewById(R.id.detalhe_titulo);
        detalheQuando = findViewById(R.id.detalhe_quando);
        detalheLocal = findViewById(R.id.detalhe_local);
        detalheDescricao = findViewById(R.id.detalhe_descricao);
        LinearLayout grade = findViewById(R.id.grade);
        for (int i = 0; i < 7; i++) {
            View coluna = getLayoutInflater().inflate(R.layout.coluna_dia, grade, false);
            rotulos[i] = coluna.findViewById(R.id.coluna_rotulo);
            numeros[i] = coluna.findViewById(R.id.coluna_numero);
            colunas[i] = coluna.findViewById(R.id.coluna_itens);
            rolagens[i] = coluna.findViewById(R.id.coluna_rolagem);
            // O construtor do ScrollView liga o foco depois do XML e rouba o D-pad.
            rolagens[i].setFocusable(false);
            rolagens[i].setFocusableInTouchMode(false);
            rolagens[i].setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            grade.addView(coluna);
        }
        uidPedido = getIntent().getStringExtra(EXTRA_UID);
        if (uidPedido == null) uidPedido = "";
        inicioPedido = getIntent().getLongExtra(EXTRA_INICIO, Long.MIN_VALUE);
        LocalDate dia = inicioPedido == Long.MIN_VALUE
                ? LocalDate.now(Horario.SAO_PAULO)
                : java.time.Instant.ofEpochMilli(inicioPedido).atZone(Horario.SAO_PAULO).toLocalDate();
        segunda = dia.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int tecla = event.getKeyCode();
            if (tecla == KeyEvent.KEYCODE_DPAD_LEFT) return moverSemana(-1);
            if (tecla == KeyEvent.KEYCODE_DPAD_RIGHT) return moverSemana(1);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void aoCarregar() {
        if (eventos.isEmpty()) {
            detalheTitulo.setText(R.string.carregando);
            detalheQuando.setText("");
            detalheLocal.setVisibility(View.GONE);
            detalheDescricao.setVisibility(View.GONE);
        }
    }

    @Override
    public void aoPronto(List<Evento> novos, long quandoMillis, boolean offline) {
        eventos.clear();
        eventos.addAll(novos);
        mostrarSemana();
    }

    @Override
    public void aoFalharSemCache() {
        detalheTitulo.setText(R.string.sem_rede);
        detalheQuando.setText("");
        detalheLocal.setVisibility(View.GONE);
        detalheDescricao.setVisibility(View.GONE);
    }

    private boolean moverSemana(int delta) {
        if (!aceitaToque()) return true;
        segunda = segunda.plusWeeks(delta);
        uidPedido = "";
        inicioPedido = Long.MIN_VALUE;
        selecionado = -1;
        mostrarSemana();
        return true;
    }

    private boolean aceitaToque() {
        long agora = SystemClock.uptimeMillis();
        if (agora - ultimoToque < 160) return false;
        ultimoToque = agora;
        return true;
    }

    private void mostrarSemana() {
        ZonedDateTime de = segunda.atStartOfDay(Horario.SAO_PAULO);
        ZonedDateTime ate = segunda.plusDays(7).atStartOfDay(Horario.SAO_PAULO);
        daSemana.clear();
        daSemana.addAll(Recorrencia.naJanela(eventos, de, ate));
        int indice = Selecao.indice(daSemana, uidPedido, inicioPedido);
        if (indice >= 0) selecionado = indice;
        else if (uidPedido.isEmpty()) selecionado = daSemana.isEmpty() ? -1 : 0;
        else selecionado = -1;
        pintar();
    }

    private void pintar() {
        montando = true;
        intervalo.setText(Horario.faixaSemana(segunda));
        LocalDate hoje = LocalDate.now(Horario.SAO_PAULO);
        TextView alvo = null;
        for (int c = 0; c < 7; c++) {
            LocalDate dia = segunda.plusDays(c);
            rotulos[c].setText(Horario.sigla(dia));
            numeros[c].setText(String.valueOf(dia.getDayOfMonth()));
            numeros[c].setTextColor(getColor(dia.equals(hoje) ? R.color.azul : R.color.branco));
            colunas[c].removeAllViews();
            for (int i = 0; i < daSemana.size(); i++) {
                Ocorrencia o = daSemana.get(i);
                if (!ocupa(o, dia)) continue;
                TextView chip = (TextView) getLayoutInflater().inflate(R.layout.item_chip, colunas[c], false);
                chip.setText(o.diaInteiro ? o.titulo : Horario.hora(o.inicio) + "  " + o.titulo);
                chip.setTag(i);
                chip.setFocusable(true);
                chip.setFocusableInTouchMode(true);
                if (Build.VERSION.SDK_INT >= 26) chip.setDefaultFocusHighlightEnabled(false);
                chip.setOnFocusChangeListener((v, focado) -> {
                    if (focado) v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(140).start();
                    else v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                    if (montando || !focado) {
                        pintarChip((TextView) v);
                        return;
                    }
                    selecionado = (int) v.getTag();
                    guardarPedido();
                    pintarChips();
                    pintarDetalhe();
                });
                pintarChip(chip);
                colunas[c].addView(chip);
                if (i == selecionado && o.inicio.toLocalDate().equals(dia)) alvo = chip;
            }
        }
        montando = false;
        pintarDetalhe();
        if (alvo != null) focar(alvo);
    }

    private void focar(TextView chip) {
        chip.requestFocus();
        chip.post(() -> {
            chip.requestFocus();
            chip.requestRectangleOnScreen(new Rect(0, 0, chip.getWidth(), chip.getHeight()), true);
        });
    }

    private void guardarPedido() {
        if (selecionado < 0 || selecionado >= daSemana.size()) return;
        Ocorrencia o = daSemana.get(selecionado);
        uidPedido = o.uid;
        inicioPedido = o.inicioMillis();
    }

    private void pintarChips() {
        for (int c = 0; c < 7; c++) {
            for (int i = 0; i < colunas[c].getChildCount(); i++) {
                View filho = colunas[c].getChildAt(i);
                if (filho instanceof TextView) pintarChip((TextView) filho);
            }
        }
    }

    private void pintarChip(TextView chip) {
        Object marca = chip.getTag();
        boolean destaque = marca instanceof Integer && (int) marca == selecionado;
        chip.setBackgroundResource(destaque ? R.drawable.chip_destaque : R.drawable.chip);
    }

    private void pintarDetalhe() {
        if (selecionado < 0 || selecionado >= daSemana.size()) {
            detalheTitulo.setText(selecionado < 0 && !uidPedido.isEmpty()
                    ? R.string.carregando
                    : R.string.nada_semana);
            detalheQuando.setText("");
            detalheLocal.setVisibility(View.GONE);
            detalheDescricao.setVisibility(View.GONE);
            return;
        }
        Ocorrencia o = daSemana.get(selecionado);
        detalheTitulo.setText(o.titulo);
        detalheQuando.setText(textoQuando(o));
        if (o.local.isEmpty()) detalheLocal.setVisibility(View.GONE);
        else {
            detalheLocal.setVisibility(View.VISIBLE);
            detalheLocal.setText(o.local);
        }
        if (o.descricao.isEmpty()) detalheDescricao.setVisibility(View.GONE);
        else {
            detalheDescricao.setVisibility(View.VISIBLE);
            detalheDescricao.setText(o.descricao);
        }
    }

    private String textoQuando(Ocorrencia o) {
        if (o.diaInteiro) {
            LocalDate ini = o.inicio.toLocalDate();
            LocalDate fim = o.fim.toLocalDate().minusDays(1);
            String data = fim.isAfter(ini)
                    ? Horario.dataCurta(ini) + " – " + Horario.dataCurta(fim)
                    : Horario.dataCurta(ini);
            return data + " · " + getString(R.string.dia_inteiro);
        }
        String horas = Horario.hora(o.inicio) + "–" + Horario.hora(o.fim);
        if (!o.fim.toLocalDate().equals(o.inicio.toLocalDate())) {
            horas = Horario.hora(o.inicio) + " – " + Horario.dataCurta(o.fim.toLocalDate())
                    + " " + Horario.hora(o.fim);
        }
        return Horario.dataCurta(o.inicio.toLocalDate()) + " · " + horas;
    }

    private static boolean ocupa(Ocorrencia o, LocalDate dia) {
        ZonedDateTime ini = dia.atStartOfDay(Horario.SAO_PAULO);
        ZonedDateTime fim = dia.plusDays(1).atStartOfDay(Horario.SAO_PAULO);
        return o.inicio.toInstant().isBefore(fim.toInstant()) && o.fim.toInstant().isAfter(ini.toInstant());
    }
}
