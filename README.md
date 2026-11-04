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
| 1 | rojo | byte (0-255) |
| 1 | verde | byte (0-255) |
| 1 | azul | byte (0-255) |
| 4 | duración | int, big-endian, en ticks (0 = apagar ya) |
| 4 | fundido | int, big-endian, en ticks (cuánto tarda en entrar/salir) |

Son 13 bytes, todos de tamaño fijo (nada de VarInt), para que el lado del plugin los arme con un `DataOutputStream` común sin tener que reimplementar el formato de enteros variables de Minecraft. La clase que los arma y los manda, del lado del plugin, es `VisualFxBridge.java` en el repo de `VoranixExtremo` — **si se cambia este formato acá, hay que cambiarlo ahí también**, byte a byte.

## Qué hace con eso

`client/SkyColorEffect.java` guarda el color y calcula, cuadro a cuadro, una "intensidad" de 0 a 1 según cuánto pasó desde que empezó, con fundido de entrada y salida. `client/ClientEvents.java` mezcla ese color sobre la niebla (`ViewportEvent.ComputeFogColor`), que es lo que define el tono del cielo/horizonte en Minecraft vanilla — sin efecto activo, no toca absolutamente nada.

## Estado / próximos pasos

Compiló limpio (`./gradlew build`, Forge 47.2.0 / Java 17) después de corregir un nombre de clase (`ClientEvents.java` usaba `EntityViewRenderEvent.FogColors`, que en Forge moderno se llama `ViewportEvent.ComputeFogColor` — ya corregido, verificado contra la fuente real de Forge). Falta probarlo en el juego:

1. `./gradlew runClient` para probarlo a mano (se puede llamar `SkyColorEffect.aplicar(...)` desde un comando de prueba, o levantar `runServer` + el plugin de verdad en un server de prueba).
2. Confirmar en el server híbrido que el `/fx cielo <color>` del plugin realmente le llega y se ve.
3. Una vez confirmado, el `.jar` va donde el instalador reparte los mods de la serie.

Ideas para después, si hace falta más que el cielo: la misma estructura (un paquete nuevo, un `id` nuevo en `NetworkHandler`) sirve para cualquier otro efecto puramente visual (vibrado de cámara, viñeta de color, etc.) sin tocar lo que ya hay.
