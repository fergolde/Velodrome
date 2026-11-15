# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Autohosteador musical: dueño de una biblioteca propia en un servidor Navidrome accesible, que escucha a diario desde Android 14+. Su trabajo es acceder a toda su colección desde cualquier lugar, mantener la cola y el punto de reproducción entre sesiones, y descubrir música sin entregar datos a terceros.

## Product Purpose

Dar acceso rápido, estable y privado a la colección musical propia. Existe para que la música le pertenezca al usuario: sin suscripciones, sin algoritmos invasivos, sin publicidad. Éxito = abrir, darle play y que suene, incluso con red inestable o sin red.

## Positioning

Control privado total que un cliente vecino no puede copiar con verdad: caché inteligente de lo escuchado (hasta 20 GB configurables), radio adaptativa que aprende de las escuchas, ecualizador automático por género, scrobbling con cola offline por lotes, y cero rastreadores con credenciales cifradas en el dispositivo. Todo apunta al servidor del usuario.

## Operating Context

Uso móvil cotidiano: streaming con precarga del siguiente tema, integración con notificación multimedia y controles de auriculares/Bluetooth, cola persistente que sobrevive al proceso, modo offline que reproduce solo lo local, scrobbling que se agrupa sin conexión y se envía al volver. Español por defecto + inglés. Interfaz oscura Material 3.

## Capabilities and Constraints

Capacidades confirmadas: streaming optimizado, cola avanzada (reordenar, deslizar, reproducir ahora/siguiente/al final), radio instantánea/de artista/por género y años, perfil de gusto multi-dispositivo desde estadísticas de Navidrome, offline inteligente, biblioteca paginada, búsqueda local con debounce + remota, scrobbling con Last.fm vía servidor, personalización de acento incl. HEX.

Constraints duros (no romper en ningún rediseño): compatibilidad con API Subsonic/Navidrome, reproducción solo-local en modo offline, cola y punto de reproducción restaurables, claves de caché por cuenta, token Subsonic nunca fuera del host configurado, comandos de sesión por confianza (allowlist para no confiables). Hecho explícitamente indeciso: estándar formal de accesibilidad más allá de Material/defaults.

## Brand Commitments

Nombre Velodrome, tagline "Tu biblioteca. Tu servidor. Tus reglas.", voz directa y sobria, sin gamificación ni hype. Español como locale por defecto (`res/values/strings.xml`) + inglés (`res/values-en/`). Estética oscura limpia. Sin testimonios ni claims inventados.

## Evidence on Hand

README con filosofía y features, `app/` Compose + Media3 + Room/WorkManager/Retrofit, `res/values` + `res/values-en`, `AGENTS.md` con invariantes de seguridad. Ausencias que no se deben fabricar: benchmarks, usuarios/prensa, pricing.

## Product Principles

1. Mi música, mis reglas: el servidor del usuario manda.
2. Suena siempre: inestable o sin red no es excusa.
3. Privacidad por construcción, no por promesa.
4. Potente sin ruido: sobrio, rápido, sin gamificación.
5. Lo local primero cuando la red falla.
