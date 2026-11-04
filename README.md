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

El plugin `VoranixExtremo` le manda al mod un paquete por el canal de red `voranixvisualfx:main` (un [plugin message](https://wiki.vg/Plugin_channels) común y corriente, nada de HTTP ni RCON — viaja por la misma conexión del jugador). Formato exacto, en orden (ver `network/SkyColorPacket.java`):

| Bytes | Campo | Tipo |
|---|---|---|
| 1 | id del paquete | byte (siempre `0` por ahora: "color de cielo") |
| 1 | rojo horizonte | byte (0-255) |
| 1 | verde horizonte | byte (0-255) |
| 1 | azul horizonte | byte (0-255) |
| 1 | rojo cenit | byte (0-255) |
| 1 | verde cenit | byte (0-255) |
| 1 | azul cenit | byte (0-255) |
| 4 | duración | int, big-endian, en ticks (0 = apagar ya) |
| 4 | fundido | int, big-endian, en ticks (cuánto tarda en entrar/salir) |

Son 16 bytes, todos de tamaño fijo (nada de VarInt), para que el lado del plugin los arme con un `DataOutputStream` común sin tener que reimplementar el formato de enteros variables de Minecraft. "Horizonte" y "cenit" pueden ser el mismo color (cielo parejo) o distintos (degradado). La clase que los arma y los manda, del lado del plugin, es `VisualFxBridge.java` en el repo de `VoranixExtremo` — **si se cambia este formato acá, hay que cambiarlo ahí también**, byte a byte.

## Qué hace con eso

`client/SkyColorEffect.java` guarda los dos colores y calcula, cuadro a cuadro, una "intensidad" de 0 a 1 según cuánto pasó desde que empezó, con fundido de entrada y salida. `client/ClientEvents.java` hace dos cosas con eso:

1. Tiñe la niebla con el color de horizonte (`ViewportEvent.ComputeFogColor`) — lo que ya había, define el tono mirando a los costados.
2. Dibuja un domo (un abanico de triángulos, cenit en la punta, horizonte en el borde) por encima de todo, sin escribir profundidad — igual que el propio cielo de Minecraft, así que terreno, nubes, sol y luna se siguen viendo normal, dibujados encima. Esto es lo que cubre **mirando para arriba**, que la niebla sola no tiñe.

Sin efecto activo (intensidad 0), ninguna de las dos partes toca nada.

## Estado / próximos pasos

Compiló limpio una vez (`./gradlew build`, Forge 47.2.0 / Java 17) después de corregir `ClientEvents.java` (usaba `EntityViewRenderEvent.FogColors`, que en Forge moderno se llama `ViewportEvent.ComputeFogColor` — verificado contra la fuente real de Forge). El domo del cenit (`onRenderSky`, agregado después) **todavía no se compiló ni se probó**: usa `RenderLevelStageEvent#getPoseStack()`/`getPartialTick()`, nombres habituales para esto en mods Forge 1.20.1 pero sin verificar contra el jar real — si el build tira error ahí, es lo primero a revisar (está anotado en el propio archivo).

1. `./gradlew build` — si falla, mandar el error tal cual para corregir el nombre exacto del getter que falte.
2. `./gradlew runClient` o probar en el server de verdad con `/fx cielo <color>`.
3. Confirmar especialmente: mirando derecho para arriba se ve el color (no el azul/negro de siempre), y las nubes se siguen viendo (no quedan tapadas por el domo).
4. Una vez confirmado, el `.jar` va donde el instalador reparte los mods de la serie.

Ideas para después, si hace falta más que el cielo: la misma estructura (un paquete nuevo, un `id` nuevo en `NetworkHandler`) sirve para cualquier otro efecto puramente visual (vibrado de cámara, viñeta de color, etc.) sin tocar lo que ya hay.
