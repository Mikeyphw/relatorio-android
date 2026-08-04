# Relatório Mensal AD — Android

Aplicativo Android em pt-BR para preencher relatórios mensais, manter rascunhos offline, revisar inconsistências, gerar o PDF oficial e transportar os dados com segurança.

## Estado atual

Esta entrega conclui o roadmap funcional com:

- **Fase 9 — Backup e portabilidade dos dados**;
- **Fase 10 — Prontidão final de lançamento**.

Dados do projeto:

- modelo padrão: `AD_TIMOTEO_2026_V1`;
- modelo alternativo: `RELATORIO_COMPLETO_V1`;
- package ID: `com.mikeyphw.relatorio.android`;
- target Devtool: `relatorio_android`;
- versão: `1.0.0`.

## O que funciona

- criação, salvamento e retomada de rascunho;
- dois modelos de formulário, com 33 ou 74 campos;
- preenchimento guiado com botões grandes;
- movimentações de membros e anotações;
- revisão com erros bloqueantes e avisos;
- conclusão, pesquisa e manutenção do histórico;
- reutilização para o mês seguinte e reabertura para correção;
- PDF A4, visualização, salvamento, impressão e compartilhamento;
- letras grandes ou muito grandes e contraste reforçado;
- backup XML restaurável com verificação SHA-256 de integridade;
- pacote ZIP com backup e PDFs concluídos;
- prévia e proteção contra duplicatas antes da restauração;
- tela de privacidade e guia rápido;
- funcionamento offline, sem permissões de internet ou armazenamento amplo.

## Estrutura

```text
app/          Interface Compose, ViewModel, DataStore, PDF, impressão e backup Android
core-model/   Modelos, catálogos, revisão, histórico, PDF e formato de backup
persistence/  Banco Room, DAO, mapeadores e restauração transacional
docs/         Escopo, privacidade, checklist e critérios de lançamento
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

## Gerar APK release assinado

O guia do usuário está em `docs/MANUAL_RAPIDO_PT_BR.md`.

O projeto não guarda chave ou senha. Defina as variáveis descritas em `docs/RELEASE_CHECKLIST.md` e execute:

```bash
bash tools/build_signed_release.sh
```

A assinatura final depende da chave privada oficial do responsável pelo aplicativo.
