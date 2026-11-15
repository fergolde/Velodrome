# Feature: impeccable full-project

## Specs
S1: Trabajo en rama separada `impeccable/full-project`, alcance todo el proyecto.
S2: Flujo impeccable para nativo Android: init (PRODUCT.md) -> document (DESIGN.md) -> critique nativo (solo Assessment A) -> audit nativo -> fixes acotados -> finish.
S3: Sin detector web ni live/generate en nativo; verificación por builds/screenshots de dispositivo.
S4: No reemplazar identidad sin aprobación; refinamiento preserva comportamiento y copy.
S5 (critique 20/40): P1 nombres accesibles (back CD=null, icon-only sin nombre, chip sin selected, slider sin anuncio, handle 32dp); P1 errores sin retry + SnackbarHost muerto en búsqueda; P1 strings ES hardcodeados a recursos ES+EN; P2 targets 48x48 (mini play 36dp, slider height 20dp, radio 40dp, chips 38dp) + alternativa a swipe; P2 consistencia (sheet Bg3 vs surface, clearance 100 vs 36, currentRoute string vs Routes tipadas, snackbar duplicado en 4 archivos).
S6: Autorizado "haz todo lo que veas mas recomendable": implementar todo S5 en esta rama por unidades revisables.

## Tasks
- T1 (S2): Entrevista + PRODUCT.md -> odd/tasks/impeccable-design-system.md, commit
- T2 (S2): DESIGN.md scan desde Theme.kt y componentes -> commit
- T3 (S2,S4): Critique nativo superficies clave -> snapshot
- T4 (S2): Audit nativo + fixes (clarify/layout/typeset/harden) -> commit(s)
- T5: Finish review + cierre

## Log
L1: Usuario: "hazlo en todo el proyecto. pero trabaja en una rama separada" (tras pedir plan impeccable).
L2: Rama creada: impeccable/full-project desde master.
L3: Workers aplicaron S5 completo (a11y, snackbar, strings ES+EN, targets 48dp, consistencia, retry en 3 VMs con tests RED-GREEN); checks verdes según handoffs, pendiente verificación propia y commits.
L4: Verificación propia: testDebugUnitTest + lintDebug --rerun-tasks en verde. Commits 94fe745 (AGENTS.md), cae95ab (PRODUCT/DESIGN/sidecar/task doc), 768d7c3 (fix UI 20 archivos + 4 tests nuevos). Límite: el slice fix supera ~400 líneas como unidad única porque res y usos no se pueden partir sin romper el build; queda registrado el borde.
L5: Verificador independiente: todo PASS (retry sin leaks con sondas 4/4, VeloTab sin regresión, seguridad intacta, 315 tests + lint 0 errores). 4 advisories aplicados (copy baseline restaurado con action_play_all, KDoc VeloTab, default muerto, tests retry x3 en 3 VMs) + residuo ArtistDetail:234 a action_play_all. Verificación final propia en verde.
L6: Usuario pidió quitar el botón de quitar pista en la cola (queda solo swipe lateral). Quitado IconButton + import Close + claves queue_remove ES/EN. Tradeoff: TalkBack pierde la alternativa al swipe en esa fila.
