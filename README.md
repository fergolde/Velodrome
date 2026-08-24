<div align="center">

# 🎵 Velodrome

### Tu biblioteca musical. Tu servidor. Tus reglas.

Cliente Android moderno para Navidrome, diseñado para ofrecer una experiencia rápida, elegante y privada.

<p>
  <img src="https://img.shields.io/badge/Android-14%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Navidrome-Compatible-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-GPLv3-orange?style=for-the-badge" />
</p>

</div>

---

## ✨ ¿Qué es Velodrome?

Velodrome es un cliente Android para Navidrome que te permite acceder a toda tu colección musical desde cualquier lugar.

Reproducción optimizada con caché inteligente, radio adaptativa que aprende de tus escuchas, ecualizador automático por género y gestión completa de cola. Pensado para quienes quieren controlar completamente su música sin depender de servicios de streaming comerciales.

---

## 🚀 Características

### 🎧 Reproducción fluida
- Streaming optimizado con **precarga de la siguiente canción**.
- Reproducción estable incluso con conexiones inestables.
- Integración completa con el sistema: notificación multimedia, controles de auriculares y Bluetooth.
- **Cola persistente**: si cierras la app, al volver tienes tu cola exactamente donde la dejaste.

### 🎛 Gestión avanzada de cola
- **Reordenar arrastrando** desde el asa de cada tema.
- Desliza para quitar, toca para saltar.
- Añadir canciones: reproducir ahora, reproducir siguiente o al final de la cola.
- La cola no muere con el proceso: lista, canción y punto de reproducción se restauran solos.

### 📻 Radio Inteligente
Mezclas automáticas que se extienden solas mientras escuchas:

- **Mezcla instantánea**: mantén pulsada cualquier canción y genera una radio a partir de ella. Tu canción suena primera.
- **Radio de artista**: empieza centrada en ese artista y abre hacia tus afinidades a medida que avanza.
- **Perfil de gusto multi-dispositivo**: derivado de las estadísticas de tu servidor Navidrome, así que todos tus dispositivos comparten el mismo perfil.
- Exploración equilibrada (~40%) para descubrir música nueva sin perder identidad.
- Shuffle aleatorio clásico y radios filtradas por género y años también incluidos.

### 🎛 Ecualizador automático
- **Se adapta solo al género de cada canción**: rock, electrónica, jazz, clásica, pop, folk y más.
- Refuerzo de graves opcional aparte.
- Actívalo o desactívalo desde Ajustes › Sonido, con efecto inmediato.
- Sin soporte en el dispositivo, simplemente queda inactivo.

### 📦 Offline inteligente
- Caché automática de lo que escuchas (hasta 20 GB configurables).
- Escucha tu música sin conexión.
- Modo offline que reproduce solo lo que tienes en local.
- Caché de imágenes independiente y configurable.

### 🎼 Biblioteca completa
- Inicio con: recién añadidos, más reproducidos, recientes, aleatorios, géneros y tus playlists.
- Artistas y álbumes con carga paginada fluida.
- Detalle de álbum, artista y playlist con layouts específicos para vertical, horizontal y tablet.
- Playlists del servidor con su carátula y lista de temas.

### 🔎 Búsqueda rápida
Búsqueda local e instantánea de canciones, artistas y álbumes con *debounce* inteligente, además de búsqueda remota en el servidor desde Explorar.

### 📝 Scrobbling
- Envía a tu servidor Navidrome lo que escuchas, con detección precisa de transiciones.
- Funciona sin conexión: los scrobbles pendientes se agrupan y envían por lotes al volver.
- Si tu servidor Navidrome está conectado a Last.fm, tu historial llega allí automáticamente.

### 🎨 Personalización
- Color de acento configurable, incluido HEX personalizado.
- Español e inglés.
- Interfaz oscura limpia y moderna.

### 🔒 Privacidad primero
- Sin rastreadores ni analíticas.
- Credenciales cifradas en el dispositivo.
- Todo apunta a tu servidor: tus datos permanecen bajo tu control.

---

## ⚙️ Requisitos

- Android 14 o superior
- Servidor Navidrome accesible
- Cuenta de usuario válida
- (Opcional) Servidor Navidrome conectado a Last.fm si quieres llevar tu historial allí

---

## 🏁 Primeros pasos

1. Instala Velodrome.
2. Abre la aplicación.
3. Introduce:
   - URL de tu servidor Navidrome
   - Usuario
   - Contraseña
4. ¡Empieza a escuchar tu colección!

Consejo: activa la **Radio Inteligente** desde cualquier canción (mantén pulsado → *Mezcla instantánea*) y el **ecualizador automático** en Ajustes › Sonido.

---

## 🏗️ Tecnologías

- **Lenguaje:** Kotlin 100%
- **UI:** Jetpack Compose + Material Design 3
- **Audio:** Media3 / ExoPlayer + MediaSession
- **Arquitectura:** MVVM + Clean Architecture (domain / data / presentation)
- **Datos:** Room (con migraciones), DataStore, Paging 3
- **Red:** Retrofit + OkHttp + Moshi (Subsonic/Navidrome API)
- **Imágenes:** Coil 3 con claves de caché estables para Navidrome
- **Inyección:** Hilt
- **Background:** WorkManager (sincronización de biblioteca y scrobbles)
- **Async:** Coroutines + Flow
- **Testing:** JUnit + MockK + Turbine

---

## 🎯 Filosofía del proyecto

Velodrome nace con una idea sencilla:

> Tu música debería pertenecerte a ti.

Sin suscripciones obligatorias.
Sin algoritmos invasivos.
Sin publicidad.

Solo tú, tu biblioteca y la música que te gusta.

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas.

Si encuentras un error o tienes una idea para mejorar Velodrome:

1. Abre un Issue.
2. Crea una rama.
3. Envía un Pull Request.

---

## 📄 Licencia

Este proyecto se distribuye bajo GPLv3.

---

<div align="center">

### 🎵 Escucha tu música sin límites

**Velodrome • Android Client for Navidrome**

</div>
