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

## Build e validação via Devtool + GitHub Actions

**Gradle é executado apenas no GitHub Actions.** Os checks locais são Python puro:

```bash
./devtoolw                    # ci-source (padrão)
./devtoolw remote-debug-plan # planejar build debug remoto
./devtoolw remote-debug-build --yes  # após commit/push para master
```

Os testes JVM, lint e compilação do APK rodam no GitHub. A compilação dos
AndroidTest **não significa execução em emulador**. Consulte
`docs/ci/DEVTOOL_ORCHESTRATION.md` para acompanhar requisições, verificar
artefatos e usar refs diferentes de `master`.

## Release assinada

O guia do usuário está em `docs/MANUAL_RAPIDO_PT_BR.md`. A geração local por
`tools/build_signed_release.sh` está bloqueada para evitar Gradle fora do
GitHub. A Wave A **não cria build ou assinatura de release**: isso exige
workflow hospedado revisado em etapa separada, com tratamento seguro das chaves.
