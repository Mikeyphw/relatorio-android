# Relatório Mensal AD — Android

Aplicativo Android em pt-BR para preencher relatórios mensais, manter rascunhos offline e, nas próximas fases, gerar o PDF oficial.

## Estado atual

Esta entrega implementa a **Fase 2 — Fundação Android**.

- modelo padrão: `AD_TIMOTEO_2026_V1`;
- modelo alternativo: `RELATORIO_COMPLETO_V1`;
- package ID: `com.mikeyphw.relatorio.android`;
- target Devtool: `relatorio_android`;
- versão: `0.2.0-phase2`.

## Estrutura

```text
app/          Interface Compose, ViewModel, DataStore e composição do aplicativo
core-model/   Modelos, catálogo dos formulários e validações
persistence/  Banco Room, DAO, mapeadores e repositório
docs/         Escopo e critérios da fase
```

## Compilar

```bash
./gradlew :app:assembleDebug
```

APK esperado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Testar

```bash
./gradlew \
  :core-model:test \
  :persistence:testDebugUnitTest \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

Em um ambiente com dispositivo ou emulador:

```bash
./gradlew \
  :persistence:connectedDebugAndroidTest \
  :app:connectedDebugAndroidTest
```

## Devtool

A configuração inclui o target `relatorio_android`:

```bash
devtool --yes --copy -r "$HOME/Code/relatorio-android" \
  --target relatorio_android validate
```

Consulte `docs/PHASE_2_IMPLEMENTATION.md` para o escopo completo.
