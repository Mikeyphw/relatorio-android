# Relatório Mensal AD — Android

Aplicativo Android em pt-BR para preencher relatórios mensais, manter rascunhos offline, revisar inconsistências e gerar o PDF oficial.

## Estado atual

Esta entrega implementa em conjunto:

- **Fase 7 — Acessibilidade e usabilidade**;
- **Fase 8 — Histórico e manutenção segura**.

Dados do projeto:

- modelo padrão: `AD_TIMOTEO_2026_V1`;
- modelo alternativo: `RELATORIO_COMPLETO_V1`;
- package ID: `com.mikeyphw.relatorio.android`;
- target Devtool: `relatorio_android`;
- versão: `0.8.0-phase7-8`.

## O que funciona

- criação, salvamento e restauração de rascunho;
- dois modelos de formulário, com 33 ou 74 campos;
- preenchimento guiado com botões grandes;
- movimentações de membros e anotações;
- revisão com erros bloqueantes e avisos;
- conclusão e histórico local;
- PDF A4, visualização, salvamento e compartilhamento;
- letras grandes ou muito grandes;
- contraste reforçado opcional;
- ajuda em pt-BR dentro do aplicativo;
- pesquisa no histórico por mês, ano, congregação ou formulário;
- reutilização segura para o próximo mês;
- reabertura para correção;
- exclusão confirmada de relatório concluído;
- funcionamento offline, sem permissões de internet ou armazenamento amplo.

## Estrutura

```text
app/          Interface Compose, ViewModel, DataStore, PDF Android e composição
core-model/   Modelos, catálogos, revisão, histórico e conteúdo do PDF
persistence/  Banco Room, DAO, mapeadores e repositório
docs/         Escopo e critérios de cada fase
```

## Compilar

```bash
./gradlew :app:assembleDebug
```

## Validar

```bash
./gradlew \
  :app:compileDebugKotlin \
  :core-model:test \
  :persistence:testDebugUnitTest \
  :persistence:compileDebugAndroidTestKotlin \
  :app:testDebugUnitTest \
  :app:compileDebugAndroidTestKotlin \
  :app:lintDebug
```

Consulte `docs/PHASE_7_ACCESSIBILITY_USABILITY.md` e `docs/PHASE_8_HISTORY_MAINTENANCE.md`.
