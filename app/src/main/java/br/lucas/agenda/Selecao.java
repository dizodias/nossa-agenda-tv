package br.lucas.agenda;

import java.util.List;

/** Acha a ocorrência clicada. Repetição usa o mesmo UID, então o início também entra. */
final class Selecao {
    private Selecao() { }

    /** -1 quando nenhuma ocorrência tem esse UID nesse início. */
    static int indice(List<Ocorrencia> itens, String uid, long inicioMillis) {
        if (uid == null || uid.isEmpty() || itens == null) return -1;
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).mesma(uid, inicioMillis)) return i;
        }
        return -1;
    }
}
