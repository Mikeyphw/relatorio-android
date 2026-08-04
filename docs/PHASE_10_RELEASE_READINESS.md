# Fase 10 — Prontidão final de lançamento

## Entregue

- versão de produto `1.2.0`;
- ação direta de impressão pelo serviço de impressão do Android;
- PDF criado de forma atômica, sem deixar arquivo parcial após falha;
- verificação de espaço livre antes da geração do PDF;
- limpeza limitada de PDFs temporários antigos;
- tela de privacidade e guia rápido em pt-BR;
- tela de abertura nativa no Android 12 ou superior;
- ícone adaptativo e monocromático;
- fluxo de assinatura de APK por variáveis de ambiente, sem guardar segredos no repositório;
- checklist de lançamento e roteiro de teste em aparelho real;
- manual rápido em pt-BR com representações visuais dos botões;
- validador estrutural das Fases 9 e 10.

## Limite de entrega

O repositório não contém chave de assinatura. A assinatura final depende da chave privada do responsável pelo aplicativo. O comando `bash tools/build_signed_release.sh` usa a chave indicada no ambiente e verifica o APK assinado.


O fechamento pós-auditoria está documentado em `PHASE_1_TO_10_GAP_CLOSURE.md`.
