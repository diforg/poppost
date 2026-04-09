# PopPost

Aplicativo Android em Kotlin com Jetpack Compose, foco em publicações curtas e gerenciamento local de posts (ativos/arquivados), sem backend.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM
- Room (SQLite local)
- Navigation Compose
- StateFlow + ViewModel
- Gradle Kotlin DSL
- Docker (build Android reprodutível)

## Estrutura de pacotes

```text
com.poppost/
├── data/
│   ├── local/
│   └── repository/
├── domain/
│   └── model/
├── ui/
│   ├── main/
│   ├── create/
│   ├── archived/
│   └── components/
├── viewmodel/
└── navigation/
```

## Fluxo de branch recomendado (pre-MVP e pos-MVP)

Para manter historico limpo e seguro, use:

1. `develop` para integração de features.
2. branches de feature curtas (`feature/...`) saindo de `develop`.
3. PR de cada feature para `develop`.
4. `main` como branch estavel/publicavel (somente merge de release aprovada).

### Sugestão prática para este momento

- Criar uma branch de documentação (ex.: `docs/readme-mvp`) saindo de `develop`.
- Fazer PR da documentação para `develop`.
- Quando o MVP estiver aprovado, abrir PR `develop -> main`.

## Requisitos locais

- Android Studio recente
- JDK 17
- Android SDK API 34
- `adb` no PATH (para instalar APK no celular via terminal)

Se for compilar fora do container, mantenha `local.properties` com caminho válido do SDK Android.

## Como rodar no Android Studio

1. Abra a pasta do projeto.
2. Aguarde sincronização do Gradle.
3. Execute a configuração `app` em emulador/dispositivo.

## Build local via Gradle

```bash
cd /home/makon/Documents/repo/github/poppost
./gradlew :app:assembleDebug
```

APK gerado em:

- `app/build/outputs/apk/debug/app-debug.apk`

## Build via Docker (recomendado para ambiente padronizado)

```bash
cd /home/makon/Documents/repo/github/poppost
docker compose build android-build
docker compose run --rm android-build gradle :app:assembleDebug
```

APK gerado em:

- `app/build/outputs/apk/debug/app-debug.apk`

## Instalar APK no celular (teste real)

Pre-requisitos no Android:

- Opções de desenvolvedor ativadas
- Depuração USB ativada

Comandos:

```bash
cd /home/makon/Documents/repo/github/poppost
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Se aparecer erro de assinatura por versão anterior instalada, desinstale e reinstale:

```bash
adb uninstall com.poppost
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Publicar na Google Play

Resumo rápido (pipeline recomendado):

1. Definir versão de release em `app/build.gradle.kts`:
   - aumentar `versionCode`
   - atualizar `versionName`
2. Gerar Android App Bundle (`.aab`):

```bash
cd /home/makon/Documents/repo/github/poppost
./gradlew :app:bundleRelease
```

3. Assinar release (keystore propria) e configurar assinatura no Gradle.
4. Criar app no Google Play Console.
5. Preencher ficha da loja (descrição, categoria, política de privacidade, classificação etária).
6. Subir o `.aab` em uma faixa (interna/fechada/produção).
7. Publicar release e acompanhar revisão da Google.

> Detalhamento completo em `docs/google-play-release.md`.

## Qualidade e validação

Rodar testes unitários:

```bash
cd /home/makon/Documents/repo/github/poppost
./gradlew :app:testDebugUnitTest
```

Ou no Docker:

```bash
cd /home/makon/Documents/repo/github/poppost
docker compose run --rm android-build gradle :app:testDebugUnitTest
```
