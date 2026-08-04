# Relatório de validação — Fase 5

## Concluído neste ambiente

- validadores estruturais das Fases 2, 3, 4 e 5: aprovados;
- catálogo do modelo padrão: 33 campos em 5 seções;
- catálogo do modelo completo: 74 campos em 10 seções;
- compilação Kotlin do `core-model`: aprovada;
- verificações em execução do motor de revisão: aprovadas;
- regras de bloqueio e aviso: verificadas;
- conferência de EBD e Santa Ceia: verificada;
- conferência de cartas e datas de movimentação: verificada;
- rota de revisão e confirmação de conclusão: verificadas;
- mudança de `DRAFT` para `COMPLETED`: contrato verificado;
- limpeza do progresso no DataStore: contrato verificado;
- esquema Room mantido na versão 1;
- manifesto Android sem permissão de internet: confirmado;
- XML, JSON, TOML e Python: analisados sem erro;
- inventário do overlay e hashes de origem/destino: verificados.

## Limitação deste ambiente

O wrapper tentou obter a distribuição Gradle 9.4.1, mas o ambiente de geração não possui acesso à internet. A compilação Android completa deve ser confirmada pelo Devtool no ambiente do usuário, que já possui SDK e caches necessários.

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
