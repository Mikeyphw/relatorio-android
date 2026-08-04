# Relatório de validação — Fase 4

## Concluído neste ambiente

- validadores estruturais das Fases 2, 3 e 4: aprovados;
- catálogo do modelo padrão: 33 campos em 5 seções;
- catálogo do modelo completo: 74 campos em 10 seções;
- compilação Kotlin do `core-model`: aprovada;
- compilação Kotlin da persistência com stubs Room: aprovada;
- compilação Kotlin do ViewModel com stubs Android: aprovada;
- parser de datas brasileiras e conversão ISO: verificados;
- validação de nome, motivo e datas: verificada;
- CRUD de movimentações no DAO e repositório: contrato verificado;
- salvamento automático das anotações: contrato verificado;
- separação dos campos por modelo: verificada;
- XML, JSON, TOML e Python: analisados sem erro;
- manifesto Android sem permissão de internet: confirmado;
- inventário do overlay e hashes de origem/destino: verificados.

## Limitação deste ambiente

O Gradle tentou obter a distribuição 9.4.1, mas o ambiente de geração não possui acesso à internet. A compilação Android completa deve ser confirmada pelo Devtool no ambiente do usuário, que já possui o SDK e os caches necessários.

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
  --task :persistence:compileDebugAndroidTestKotlin \
  --task :app:testDebugUnitTest \
  --task :app:lintDebug
```

Os testes instrumentados de persistência exigem dispositivo ou emulador para execução, mas sua compilação está incluída nas tarefas acima.
