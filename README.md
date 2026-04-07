# PopPost

Aplicativo Android em Kotlin com Jetpack Compose, construído incrementalmente.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM
- Room
- Navigation Compose
- StateFlow + ViewModel
- Gradle Kotlin DSL

## Estrutura inicial

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

## Requisitos locais

- Android Studio recente com suporte a Kotlin DSL
- JDK 17
- Android SDK com API 34

Se estiver compilando localmente fora do container, gere ou mantenha um `local.properties` apontando para o SDK Android.

## Executar no Android Studio

1. Abra a pasta do projeto no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Execute a configuração `app` em um emulador ou dispositivo.

## Compilar em container

O projeto inclui um ambiente de build com Gradle + JDK 17 + Android SDK.

```bash
docker compose build android-build
docker compose run --rm android-build gradle assembleDebug
```

O APK de debug será gerado em `app/build/outputs/apk/debug/`.

## Próximas etapas

- Feature 2: camada de dados com Room
- Feature 3: ViewModel e fluxos de estado
- Feature 4 em diante: telas, navegação e polimento visual