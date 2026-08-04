# Fase 9 — Backup e portabilidade dos dados

## Entregue

- backup restaurável em XML, criado pelo seletor de documentos do Android;
- pacote ZIP opcional com o backup e um PDF de cada relatório concluído;
- inclusão de relatórios prontos, rascunho, valores, movimentações, anotações e preferências;
- leitura segura de XML, com DTD e entidades externas rejeitadas;
- checksum SHA-256 interno para detectar arquivo alterado ou corrompido;
- UTF-8 estrito, sem aceitar sequências de bytes malformadas;
- limite de tamanho e quantidade para impedir arquivos abusivos;
- prévia antes da restauração;
- relatórios repetidos ignorados por ID e por mês/formulário/congregação;
- proteção do rascunho atual, substituído somente após escolha explícita;
- substituição correta mesmo quando o rascunho restaurado mantém o mesmo identificador;
- restauração transacional no Room;
- suporte a backup XML isolado e ao pacote ZIP completo.

## Política de restauração

Relatórios concluídos equivalentes não são duplicados. Um rascunho encontrado no backup é ignorado enquanto já houver outro no aparelho, salvo quando o usuário marca **Substituir o relatório em andamento atual**.

Nenhum dado é alterado antes da tela de conferência e da confirmação final.
