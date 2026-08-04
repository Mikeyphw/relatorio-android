# Fase 4 — Movimentações e anotações

## Objetivo

Permitir que o usuário registre movimentações de membros e anotações mensais sem utilizar uma tabela apertada, preservando tudo no rascunho offline.

## Entregue

### Lista de movimentações

- acesso direto pelo resumo do rascunho;
- estado vazio explicado em linguagem simples;
- cartões grandes com nome, registro, data, motivo e código;
- exibição de ocorrência e data de nascimento no modelo padrão;
- quantidade de pessoas exibida no resumo;
- edição de qualquer movimentação;
- exclusão somente após confirmação.

### Cadastro guiado

Campos comuns aos dois modelos:

- número de registro;
- nome da pessoa;
- data da ocorrência;
- motivo por extenso;
- código atribuído automaticamente.

Campos adicionais do modelo padrão:

- ocorrência;
- data de nascimento.

O modelo completo mantém apenas os campos existentes em sua estrutura anterior.

### Datas

- digitação em `DD/MM/AAAA`;
- barras inseridas automaticamente;
- validação de datas reais, incluindo dia, mês e ano;
- armazenamento interno em ISO `AAAA-MM-DD`;
- campos de data opcionais podem permanecer vazios.

### Motivos oficiais

- FCO — Fora de comunhão;
- RCO — Recebidos em comunhão;
- EXC — Desligados;
- CME — Carta de mudança expedida;
- CMR — Carta de mudança recebida;
- BNA — Batismo nas águas;
- FAL — Falecimento.

O usuário escolhe o motivo por extenso. A sigla não precisa ser memorizada.

### Anotações e sugestões

- campo grande de várias linhas;
- limite de 4.000 caracteres;
- salvamento automático com pequena espera para evitar escrita a cada toque;
- salvamento imediato ao sair da tela;
- indicador visível de “Salvando...” ou “Salvo automaticamente”.

## Persistência

Não houve alteração no esquema Room. As tabelas de movimentações e o campo de anotações já existiam desde a fundação.

Foram adicionadas operações para:

- inserir ou substituir uma movimentação;
- excluir uma movimentação ligada ao rascunho;
- atualizar as anotações;
- renovar a data de atualização do relatório em cada mudança.

## Fora do escopo desta fase

- validações cruzadas entre totais;
- tela de revisão final;
- conclusão definitiva do relatório;
- geração, visualização e compartilhamento de PDF.

## Critérios de aceitação

1. O usuário adiciona uma movimentação sem digitar a sigla.
2. Nome ausente impede o salvamento com mensagem em pt-BR.
3. Data inválida é explicada sem termos técnicos.
4. Editar conserva o mesmo identificador da movimentação.
5. Excluir exige confirmação.
6. Fechar e reabrir o app conserva movimentações e anotações.
7. O modelo padrão mostra ocorrência e nascimento.
8. O modelo completo não mostra campos que não existem nele.
9. Anotações são salvas automaticamente.
10. Nenhuma permissão de internet é adicionada.
