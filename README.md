# VoranixVisualFX

Mod cliente de Forge, **solo efectos visuales** — sin bloques, items ni mecánicas de juego. Pensado para que producción pueda cambiar cosas como el color del cielo "en el momento que quiera" durante el stream, controlado desde el servidor (el plugin [VoranixExtremo](https://github.com/ashbisoficial/VoranixExtremo)), no desde el mod en sí.

## Por qué existe

El plugin principal (`VoranixExtremo`) es Paper/Spigot a propósito, para que cualquiera pueda conectarse sin instalar nada. Pero Minecraft no deja recolorear el cielo de verdad (el degradado azul calculado por bioma) desde comandos de servidor — eso es render del cliente. Como la serie ya tiene un instalador que mantiene actualizado un mod Forge en las máquinas de quienes participan, tiene sentido que este efecto puntual viva en un mod chiquito en vez de simularlo con partículas.

Si alguien se conecta **sin** este mod, no pasa nada: el servidor y el resto del plugin funcionan igual, esa persona simplemente no ve el efecto de cielo (Minecraft descarta en silencio un paquete de un canal que el cliente no registró).

## Requisitos

- Minecraft **1.20.1**
- Forge **47.2.0** (mismo que [`voranixmod`](https://github.com/ashbisoficial/voranixmod))
- Java 17

## Compilar

```bash
./gradlew build
```
El `.jar` queda en `build/libs/`.

## Cómo se controla (protocolo)

El plugin `VoranixExtremo` le manda al mod paquetes por el canal de red `voranixvisualfx:main` (un [plugin message](https://wiki.vg/Plugin_channels) común y corriente, nada de HTTP ni RCON — viaja por la misma conexión del jugador). Cada uno tiene su `id` (primer byte, lo consume `NetworkHandler` antes del resto del payload). Los campos de texto (nombre de zona) usan un formato propio — 1 byte de longitud + UTF-8 (`network/ByteUtil.java`) — **no** el `writeUtf` nativo de Minecraft, que usa VarInt para la longitud; así del lado del plugin se arman con un `DataOutputStream` común sin reimplementar el VarInt de Minecraft. Todo el resto de los campos también es de tamaño fijo por el mismo motivo.

### id 0 — `SkyColorPacket`: color de cielo global

| Bytes | Campo | Tipo |
|---|---|---|
| 1 | rojo horizonte | byte (0-255) |
| 1 | verde horizonte | byte (0-255) |
| 1 | azul horizonte | byte (0-255) |
| 1 | rojo cenit | byte (0-255) |
| 1 | verde cenit | byte (0-255) |
| 1 | azul cenit | byte (0-255) |
| 4 | duración | int, big-endian, en ticks (0 = apagar ya) |
| 4 | fundido | int, big-endian, en ticks (cuánto tarda en entrar/salir) |

"Horizonte" y "cenit" pueden ser el mismo color (cielo parejo) o distintos (degradado).

### id 1 — `ZoneAddPacket`: agrega (o reemplaza) una zona de humo/niebla

| Bytes | Campo | Tipo |
|---|---|---|
| 1+N | nombre | texto (`ByteUtil`) |
| 1 | tipo | byte (0 = caja, 1 = esfera) |
| 8×6 | p0..p5 | double — **caja:** minX,minY,minZ,maxX,maxY,maxZ · **esfera:** centroX,centroY,centroZ,radio,-,- |
| 3 | color | byte×3 (0-255) |
| 4 | cercana | float, bloques donde arranca la niebla adentro de la zona |
| 4 | lejana | float, bloques donde ya no se ve nada adentro de la zona |
| 4 | margen | float, bloques de caída ANTES de llegar al borde (fuera de la zona) |
| 1 | creciendoActivo | byte (0/1) — reservado para la "máquina de humo", sin usar todavía |
| 8 | tickInicio | long, gameTime del mundo — reservado, sin usar todavía |
| 4 | velocidadBloquesPorSegundo | float — reservado, sin usar todavía |
| 4 | radioMaximo | float — reservado, sin usar todavía |

### id 2 — `ZoneRemovePacket`: saca una zona por nombre

| Bytes | Campo | Tipo |
|---|---|---|
| 1+N | nombre | texto (`ByteUtil`) |

### id 3 — `ZoneClearPacket`: "borrá todas las zonas que tengas"

Sin payload. El plugin lo manda antes de reenviar las zonas de un mundo nuevo (al conectarse, registrar el canal, o cambiar de mundo), para que el cliente nunca tenga zonas de un mundo que no es el actual — el plugin decide **qué zonas mandar** según en qué mundo está cada jugador; el cliente no filtra nada por su cuenta, confía en que lo que tiene en su registro es del mundo correcto.

Las clases que arman y mandan estos tres últimos, del lado del plugin, están en `VisualFxBridge.java`/`FogZoneManager.java` del repo de `VoranixExtremo` — **si se cambia alguno de estos formatos acá, hay que cambiarlo ahí también**, byte a byte.

## Qué hace con eso

- `client/SkyColorEffect.java` — color de cielo global, con su rampa de entrada/salida (eso no cambió).
- `client/SmokeZone.java` — una zona de humo (caja o esfera, la misma clase con un campo de tipo), con `density(x, y, z, tick)`: 1.0 adentro, cae lineal en los últimos `margen` bloques antes del borde, 0.0 más allá.
- `client/SmokeZoneRegistry.java` — qué zonas hay activas ahora (las que mandó el plugin para este mundo) y `mezclar(x, y, z)`: la densidad **máxima** entre todas las que tocan ese punto, con el color mezclado por peso de cada una que esté aportando.

`client/ClientEvents.java` junta todo, cada frame, sin ningún paquete de red ni rampa de por medio para el humo (eso es justamente lo que cambió respecto a la versión anterior — antes el servidor calculaba la densidad cada medio segundo y el cliente suavizaba hacia ese valor; ahora el cliente calcula todo en vivo contra la posición real de la cámara):

1. Tiñe la niebla (`ViewportEvent.ComputeFogColor`) con el color de horizonte del cielo global y, encima, con el color mezclado de las zonas de humo que toquen la cámara — se pueden combinar los dos a la vez.
2. Dibuja el domo del cielo global (`RenderLevelStageEvent.AFTER_SKY`) por encima de todo, sin escribir profundidad — igual que el propio cielo de Minecraft, así que terreno, nubes, sol y luna se siguen viendo normal. Esto es lo único que cubre **mirando para arriba** por el cielo de color; la niebla de zona usa otro mecanismo para lo mismo (punto 4).
3. Achica la distancia de niebla (`ViewportEvent.RenderFog`, cancelando el evento como pide su Javadoc) según la densidad de humo en la posición de la cámara, con forma **esférica** (`FogShape.SPHERE`, no el cilindro por defecto) para que mirar arriba/abajo adentro de una zona se sienta igual de tapado que mirar al horizonte.
4. Dibuja una **capa de color a pantalla completa** (`RenderGuiOverlayEvent.Pre`, justo antes del hotbar) con alpha proporcional a la densidad — esto es lo que tapa el cielo/sol/nubes de fondo cuando estás adentro de una zona de humo, que la niebla de distancia sola no alcanza a cubrir (esas cosas no tienen una "distancia" real). No tapa el resto del HUD (hotbar, vida, chat, scoreboard, etc.) porque se dibuja ANTES y no cancela el evento — todo eso se sigue pintando encima, normal.

Sin zonas activas ni cielo de color, ninguna de las partes toca nada.

## Estado / próximos pasos

Lo del cielo (domo, culling, `BufferUploader`) ya está probado y funcionando en el juego. **La sincronización de zonas (Fase 1+2) se probó en el servidor real y funciona** — la niebla de distancia se aplicaba, pero con los defaults viejos (cercana/lejana 2-18) se veía muy larga y el cielo de fondo no quedaba cubierto, así que se ajustó: defaults 0/4, forma esférica, y la capa de pantalla completa (punto 4 de arriba). Esto último (`FogShape.SPHERE` y `RenderGuiOverlayEvent.Pre`) **todavía no se compiló ni se probó** en el juego.

**Las partículas vanilla del lado del plugin se sacaron** (`FogZoneManager.animarHumo()` ya no existe) — se veían como puntitos negros sueltos, no como una nube. Hoy, sin el mod instalado, una zona no se nota en absoluto.

Pendiente, en fases ya acordadas con el usuario:
- **Fase 3** — máquina de humo: una zona esfera que crece y después encoge sola, usando los campos `creciendoActivo`/`tickInicio`/`velocidadBloquesPorSegundo`/`radioMaximo` que el protocolo ya reserva (por eso van en el paquete desde ahora, aunque hoy siempre lleguen en 0/false).
- **Fase 4** — humo visible de verdad: una partícula custom del mod, grande (3-6 bloques) y de vida larga, generada del lado del cliente dentro del volumen de cada `SmokeZone` (reemplaza a la capa de pantalla plana del punto 4 de arriba, que es un placeholder más simple mientras tanto).

Ideas para después, sin relación con el humo: la misma estructura (un paquete nuevo, un `id` nuevo en `NetworkHandler`) sirve para cualquier otro efecto puramente visual (vibrado de cámara, viñeta de color, HUD de corazones custom, etc.) sin tocar lo que ya hay.
