# Relatório de validação — Fase 3

## Concluído neste ambiente

- validador estrutural da Fase 3: aprovado;
- catálogo do modelo padrão: 33 campos em 5 seções;
- catálogo do modelo completo: 74 campos em 10 seções;
- atualização numérica com limite inferior zero: verificada;
- persistência imediata de valores: contrato verificado;
- persistência de rótulos personalizados: contrato verificado;
- progresso do rascunho no DataStore: contrato verificado;
- navegação para frente, para trás e conclusão numérica: verificada;
- XML, JSON, TOML e Python: analisados sem erro;
- manifesto Android sem permissão de internet: confirmado;
- inventário do overlay e hashes de origem/destino: verificados.

## Validação no ambiente Android do usuário

Executar:

```bash
devtool --yes --copy \
  -r "$HOME/Code/relatorio-android" \
  --target relatorio_android \
  validate \
  --task :app:compileDebugKotlin \
  --task :core-model:test \
  --task :persistence:testDebugUnitTest \
  --task :app:testDebugUnitTest \
  --task :app:lintDebug
```

Os testes instrumentados de persistência exigem dispositivo ou emulador e permanecem disponíveis em `persistence/src/androidTest`.
