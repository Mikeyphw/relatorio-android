# Fase 2 — Fundação Android

## Base utilizada

O projeto foi reconstruído como um aplicativo Android independente usando a base técnica do XDM Android fornecido:

- Gradle Wrapper 9.4.1;
- AGP 9.2.1;
- Kotlin 2.3.20;
- compileSdk e targetSdk 36;
- minSdk 26;
- Jetpack Compose e Material 3;
- arquitetura modular semelhante, com `app`, `core-model` e `persistence`;
- Room, DataStore, StateFlow e ViewModel;
- edge-to-edge e controles com áreas de toque grandes.

Nenhum recurso de download, navegador, Termux ou diagnóstico do XDM foi transportado. A base foi usada como fundação de build, arquitetura e qualidade de interface.

## Entregue nesta fase

### Modelo de domínio

- IDs estáveis dos dois modelos;
- modelo AD Timóteo como padrão;
- modelo completo como alternativa;
- catálogo de 33 e 74 campos;
- identidade do relatório;
- valores, movimentações, estado e resumo;
- códigos oficiais de movimentação;
- criação de rascunho com todos os valores em zero;
- validação básica em pt-BR.

### Persistência

Banco Room versão 1 com:

- `reports`;
- `report_values`;
- `member_movements`.

O repositório permite:

- criar ou substituir o único rascunho ativo;
- restaurar o rascunho após fechar o aplicativo;
- editar a identificação sem apagar os valores preparados;
- observar relatórios concluídos;
- concluir ou excluir um relatório.

DataStore conserva:

- modelo usado por padrão;
- regional;
- congregação;
- pastor dirigente;
- ministério;
- pastor presidente.

### Interface real

- tela inicial com três ações grandes;
- confirmação antes de iniciar outro relatório quando já existe rascunho;
- seleção dos dois modelos;
- formulário atual marcado como padrão;
- tela de identificação;
- resumo do rascunho salvo;
- tela de relatórios prontos;
- suporte ao botão Voltar do Android;
- tema claro e escuro conforme o aparelho;
- tipografia ampliada;
- textos integralmente em pt-BR;
- nenhuma permissão de internet.

## Fora do escopo desta fase

- preenchimento dos 33 ou 74 contadores;
- movimentação de membros na interface;
- revisão completa;
- geração e compartilhamento de PDF;
- backup e restauração externos.

Esses itens permanecem para as fases seguintes.

## Critérios de aceitação

1. O aplicativo abre na tela inicial.
2. O modelo AD Timóteo aparece selecionado por padrão.
3. O modelo completo pode ser selecionado.
4. A identificação rejeita congregação vazia.
5. Um rascunho salvo reaparece na tela inicial.
6. Fechar e abrir o aplicativo não apaga o rascunho.
7. Editar a identificação não recria os valores.
8. O banco prepara 33 valores no modelo padrão e 74 no completo.
