---
name: Velodrome
description: Sala de maquinas musical nocturna y sobria para tu servidor Navidrome.
colors:
  bg: "#0A0B0F"
  bg3: "#161920"
  bg4: "#1E2129"
  border: "#2A2E3A"
  text-primary: "#F0F0F0"
  text-secondary: "#8A8F9E"
  text-tertiary: "#4A4F5E"
  accent: "#C8FF00"
  destructive: "#EF5350"
  feature-offline: "#0F1F18"
  feature-top100: "#1F1508"
  feature-discovery: "#1A0F1F"
  lastfm-red: "#E2231A"
typography:
  display:
    fontFamily: "Syne ExtraBold, sans-serif"
    fontSize: "32sp"
    fontWeight: 800
    lineHeight: "36sp"
  headline:
    fontFamily: "Syne ExtraBold, sans-serif"
    fontSize: "28sp"
    fontWeight: 800
    lineHeight: "32sp"
  title:
    fontFamily: "DM Sans SemiBold, sans-serif"
    fontSize: "15sp"
    fontWeight: 600
  body:
    fontFamily: "DM Sans, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
  label:
    fontFamily: "DM Sans SemiBold, sans-serif"
    fontSize: "10sp"
    fontWeight: 600
    letterSpacing: "2sp"
rounded:
  sm: "8dp"
  md: "16dp"
  lg: "28dp"
  pill: "999dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "#000000"
    rounded: "{rounded.pill}"
    padding: "16dp 16dp"
  search-bar:
    backgroundColor: "{colors.bg3}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.md}"
    padding: "0dp 16dp"
  miniplayer:
    backgroundColor: "{colors.bg3}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.md}"
    padding: "12dp 12dp"
  nav-bar:
    backgroundColor: "{colors.bg3}"
    textColor: "{colors.text-secondary}"
    padding: "0dp 0dp"
---

# Design System: Velodrome

## Overview

**Creative North Star: "La Sala de Maquinas"**

Velodrome es una sala de maquinas nocturna: precisa, sobria, sin decoracion prestada. La densidad esta al servicio de la escucha diaria, no del adorno. Material 3 gobierna estructura y navegacion; la marca vive en el tema (roles de color, escala tipografica, forma y ritmo), no en componentes inventados.

Rechaza el disfraz iOS, los neones genericos, el glass y las sombras arbitrarias. Lo que brilla es una sola voz por pantalla: el acento lima sobre fondo nocturno.

**Key Characteristics:**
- Nocturno por defecto, dark-only disenado (no invertido).
- Geometria sobria: 8-16dp en contenido, 28dp solo en cover del player.
- Una sola voz: un acento por pantalla, resto neutro.
- Tacto real: objetivos 48x48dp minimo.

## Colors

Lima senal sobre noche. Neutros tintados del mundo mas un acento.

