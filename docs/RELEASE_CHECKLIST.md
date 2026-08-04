# Checklist de lançamento 1.0.0

## Validação automatizada

- [ ] `:app:compileDebugKotlin`
- [ ] `:core-model:test`
- [ ] `:persistence:testDebugUnitTest`
- [ ] `:persistence:compileDebugAndroidTestKotlin`
- [ ] `:app:testDebugUnitTest`
- [ ] `:app:compileDebugAndroidTestKotlin`
- [ ] `:app:lintDebug`
- [ ] `:app:assembleRelease`

## Teste em aparelho real

- [ ] criar e retomar um rascunho após encerrar o processo;
- [ ] preencher os dois modelos com fonte do Android em 200%;
- [ ] navegar com TalkBack;
- [ ] cadastrar nomes longos, acentos, datas e muitas movimentações;
- [ ] gerar PDFs dos dois modelos e conferir todas as páginas;
- [ ] salvar, visualizar, imprimir e compartilhar um PDF;
- [ ] criar backup XML e pacote ZIP com PDFs;
- [ ] restaurar em instalação limpa;
- [ ] confirmar que duplicados são ignorados;
- [ ] testar restauração com e sem substituição do rascunho;
- [ ] testar arquivo corrompido e arquivo XML com DTD;
- [ ] testar geração de PDF com pouco espaço livre;
- [ ] testar telas pequena, grande, retrato e paisagem;
- [ ] confirmar que o app funciona sem internet.

## Assinatura

Use `bash tools/build_signed_release.sh` somente com a chave oficial do projeto. Não inclua a chave, senhas ou APKs assinados no Git.
