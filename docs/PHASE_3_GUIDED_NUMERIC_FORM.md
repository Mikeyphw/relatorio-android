# Fase 3 — Formulário numérico guiado

## Objetivo

Transformar os 33 campos do formulário padrão e os 74 campos do formulário completo em um fluxo Android simples, dividido por seções, com salvamento automático.

## Entregue

### Preenchimento por seções

- rota própria de preenchimento numérico;
- uma seção por tela;
- indicação clara de seção atual e total;
- botão **Voltar** e botão **Próxima seção** sempre no final da tela;
- na última seção, o botão muda para **Concluir números**;
- retorno ao resumo do rascunho após percorrer todas as seções.

### Controles acessíveis

Cada campo possui:

- nome em pt-BR;
- botão grande para diminuir;
- campo para digitação direta;
- botão grande para aumentar;
- bloqueio de valores negativos;
- limite de seis dígitos;
- valor zero aceito como resposta válida.

### Salvamento automático

- cada alteração é persistida imediatamente no Room;
- o carimbo de atualização do relatório é renovado;
- fechar e abrir o aplicativo preserva os números;
- o progresso de seções é preservado no DataStore;
- o usuário retoma a seção onde parou;
- após concluir todas as seções, **Revisar números** reabre pela primeira seção.

### Modelo completo

As três linhas sem descrição do formulário antigo permanecem disponíveis como:

- Outro tipo de reunião 1;
- Outro tipo de reunião 2;
- Outro tipo de reunião 3.

O usuário pode trocar esses nomes diretamente na tela, e o rótulo personalizado é salvo com o relatório.

## Persistência

Não houve mudança de esquema do banco.

Foram adicionadas operações de atualização para:

- valor numérico;
- rótulo personalizado;
- data de atualização do relatório.

O progresso de navegação usa as chaves:

- ID do rascunho;
- índice da seção atual;
- quantidade de seções já revisadas.

## Fora do escopo desta fase

- movimentação de membros;
- anotações;
- validações cruzadas;
- conclusão definitiva do relatório;
- geração e compartilhamento de PDF.

## Critérios de aceitação

1. O modelo padrão apresenta cinco seções e 33 campos.
2. O modelo completo apresenta dez seções e 74 campos.
3. Mais e menos nunca produzem valor negativo.
4. Digitação direta aceita somente números.
5. Alterações reaparecem após restaurar o rascunho.
6. O progresso retorna à seção correta.
7. As três reuniões personalizadas conservam seus nomes.
8. Concluir a última seção retorna ao resumo com progresso completo.
