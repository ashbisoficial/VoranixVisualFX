package com.voranix.visualfx.client;

import com.voranix.visualfx.VoranixVisualFX;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mezcla el color configurado sobre la niebla (que es lo que define el tono del cielo/horizonte
 * en Minecraft vanilla) según la intensidad del efecto activo en este frame. Sin efecto activo,
 * no toca nada - se ve todo exactamente como siempre.
 */
@Mod.EventBusSubscriber(modid = VoranixVisualFX.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        // getPartialTick() de ViewportEvent devuelve double; SkyColorEffect trabaja en float.
        float intensidad = SkyColorEffect.intensidad((float) event.getPartialTick());
        if (intensidad <= 0f) {
            return;
        }
        event.setRed(lerp(event.getRed(), SkyColorEffect.rojo(), intensidad));
        event.setGreen(lerp(event.getGreen(), SkyColorEffect.verde(), intensidad));
        event.setBlue(lerp(event.getBlue(), SkyColorEffect.azul(), intensidad));
    }

    private static float lerp(float desde, float hasta, float t) {
        return desde + (hasta - desde) * t;
    }
}