### Primary
- **Lima Senal** (#C8FF00): acento firma. Play principal, seleccion, progreso, iconos activos. Nota: el picker defaultea a #B6A0FF en `SettingsViewModel`; el canon es #C8FF00 de `Theme.kt`.
- **Rojo Last.fm** (#E2231A): solo marca Last.fm.
- **Destructivo** (#EF5350): acciones destructivas.

### Neutral
- **Noche** (#0A0B0F): fondo base.
- **Sala 3** (#161920): superficies (`surface`).
- **Sala 4** (#1E2129): variante de superficie.
- **Borde Sala** (#2A2E3A): separadores y bordes.
- **Texto Principal** (#F0F0F0): texto primario.
- **Texto Secundario** (#8A8F9E): cuerpo pequeno y metadatos.
- **Texto Terciario** (#4A4F5E): labels de 10sp con tracking.

### Named Rules
**The Una Sola Voz Rule.** Un acento por pantalla. Si el lima ya habla en el play y el progreso, nada mas compite.

## Typography

**Display Font:** Syne ExtraBold (display/headline)
**Body Font:** DM Sans regular/medium/semibold (cuerpo, titulos chicos, labels)
**Label/Mono Font:** DM Sans SemiBold para labels

**Character:** Titulares con maquina y presencia, cuerpo sobrio y legible. Nada de serif editorial prestada.

### Hierarchy
- **Display** (ExtraBold, 32sp/36sp): hero del player y titulos grandes.
- **Headline** (ExtraBold, 28sp/32sp y 22sp/26sp): cabeceras de seccion.
- **Title** (SemiBold, 15sp): titulos de fila y seccion.
- **Body** (400, 16sp/14sp/12sp): cuerpo y metadatos; 12sp siempre en secundario.
- **Label** (SemiBold, 10sp, tracking 2sp, terciario): etiquetas en mayusculas.

### Named Rules
**The Roles Not Sizes Rule.** Todo texto mapea a un rol Material; nunca tamanos a mano por pantalla.

## Layout

Modelo compacto con barra inferior de 3 destinos (Home/Explore/Settings) en telefono; en expanded pasar a rail o drawer, nunca la bottom-bar estirada. Escala inline 4/8/12/16/20/24/32dp, sin `dimens.xml`. Pantallas con columna a 20dp horizontal y 24dp entre bloques (Settings). Player con cover de 320dp alto y ritmo vertical marcado. Sin `WindowWidthSizeClass` detectado: el soporte tablet es deuda.

## Elevation & Depth

Sistema plano por defecto con niveles tonales (Bg3/Bg4) y borde #2A2E3A. Sin sombras custom en tema; `tonalElevation 0dp` en nav. Excepcion aprobada: elevacion minima solo en player y sheets para separar del fondo.

### Named Rules
**The Tonal First Rule.** La profundidad se dice con tono y borde; la sombra solo aparece como respuesta en player/sheets.

## Shapes

Lenguaje sobrio: 8dp en portadas chicas (44dp), 16dp en buscador/cards/miniplayer, 28dp en cover grande del player, pill en play y controles circulares. Sin `VeloShapes` declarado: documentar aqui es el canon hasta centralizarlo.

## Components

### Buttons
- **Shape:** pill en play (72dp con icono 34dp), 52dp secundarios, 36dp iconicos.
- **Primary:** fondo lima, icono negro.
- **Hover / Focus:** estados Material; respetar Remove animations con crossfade.
- **Secondary:** iconos sobre superficie, 24dp.

### Chips
- **Style:** por definir en codigo actual; si aparecen, fondo superficie con texto primario y borde sala.
- **State:** seleccionado en lima, no seleccionado en secundario.

### Cards / Containers
- **Corner Style:** 16dp.
- **Background:** Sala 3, variante Sala 4.
- **Shadow Strategy:** plano; ver Elevation.
- **Border:** 1dp Borde Sala cuando necesita separacion.
- **Internal Padding:** 12-16dp.

### Inputs / Fields
- **Style:** barra 50dp alto, 16dp radio, fondo superficie, borde sala, icono 18dp en primario, texto 14sp DM Sans.
- **Focus:** borde en acento, sin glow inventado.
- **Error / Disabled:** destructivo para error; terciario para deshabilitado.

### Navigation
- Barra inferior Material, contenedor superficie al 95%, 3 items, seleccionado en primario. Back del sistema siempre funciona, incl. gesto predictivo. Edge-to-edge con insets.

## Do's and Don'ts

### Do:
- **Do** usar un acento por pantalla y dejar el resto neutro.
- **Do** mapear todo texto a roles Display/Headline/Title/Body/Label.
- **Do** mantener 48x48dp minimo con 8dp entre objetivos.
- **Do** probar dark + font scale 1.3 antes de dar por lista una pantalla.

### Don't:
- **Don't** copiar controles iOS ni inventar equivalentes Material.
- **Don't** poner neones, glass, ni sombras arbitrarias fuera de player/sheets.
- **Don't** usar hex crudo fuera de roles; los roles resuelven dark y contraste.
- **Don't** estirar la bottom-bar del telefono en tablet sin rail/drawer.
