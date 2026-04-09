# Publicacao na Google Play (PopPost)

Este documento descreve um fluxo pratico para publicar o PopPost na Google Play usando Android App Bundle (`.aab`).

## 1. Pre-requisitos

- Conta ativa no Google Play Console
- App com `applicationId` definitivo (atual: `com.poppost`)
- Keystore de assinatura de release
- Politica de privacidade publicada em URL publica

## 2. Versao do app

Atualize em `app/build.gradle.kts`:

- `versionCode`: inteiro sempre crescente
- `versionName`: string legivel para usuario

Exemplo:

```kotlin
defaultConfig {
    versionCode = 2
    versionName = "1.1.0"
}
```

## 3. Assinatura de release

Se ainda nao existir keystore, gere uma:

```bash
keytool -genkeypair -v \
  -keystore poppost-release.keystore \
  -alias poppost \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Recomendacao de seguranca:

- Nao commitar keystore nem senhas no repositorio
- Guardar segredos em cofre seguro/CI secrets

## 4. Gerar o AAB

```bash
cd /home/makon/Documents/repo/github/poppost
./gradlew :app:bundleRelease
```

Saida esperada:

- `app/build/outputs/bundle/release/app-release.aab`

## 5. Publicar no Play Console

1. Criar aplicativo no Play Console
2. Preencher cadastro da loja (nome, descricao curta/completa, icones, screenshots)
3. Configurar classificacao indicativa
4. Preencher politicas obrigatorias (seguranca de dados, publico-alvo, etc.)
5. Criar release na faixa desejada:
   - Interna (recomendado primeiro)
   - Fechada
   - Producao
6. Enviar `app-release.aab`
7. Revisar e publicar

## 6. Checklist de release

- [ ] `versionCode` incrementado
- [ ] `versionName` atualizado
- [ ] `./gradlew :app:testDebugUnitTest` passando
- [ ] Build release gerado sem erro
- [ ] Politica de privacidade atualizada
- [ ] Notas de release preparadas

## 7. Fluxo de branch sugerido para release

1. Merge das features em `develop`
2. Teste integrado do MVP em `develop`
3. PR `develop -> main`
4. Tag de release no commit de `main` (ex.: `v1.0.0`)
5. Build assinado e envio para Play Console

