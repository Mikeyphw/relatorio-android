# Relatório Mensal AD — Android

Aplicativo Android em pt-BR para preencher relatórios mensais, manter rascunhos offline, revisar inconsistências, gerar o PDF oficial e transportar os dados com segurança.

## Estado atual

Esta entrega fecha as lacunas auditadas das Fases 1 a 10, preservando o roadmap funcional e adicionando a regressão final de qualidade.

Dados do projeto:

- modelo padrão: `AD_TIMOTEO_2026_V1`;
- modelo alternativo: `RELATORIO_COMPLETO_V1`;
- package ID: `com.mikeyphw.relatorio.android`;
- target Devtool: `relatorio_android`;
- versão: `1.0.1`.

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
- restauração isolada de configurações mesmo quando não há relatório novo;
- ajuda contextual em todas as telas;
- tela inicial com três ações principais e opções secundárias organizadas;
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
bash tools/run_final_release_gate.sh
```

Com um aparelho de aceitação conectado:

```bash
bash tools/run_final_release_gate.sh --device
```

## Gerar APK release assinado

O guia do usuário está em `docs/MANUAL_RAPIDO_PT_BR.md`.

O projeto não guarda chave ou senha. Defina as variáveis descritas em `docs/RELEASE_CHECKLIST.md` e execute:

```bash
bash tools/build_signed_release.sh
```

A assinatura final depende da chave privada oficial do responsável pelo aplicativo.


A distribuição final exige o preenchimento de `docs/REAL_DEVICE_ACCEPTANCE.md`.
