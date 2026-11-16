# Feature: artist-sort

## Specs
S1: Orden y filtros en artista/álbum (año ↑↓, nombre, aleatorio, más tocadas por historial local de scrobbles). No tocar el orden de tracks del detalle de álbum.
S2 (fuera de alcance, follow-up): Historial y sugerencias en búsqueda.
S3 (fuera de alcance, follow-up): Radio multi-semilla + exclusiones.

## Tasks
- T1 (S1): Mapear datos y readiness (campos año/playCount, loaders, settings) — vía explorer
- T2 (S1): Orden de álbumes en artista (año ↑↓, nombre, más tocadas, aleatorio)
- T3 (S1): Filtros + preferencia persistida
- T4 (S1): Tests + lint + screenshots
- T5: Finish review y cierre

## Log
L1: Usuario elige el punto 2 del roadmap tras cerrar tablet (rama feature/tablet-polish pendiente de push).
L4: Usuario: en artist horizontal los álbumes no ocupan todo el ancho. Fix: artistAlbumsContentMaxWidth (horizontal sin tope) + columnas horizontales 3/4/6 para igualar tamaño de tarjeta al vertical (~180-200dp); suite 408 verdes, lint limpio, capturas en tablet.
L5: Columnas por orientación 3/4/6 en horizontal (vertical 2/4/5) + bump 3.0.7 para integrar.
L2: Mapa listo: álbumes vienen de getArtist sin playCount (solo SongDto lo trae); default actual año asc con nulls a 0; patrón Settings/DataStore identificado. Usuario decide más-tocadas por historial local.
L3: Worker implementó orden (año ↑↓, nombre, aleatorio con semilla, más tocadas por playCount local de Room) + filtro por año + preferencia persistida (clave artist_album_sort_order); 16+5+3 tests nuevos RED-GREEN; suite 355 verdes, lint limpio. Gap documentado en KDoc: sin log durable de scrobbles en device.
L5: Usuario: segundo tap sobre el año seleccionado vuelve a Todos. Implementado como toggle en el chip (sin test JVM posible, verificado con suite + lint en verde). Capturas en tablet del sort + filtro.
L6: Usuario: el chip seleccionado debe llevar el color de acento, no gris. Aplicado selectedContainerColor=primary + selectedLabelColor=onPrimary en los chips de año.
L7: Concepto en prueba (usuario): header sin avatar de 180dp, portada a sangre completa difuminada (blur + scrim) reutilizando el cargador existente, degradado a fondo plano sin portada. Captura en tablet verifica bleed completo y legibilidad. Usuario fija blur en 10dp.
