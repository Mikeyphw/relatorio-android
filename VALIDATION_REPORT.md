# Relatório de validação — fechamento das Fases 1 a 10

## Concluído nesta entrega

- todos os validadores históricos das Fases 2 a 10 passam sobre a versão final 1.0.1;
- 33 campos do modelo padrão e 74 do modelo completo preservados;
- restauração somente de preferências implementada e coberta por teste;
- 26 testes de `core-model` executados localmente e aprovados;
- fontes principais de `core-model`, persistência e ViewModel compiladas com contratos locais;
- ajuda contextual adicionada a todas as rotas;
- tela inicial restaurada para três ações principais;
- APIs Compose depreciadas removidas do código e dos testes;
- regressão instrumental ponta a ponta adicionada;
- esquema Room permanece na versão 1;
- manifesto permanece sem internet e sem armazenamento amplo;
- inventário e hashes do overlay verificados.

## Validação Android anterior confirmada pelo usuário

A base auditada havia passado com 21 testes, 0 falhas e 0 erros. Os seis avisos relatados nessa execução são removidos por esta entrega.

## Validação que deve ser executada após aplicar

```bash
devtool --yes --copy \
  -r "$HOME/Code/relatorio-android" \
  --target relatorio_android \
  validate \
  --task :app:compileDebugKotlin \
  --task :core-model:test \
  --task :persistence:testDebugUnitTest \
  --task :persistence:compileDebugAndroidTestKotlin \
  --task :app:testDebugUnitTest \
  --task :app:compileDebugAndroidTestKotlin \
  --task :app:lintDebug
```

A execução local do Gradle não foi possível neste ambiente porque o wrapper tentou baixar a distribuição e a rede está desabilitada.

## Gates humanos ainda obrigatórios

- executar `:app:connectedDebugAndroidTest` em aparelho;
- preencher `docs/REAL_DEVICE_ACCEPTANCE.md`;
- realizar o teste com o usuário principal;
- gerar e verificar o APK assinado com a chave oficial.
