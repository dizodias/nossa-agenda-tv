# Nossa Agenda (Fire TV)

Um app Android TV/Fire TV (Fire OS 6, Android 7.1, minSdk 25) chamado "Nossa Agenda", com package br.lucas.agenda, em Java e sem libs pesadas.

## Dados
- Versão pública, sem conta pessoal. A agenda de exemplo fica em `app/src/main/assets/demo.ics`.
- Com `ICAL_URL` vazio, o app usa esse arquivo. As datas do modelo são deslocadas para a semana corrente e o selo DEMO aparece ao lado de FAMÍLIA.
- `secrets.properties` não faz parte desta pasta e não deve ser criado aqui. Se um dia existir com `ICAL_URL`, o app usa essa agenda no lugar da demo. O endereço entra via BuildConfig e não aparece em log.
- Faça o parse do ICS com um parser simples próprio (VEVENT: SUMMARY, DTSTART/DTEND com TZID e dia inteiro, LOCATION, DESCRIPTION, RRULE básica: DAILY/WEEKLY/MONTHLY/YEARLY com COUNT/UNTIL/BYDAY). Atualize ao abrir e a cada 15 min, com cache local para abrir offline.
- Use HttpURLConnection e leia os bytes com new String(bytes, UTF_8). Não use ByteArrayOutputStream.toString(Charset), que crasha no Android 7.

## Telas
1. Lista: os próximos compromissos agrupados por dia ("Hoje", "Amanhã", "qui 30/10"), mostrando hora, título e local. Navegação pelo D-pad, com foco bem visível.
2. Ao clicar num compromisso: abre a visão de agenda otimizada para TV. É uma grade da semana em 7 colunas, com o compromisso escolhido em destaque, e a tela de detalhes mostra título, data/hora, local e descrição. Esquerda/direita troca de semana.

## Visual (igual ao launcher e ao app SPFC)
- Fundo escuro (#0F0F10), cartões #1C1C1E com cantos de 14dp, texto branco e cinza #A6FFFFFF.
- Tipografia estilo Apple: sans-serif-medium nos títulos e sans-serif-light na hora grande. Letras maiúsculas espaçadas (letterSpacing 0.14) nos rótulos.
- Cor de destaque azul #0A84FF (a "família"). Foco com o cartão aumentando 1.05x e borda branca.
- Ícone e banner (320×180 em drawable-nodpi/banner.png): gradiente escuro, título em negrito.
- Precisa ter android:banner e a categoria LEANBACK_LAUNCHER.

## Entrega
Compile com `./gradlew assembleDebug`. Não instale na TV.
