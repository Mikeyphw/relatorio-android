# Relatório de validação — Fase 6

## Concluído neste ambiente

- validadores estruturais das Fases 2, 3, 4, 5 e 6: aprovados;
- catálogo do modelo padrão: 33 campos convertidos em seis blocos de PDF;
- catálogo do modelo completo: 74 campos preservados;
- compilação Kotlin do `core-model`: aprovada;
- fábrica pura de conteúdo PDF: compilada e exercitada;
- nomes de arquivo sem caracteres inseguros: verificados;
- datas ISO convertidas para `DD/MM/AAAA`: verificadas;
- consulta Room de relatório completo por ID: contrato verificado;
- rota de detalhes e ações de PDF: verificadas;
- `PdfDocument`, `CreateDocument` e `FileProvider`: integrados;
- `FileProvider` limitado a `cache/pdf_exports`;
- manifesto Android sem permissão de internet ou armazenamento: confirmado;
- esquema Room mantido na versão 1;
- XML, JSON, TOML e Python: analisados sem erro;
- inventário do overlay e hashes de origem/destino: verificados.

## Validação Android necessária no ambiente do usuário

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
  --task :app:compileDebugAndroidTestKotlin \
  --task :app:lintDebug
```

O teste instrumental de exportação confirma em dispositivo ou emulador que o arquivo gerado possui assinatura `%PDF` e URI `content://`.
