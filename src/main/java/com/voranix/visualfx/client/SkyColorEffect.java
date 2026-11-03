package com.voranix.visualfx.client;

import com.voranix.visualfx.network.SkyColorPacket;
import net.minecraft.client.Minecraft;

/**
 * Guarda el efecto de cielo activo (si hay uno) y calcula, para el tick/frame actual, qué tan
 * "adentro" del color está (0 = sin efecto, se ve todo vanilla; 1 = color a pleno), con fundido
 * de entrada y de salida. ClientEvents lee esto en cada frame para teñir la niebla.
 */
public final class SkyColorEffect {

    private static float rojo, verde, azul;
    private static long inicioTick = -1;
    private static int duracionTicks, fundidoTicks;

    private SkyColorEffect() {
    }

    public static void aplicar(SkyColorPacket pkt) {
        rojo = pkt.rojo() / 255f;
        verde = pkt.verde() / 255f;
        azul = pkt.azul() / 255f;
        duracionTicks = Math.max(0, pkt.duracionTicks());
        fundidoTicks = Math.max(0, pkt.fundidoTicks());
        inicioTick = duracionTicks > 0 ? tickActual() : -1;
    }

    private static long tickActual() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0;
    }

    /** 0 = sin efecto, 1 = color a pleno, con fundido de entrada/salida. */
    public static float intensidad(float partialTick) {
        if (inicioTick < 0) {
            return 0f;
        }
        double transcurrido = (tickActual() - inicioTick) + partialTick;
        if (transcurrido >= duracionTicks) {
            inicioTick = -1;
            return 0f;
        }
        if (fundidoTicks <= 0) {
            return 1f;
        }
        double restante = duracionTicks - transcurrido;
        double f = Math.min(transcurrido, Math.min(restante, fundidoTicks)) / fundidoTicks;
        return (float) Math.max(0, Math.min(1, f));
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
}
