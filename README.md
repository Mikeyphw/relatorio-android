# Relatório Mensal AD — Android

Aplicativo Android em pt-BR para preencher relatórios mensais, manter rascunhos offline, revisar inconsistências e gerar o PDF oficial.

## Estado atual

Esta entrega implementa a **Fase 6 — PDF oficial, salvamento e compartilhamento**.

- modelo padrão: `AD_TIMOTEO_2026_V1`;
- modelo alternativo: `RELATORIO_COMPLETO_V1`;
- package ID: `com.mikeyphw.relatorio.android`;
- target Devtool: `relatorio_android`;
- versão: `0.6.0-phase6`.

## O que já funciona

- criação e restauração de rascunho;
- identificação com dados reutilizados;
- seleção entre os dois modelos;
- preenchimento guiado dos 33 ou 74 campos;
- botões grandes de menos e mais;
- digitação direta dos números;
- salvamento automático no Room;
- retomada da seção atual pelo DataStore;
- nomes personalizados para reuniões sem rótulo do modelo completo;
- cadastro, edição e exclusão de movimentações;
- motivo por extenso com código automático;
- datas em formato brasileiro com armazenamento ISO;
- anotações com salvamento automático;
- revisão final com erros bloqueantes e avisos não bloqueantes;
- conferência de EBD, Santa Ceia, cartas e datas de movimentação;
- confirmação antes de concluir;
- histórico de relatórios concluídos;
- PDF A4 próprio para cada modelo;
- visualização do PDF em aplicativo instalado;
- salvamento pelo seletor de documentos do Android;
- compartilhamento seguro por `content://`;
- funcionamento sem internet.

## Estrutura

```text
app/          Interface Compose, ViewModel, DataStore, PDF Android e composição do aplicativo
core-model/   Modelos, catálogos, datas, revisão e conteúdo de impressão
persistence/  Banco Room, DAO, mapeadores e repositório
docs/         Escopo e critérios das fases
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
  :app:assembleDebug \
  :app:lintDebug
```

Em um ambiente com dispositivo ou emulador:

```bash
./gradlew \
  :persistence:connectedDebugAndroidTest \
  :app:connectedDebugAndroidTest
```

## Devtool

```bash
devtool --yes --copy -r "$HOME/Code/relatorio-android" \
  --target relatorio_android validate
```

Consulte `docs/PHASE_6_PDF_EXPORT.md` para o escopo completo.
