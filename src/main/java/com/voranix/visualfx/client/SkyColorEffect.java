package com.voranix.visualfx.client;

import com.voranix.visualfx.network.SkyColorPacket;
import net.minecraft.client.Minecraft;

/**
 * Guarda el efecto de cielo activo (si hay uno) y calcula, para el tick/frame actual, qué tan
 * "adentro" del color está (0 = sin efecto, se ve todo vanilla; 1 = color a pleno), con fundido
 * de entrada y de salida. ClientEvents lee esto en cada frame para teñir la niebla y dibujar el
 * domo del cielo.
 */
public final class SkyColorEffect {

    private static float rojoH, verdeH, azulH;
    private static float rojoC, verdeC, azulC;
    private static long inicioTick = -1;
    private static int duracionTicks, fundidoTicks;

    private SkyColorEffect() {
    }

    public static void aplicar(SkyColorPacket pkt) {
        rojoH = pkt.rojoHorizonte() / 255f;
        verdeH = pkt.verdeHorizonte() / 255f;
        azulH = pkt.azulHorizonte() / 255f;
        rojoC = pkt.rojoCenit() / 255f;
        verdeC = pkt.verdeCenit() / 255f;
        azulC = pkt.azulCenit() / 255f;
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

    public static float rojoHorizonte() {
        return rojoH;
    }

    public static float verdeHorizonte() {
        return verdeH;
    }

    public static float azulHorizonte() {
        return azulH;
    }

    public static float rojoCenit() {
        return rojoC;
    }

    public static float verdeCenit() {
        return verdeC;
    }

    public static float azulCenit() {
        return azulC;
    }
}
