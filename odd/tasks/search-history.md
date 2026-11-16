# Feature: search-history

## Specs
S1: Historial y sugerencias en búsqueda — últimas queries guardadas por cuenta en Room (tabla search_history con accountScope, dedupe, tope 10, borrar una y borrar todo) + sugerencias por prefijo sobre la biblioteca local mientras se escribe. (Nota: la spec original decía DataStore; se implementó en Room por query-by-prefix y reglas Room del proyecto.)
S2 (fuera de alcance, follow-up): Radio multi-semilla + exclusiones.
S3: Sin red para el historial; no sale del dispositivo. Query vacía muestra historial; con texto muestra sugerencias locales + resultados remotos como hoy.

## Tasks
- T1 (S1): Mapear flujo de búsqueda y datos locales disponibles — vía explorer
- T2 (S1): Historial por cuenta (guardar, deduplicar, límite, borrar)
- T3 (S1): Sugerencias por prefijo + UI (historial con query vacía)
- T4 (S1): Tests + lint + screenshots
- T5: Finish review y cierre

## Log
L1: Usuario elige el punto 3 del roadmap tras cerrar artist-sort.
L2: Mapa listo: sin search en TrackDao; DAOs con LIKE %query%; accountScope vía CredentialsManager; sin precedente de lista en DataStore (Room encaja mejor).
L3: Worker implementó historial Room (v3 + MIGRATION_2_3, dedupe, tope 10, por cuenta, wipe en purge) + sugerencias por prefijo + UI (historial con query vacía, sugerencias antes de resultados); 30 tests nuevos RED-GREEN (GREEN con mutaciones revertidas); suite 392 verdes, lint limpio. Spec S1 actualizada: Room en vez de DataStore.
L4: Verificación propia + independiente todo PASS (migración solo agrega con DDL byte-idéntico, aislamiento por cuenta, scope limpio, suite 399 verdes, lint 0 errores). Sumados 2 tests durables del verificador (SearchHistoryMigrationDdlTest, SearchHistoryPrefixBoundaryTest; un caso corregido a contrato real: lo guardado siempre entra normalizado). Capturas en tablet: sugerencias SUGGESTIONS + RECENT SEARCHES con Nach + migración v3 en device.
