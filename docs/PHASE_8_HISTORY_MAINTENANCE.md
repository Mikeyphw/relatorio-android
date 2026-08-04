# Fase 8 — Histórico e manutenção segura

## Objetivo

Permitir encontrar, reutilizar, corrigir e excluir relatórios concluídos sem manipular arquivos ou tabelas manualmente.

## Implementado

- pesquisa por congregação, mês, ano ou nome do formulário;
- contagem de resultados;
- abertura de qualquer relatório concluído;
- opção **Usar como base para o próximo mês**;
- avanço automático de dezembro para janeiro do ano seguinte;
- cópia das quantidades e rótulos personalizados;
- movimentações e anotações iniciadas vazias no novo mês;
- opção **Corrigir este relatório**, retornando-o ao estado de rascunho;
- substituição do rascunho atual somente após confirmação visível;
- opção de exclusão com confirmação e restrição a relatórios concluídos;
- transações Room para evitar estados intermediários;
- esquema Room preservado na versão 1.

## Segurança das ações

- reutilizar e reabrir avisam quando existe outro rascunho;
- reabrir mantém todos os dados e remove temporariamente o relatório da lista de prontos;
- excluir é uma ação separada e explicitamente irreversível;
- falhas transacionais não devem apagar o rascunho anterior pela metade.

## Critérios de aceite

- uma busca por `agosto`, `2026`, congregação ou modelo encontra o item correto;
- duplicar dezembro cria janeiro do ano seguinte;
- a cópia mantém quantidades, mas não reaproveita eventos mensais;
- reabrir permite editar e concluir novamente;
- excluir remove valores e movimentações pela cascata Room.
