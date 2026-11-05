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

El plugin `VoranixExtremo` le manda al mod paquetes por el canal de red `voranixvisualfx:main` (un [plugin message](https://wiki.vg/Plugin_channels) común y corriente, nada de HTTP ni RCON — viaja por la misma conexión del jugador). Hay dos tipos de paquete, cada uno con su `id` (primer byte, lo consume `NetworkHandler` antes del resto del payload):

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

### id 1 — `FogZonePacket`: niebla de una zona específica del mapa

| Bytes | Campo | Tipo |
|---|---|---|
| 1 | rojo | byte (0-255) |
| 1 | verde | byte (0-255) |
| 1 | azul | byte (0-255) |
| 4 | cercana | float, big-endian, bloques donde arranca la niebla |
| 4 | lejana | float, big-endian, bloques donde ya no se ve nada (0 = apagar) |
| 4 | fundido | int, big-endian, en ticks |

A diferencia del de cielo, este no tiene "duración": el plugin decide quién está adentro de una zona (ver `FogZoneManager.java` en el repo de `VoranixExtremo`) y manda un paquete cada vez que un jugador entra o sale — "fundido" acá es cuánto tarda la transición al cruzar el borde, no cuánto dura adentro.

Todos los campos son de tamaño fijo (nada de VarInt), para que del lado del plugin se armen con un `DataOutputStream` común sin reimplementar el formato de enteros variables de Minecraft. Las clases que los arman y los mandan, del lado del plugin, están en `VisualFxBridge.java` del repo de `VoranixExtremo` — **si se cambia alguno de estos formatos acá, hay que cambiarlo ahí también**, byte a byte.

## Qué hace con eso

`client/SkyColorEffect.java` y `client/FogZoneEffect.java` guardan cada uno su color/intensidad y la van actualizando cuadro a cuadro. `client/ClientEvents.java` los lee y:

1. Tiñe la niebla (`ViewportEvent.ComputeFogColor`) con el color de horizonte del cielo global y, encima, con el color de la zona de niebla local si el jugador está adentro de una — se pueden combinar los dos a la vez.
2. Dibuja el domo del cielo global (`RenderLevelStageEvent.AFTER_SKY`, un abanico de triángulos, cenit en la punta y horizonte en el borde) por encima de todo, sin escribir profundidad — igual que el propio cielo de Minecraft, así que terreno, nubes, sol y luna se siguen viendo normal. Esto es lo único que cubre **mirando para arriba**, que la niebla sola no tiñe.
3. Achica la distancia de niebla (`ViewportEvent.RenderFog`, cancelando el evento como pide su Javadoc) cuando hay una zona de niebla activa, para que de verdad cueste ver lejos, no solo que se vea de otro color.

Sin efecto activo (intensidad 0), ninguna de las partes toca nada.

## Estado / próximos pasos

Compila y carga bien en el juego (probado por el usuario: vidas, cielo y niebla funcionando). Ajustes hechos sobre la marcha, probados:

- `BufferUploader` estaba mal importado (`net.minecraft.client.renderer` → `com.mojang.blaze3d.vertex`), confirmado contra la documentación de Mojang.
- El anillo del horizonte del domo (`ALTURA_HORIZONTE`) estaba exactamente a la altura de los ojos (0): mirando al frente la cámara lo veía de canto en vez de ver la cara inclinada, y el color recién se notaba mirando bastante para arriba. Se bajó a -90.
- El domo se dibuja desde ADENTRO (la cámara está rodeada). Sin desactivar el culling, la GPU descartaba buena parte de los triángulos según la dirección — por eso se veía fuerte yendo hacia el horizonte pero casi nada mirando para arriba. Se agregó `RenderSystem.disableCull()`/`enableCull()` alrededor del dibujo.

Todavía evaluando si con eso alcanza o si hace falta ajustar más el radio/altura del domo para que el color domine más (pedido explícito: "debe verse más el color").

Ideas para después: la misma estructura (un paquete nuevo, un `id` nuevo en `NetworkHandler`) sirve para cualquier otro efecto puramente visual (vibrado de cámara, viñeta de color, HUD de corazones custom, etc.) sin tocar lo que ya hay.

Ideas para después: la misma estructura (un paquete nuevo, un `id` nuevo en `NetworkHandler`) sirve para cualquier otro efecto puramente visual (vibrado de cámara, viñeta de color, etc.) sin tocar lo que ya hay.
