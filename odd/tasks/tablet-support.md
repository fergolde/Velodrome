# Feature: tablet-support

## Specs
S1: Soporte real de tablet (rail/drawer + WindowWidthSizeClass) — hoy la bottom-bar de teléfono se estira; es la deuda visual más grande que quedó documentada.
S2 (fuera de alcance, follow-up): Orden y filtros en artista/álbum (por año, más tocadas, aleatorio).
S3 (fuera de alcance, follow-up): Historial y sugerencias en búsqueda — hoy cada búsqueda parte de cero.
S4 (fuera de alcance, follow-up): Semilla múltiple y excluir artistas/géneros en radio.

## Tasks
- T1 (S1): Mapear navegación y readiness (WindowSizeClass, deps, pantallas) — vía explorer
- T2 (S1): Cablear WindowSizeClass en VelodromeApp + GlobalScaffold (rail/drawer)
- T3 (S1): Grillas adaptativas y details a ancho contenido en tablet
- T4 (S1): Tests + lint + screenshots teléfono y tablet
- T5: Finish review y cierre

## Log
L1: Usuario: "quiero que te guardes la lista de los 4 elementos, porque primero vamos a realizar la 1, tablet real".
L2: T2 navegación adaptativa (worker): dep window-size-class, navigationLayoutFor pura + NavigationLayoutTest 4/4, rail lateral en Medium/Expanded, teléfono intacto; suite 322 verdes, lint limpio.
L3: T3 grillas y details (worker): columnas por width class con AdaptiveGridLayoutTest 4/4, details a 880dp en tablet; suite 326 verdes. Cambio visible aceptado: teléfono apaisado cae en Medium (4 columnas fijas). Follow-up sugerido: extraer helper compartido (constante 880dp triplicada).
L4: Verificación propia: testDebugUnitTest + lintDebug --rerun-tasks en verde; screenshots teléfono (bottom bar, sin rail) y tablet Pixel_Tablet (rail con selección, sin bottom bar, 4-6 columnas).
L5: Verificador independiente: todo PASS (Compact intacto, VeloTab sin regresión, scope limpio, 326 tests + lint 0 errores). Sumado NavSelectionTest del verificador (329 verdes). Commits: nav adaptativa + grillas + Log.
L6: Usuario: dock siempre abajo, nada de rail lateral. Revertida la navegación adaptativa (barras, enum, función y tests de layout fuera; visibleVeloTabs intacto); grillas y details a 880dp quedan. Test renombrado a VisibleVeloTabsTest. S1 queda parcial por decisión explícita.
