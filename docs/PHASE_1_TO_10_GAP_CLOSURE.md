# Fechamento integral das Fases 1 a 10

## Objetivo

Fechar as lacunas encontradas na auditoria final sem alterar os dois modelos de relatório, o esquema Room ou o funcionamento offline.

## Correções técnicas

- restauração de configurações permitida mesmo quando todos os relatórios do backup já existem;
- prévia de backup informa claramente que identificação, formulário, leitura e contraste serão restaurados;
- validadores históricos das Fases 4 a 8 aceitam a linha final 1.x;
- cadeia completa de validadores das Fases 2 a 10 harmonizada;
- ícones direcionais migrados para `Icons.AutoMirrored`;
- testes Compose migrados para a API JUnit4 v2;
- regressão instrumental ponta a ponta adicionada;
- isolamento dos cenários de interface com limpeza controlada de Room e DataStore;
- ajuda contextual disponível na barra superior de todas as telas;
- tela inicial reduzida novamente às três ações principais;
- leitura, backup e privacidade reunidos no botão explícito **Mais opções**.

## Versão

- `versionCode`: 11
- `versionName`: `1.0.1`
- banco Room: versão 1, sem migração

## Gates externos

Os testes físicos de TalkBack, fonte do Android em 200%, impressão real, armazenamento insuficiente, usabilidade com o usuário principal e assinatura com a chave privada oficial continuam sendo gates humanos. Eles são executáveis pelo roteiro em `docs/REAL_DEVICE_ACCEPTANCE.md` e não são declarados como concluídos sem evidência real.
