Velodrome

Velodrome es una aplicación de streaming de música diseñada para servidores Navidrome. Creada con las últimas tecnologías de Android, prioriza la fluidez, la experiencia offline y el rendimiento bajo protocolos de red inestables.
🚀 Características principales

    Offline-First: Gestión inteligente de caché (imágenes y audio) para una reproducción sin interrupciones incluso sin conexión.

    Arquitectura Robusta: Implementación de Clean Architecture con MVVM y Unidirectional Data Flow (UDF).

    Reproductor Profesional: Basado en Jetpack Media3 (ExoPlayer) con soporte nativo para caching y sesiones multimedia.

    Sincronización Inteligente: Uso de WorkManager para procesos de fondo (scrobbling y sincronización de biblioteca) resilientes.

    UI Moderna: Interfaz desarrollada 100% en Jetpack Compose con un diseño altamente personalizable y rendimiento optimizado.

    Seguridad: Almacenamiento de credenciales mediante EncryptedSharedPreferences y Tink.

🛠 Tech Stack

    Lenguaje: Kotlin

    UI: Jetpack Compose, Material3

    Arquitectura: MVVM, Clean Architecture, Dependency Injection (Hilt)

    Red: Retrofit + OkHttp (con intercepción dinámica de autenticación)

    Persistencia: Room (Offline-First) + DataStore (Preferences)

    Multimedia: Android Media3 (ExoPlayer) + Coil 3

    Background Tasks: WorkManager

    Navegación: Type-safe Navigation (kotlinx.serialization)

🏗 Arquitectura de alto nivel

La aplicación está estructurada para garantizar la mantenibilidad:

    Domain: Define las entidades y los casos de uso (UseCases), manteniendo la lógica de negocio independiente de los frameworks.

    Data: Repositorios que actúan como Single Source of Truth entre la API (Remota) y la Base de Datos (Local).

    Presentation: Interfaces reactivas (Flows) y Viewmodels que exponen estados inmutables a la UI.

🔐 Seguridad

    Zero-exposure policy: Las credenciales del servidor Navidrome se almacenan mediante cifrado de nivel industrial (Tink/EncryptedSharedPreferences).

    Intercepción: Autenticación dinámica que evita la exposición de tokens en logs o almacenamiento innecesario.
