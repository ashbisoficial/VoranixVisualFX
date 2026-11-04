package com.voranix.visualfx.client;

import com.voranix.visualfx.network.FogZonePacket;
import net.minecraft.client.Minecraft;

/**
 * A diferencia de SkyColorEffect (que se apaga solo después de "duración" ticks), esto es un
 * efecto de ida y vuelta: entra en rampa hacia 1 cuando el jugador pisa una zona de niebla, y
 * hacia 0 cuando la pisa de vuelta hacia afuera (lo dice el plugin con cada paquete, según en
 * qué zona esté el jugador). "fundidoTicks" es cuánto tarda esa rampa, para que no sea un
 * cambio brusco al cruzar el borde de la zona.
 */
public final class FogZoneEffect {

    private static float rojo, verde, azul, cercana, lejana;
    private static boolean objetivoActivo;
    private static long inicioRampaTick = -1;
    private static int fundidoTicks = 1;

    private FogZoneEffect() {
    }

    public static void aplicar(FogZonePacket pkt) {
        boolean nuevoActivo = pkt.lejana() > 0;
        if (nuevoActivo) {
            rojo = pkt.rojo() / 255f;
            verde = pkt.verde() / 255f;
            azul = pkt.azul() / 255f;
            cercana = pkt.cercana();
            lejana = pkt.lejana();
        }
        if (nuevoActivo != objetivoActivo || inicioRampaTick < 0) {
            inicioRampaTick = tickActual();
        }
        objetivoActivo = nuevoActivo;
        fundidoTicks = Math.max(1, pkt.fundidoTicks());
    }

    private static long tickActual() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0;
    }

    /** 0 = niebla vanilla normal, 1 = niebla de la zona a pleno. */
    public static float intensidad(float partialTick) {
        if (inicioRampaTick < 0) {
            return objetivoActivo ? 1f : 0f;
        }
        double transcurrido = (tickActual() - inicioRampaTick) + partialTick;
        float t = (float) Math.max(0, Math.min(1, transcurrido / fundidoTicks));
        return objetivoActivo ? t : 1f - t;
    }

    public static float rojo() {
        return rojo;
    }

    public static float verde() {
        return verde;
    }

    public static float azul() {
        return azul;
    }

    public static float cercana() {
        return cercana;
    }

    public static float lejana() {
        return lejana;
    }
}
