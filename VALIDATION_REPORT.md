# Relatório de validação — Fases 7 e 8

## Concluído neste ambiente

- validadores estruturais das Fases 2 a 8: aprovados;
- `core-model` compilado com o compilador Kotlin local;
- fábrica de reutilização e filtro de histórico compilados;
- persistência, DAO e repositório compilados com contratos Room;
- ViewModel compilado com contratos Android/lifecycle;
- duplicação, reabertura e exclusão cobertas por testes instrumentais;
- avanço dezembro → janeiro coberto por teste unitário;
- preferências de tamanho e contraste ligadas ao DataStore;
- botões de ação convertidos para altura mínima adaptável;
- esquema Room confirmado na versão 1;
- manifesto permanece sem internet e sem armazenamento amplo;
- inventário, hashes e integridade do overlay verificados;
- aplicação transacional testada com o Devtool fornecido.

## Validação Android no ambiente do usuário

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

O build Android completo depende do SDK e das dependências já disponíveis no ambiente de validação do usuário.
