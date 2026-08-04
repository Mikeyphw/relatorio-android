# Fase 5 — Revisão final e conclusão

## Objetivo

Adicionar uma etapa segura de conferência antes de retirar o relatório dos rascunhos. O usuário continua podendo usar zero em qualquer campo numérico, mas precisa percorrer todas as seções e corrigir problemas estruturais antes da conclusão.

## Entregas

- Tela **Revisão final** em pt-BR.
- Resumo de identificação, seções numéricas, movimentações e anotações.
- Distinção visual entre problemas bloqueantes e avisos.
- Confirmação explícita antes de concluir.
- Mudança do status Room de `DRAFT` para `COMPLETED`.
- Remoção do relatório concluído da área de rascunhos.
- Exibição imediata em **Relatórios prontos**.
- Limpeza do progresso de rascunho no DataStore.

## Problemas bloqueantes

- identificação obrigatória inválida;
- seções numéricas ainda não revisadas;
- campos do modelo ausentes ou duplicados;
- quantidade negativa encontrada na integridade dos dados;
- movimentação sem nome;
- código de motivo inválido;
- data interna inválida.

O botão **Concluir relatório** permanece desativado enquanto houver um item bloqueante.

## Avisos não bloqueantes

- total da EBD diferente da soma das faixas etárias;
- membros diferentes da soma de participantes e não participantes da Santa Ceia;
- quantidade de cartas recebidas ou expedidas diferente das movimentações correspondentes no modelo padrão;
- data de movimentação fora do mês do relatório;
- nascimento posterior à ocorrência;
- outro relatório concluído para o mesmo modelo, mês e congregação.

Os avisos são deliberadamente não bloqueantes. O formulário de origem pode conter situações legítimas que não seguem uma igualdade matemática perfeita.

## Arquitetura

A validação principal vive em `core-model/ReportReview.kt`, sem dependência Android. A camada de apresentação acrescenta apenas regras que dependem do estado local, como progresso de navegação e duplicidade contra relatórios já concluídos.

## Persistência

A fase reutiliza o esquema Room versão 1. Nenhuma migração é necessária. A conclusão usa o campo de status que já existia no modelo e no banco.

## Fora desta fase

- geração de PDF;
- visualização de relatório concluído;
- reabertura ou duplicação de relatório pronto;
- compartilhamento e impressão.
