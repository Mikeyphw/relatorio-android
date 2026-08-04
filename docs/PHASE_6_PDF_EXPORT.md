# Fase 6 — PDF oficial, salvamento e compartilhamento

## Objetivo

Transformar qualquer relatório concluído em um documento PDF A4 legível, fiel ao modelo selecionado e fácil de visualizar, salvar ou compartilhar no Android.

## Entregas

- conteúdo de impressão puro no `core-model`;
- renderizador Android baseado em `PdfDocument`;
- layout próprio para o modelo AD Timóteo 2026;
- layout próprio para o Relatório Completo;
- cabeçalho institucional, identificação, seções numéricas, movimentações, legenda e anotações;
- continuação automática quando movimentações ou anotações excedem uma página;
- nomes de arquivo previsíveis e seguros;
- tela de detalhes para relatórios concluídos;
- visualização por leitor de PDF instalado;
- salvamento pelo seletor de documentos do Android;
- compartilhamento seguro por `content://` usando `FileProvider`;
- geração totalmente offline;
- carregamento completo de relatórios concluídos pelo repositório.

## Fluxo

1. O usuário conclui o relatório.
2. O aplicativo abre diretamente a tela **PDF do relatório**.
3. O usuário escolhe:
   - **Visualizar PDF**;
   - **Salvar uma cópia**;
   - **Compartilhar PDF**.
4. Relatórios antigos podem ser reabertos em **Relatórios prontos**.

## Modelo padrão

O modelo `AD_TIMOTEO_2026_V1` usa:

- página A4 em orientação retrato;
- duas colunas para as seções numéricas;
- Escola Bíblica Dominical e Cultos separados visualmente;
- tabela de movimentações com data de nascimento;
- legenda oficial dos códigos;
- anotações no rodapé;
- páginas de continuação quando necessário.

## Modelo completo

O modelo `RELATORIO_COMPLETO_V1` mantém as 74 quantidades e:

- distribui as seções em duas colunas;
- preserva rótulos personalizados das reuniões;
- usa tabela de movimentações sem data de nascimento;
- move movimentações e anotações para páginas próprias para evitar cortes.

## Segurança de arquivos

- PDFs temporários ficam somente em `cache/pdf_exports`;
- o `FileProvider` expõe apenas essa pasta estreita;
- os intents recebem permissão temporária de leitura;
- salvar uma cópia usa o Storage Access Framework;
- nenhuma permissão ampla de armazenamento é solicitada;
- nenhuma permissão de internet é adicionada.

## Persistência

A Fase 6 adiciona uma consulta transacional para carregar um relatório completo por ID. O esquema Room permanece na versão 1, portanto nenhuma migração é necessária.

## Critérios de aceite

- os dois modelos produzem conteúdo PDF com todos os campos;
- o modelo padrão possui 33 linhas numéricas e seis blocos visuais;
- o modelo completo possui 74 linhas numéricas;
- movimentações, códigos, ocorrência e datas aparecem no documento correto;
- caracteres acentuados são preservados;
- o arquivo começa com a assinatura `%PDF`;
- visualização usa URI `content://`;
- salvamento não exige permissão de armazenamento;
- compartilhamento concede apenas acesso temporário ao arquivo;
- lint continua configurado com avisos como erros.
