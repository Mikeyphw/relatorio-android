# Relatório de validação — Fases 9 e 10

## Concluído neste ambiente

- validador estrutural das Fases 9 e 10: aprovado;
- auditoria de preservação das promessas das Fases 2 a 8: aprovada;
- `core-model` compilado com o compilador Kotlin local;
- 25 testes de `core-model` aprovados;
- codec XML de backup executado em teste de ida e volta, incluindo acentos e emoji;
- checksum SHA-256 testado contra alteração do conteúdo;
- DTD e entidades XML maliciosas rejeitadas no teste local;
- planejador de restauração testado com duplicatas, proteção de rascunho, mesmo ID e colisão de movimentações;
- persistência e DAO compilados com contratos Room locais;
- ViewModel compilado com contratos Android/lifecycle locais;
- gerenciador de backup compilado com contratos Android locais;
- adaptador de impressão compilado com contratos Android locais;
- sintaxe das telas Compose verificada pelo compilador Kotlin;
- esquema Room permanece na versão 1;
- manifesto permanece sem internet e sem armazenamento amplo;
- script de assinatura não contém chave ou senha;
- arquivos JSON, XML, TOML, Python e shell verificados;
- inventário e hashes do overlay verificados.

## Dependências de validação no ambiente do usuário

O build Android completo, o lint real, a renderização do PDF, a impressão e o teste em aparelho dependem do Android SDK e do dispositivo do usuário.

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

Antes da distribuição, conclua o teste real descrito em `docs/RELEASE_CHECKLIST.md` e assine o APK com a chave oficial.
