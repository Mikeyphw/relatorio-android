# Fase 7 — Acessibilidade e usabilidade

## Objetivo

Deixar o aplicativo confortável para uma pessoa de aproximadamente 60 anos, sem esconder ações importantes e sem exigir familiaridade com sistemas administrativos.

## Implementado

- tela **Leitura e ajuda** acessível diretamente pela tela inicial;
- escolha persistente entre letras **Grandes** e **Muito grandes**;
- contraste reforçado opcional, com esquemas claro e escuro próprios;
- instruções em cinco passos, em pt-BR;
- botões que crescem verticalmente em vez de cortar texto ampliado;
- títulos com semântica de cabeçalho;
- ações com texto visível, sem depender somente de ícones;
- áreas de toque mínimas mantidas em todo o fluxo;
- suporte adicional ao tamanho de fonte configurado no Android.

## Persistência

As preferências de leitura ficam no DataStore e entram no estado principal do aplicativo. Nenhuma migração do banco Room é necessária.

## Critérios de aceite

- alterar o tamanho das letras muda imediatamente toda a interface;
- reiniciar o aplicativo preserva a escolha;
- ativar contraste reforçado não remove informações;
- textos ampliados não ficam presos em botões de altura fixa;
- a tela explica o fluxo principal sem linguagem técnica.
