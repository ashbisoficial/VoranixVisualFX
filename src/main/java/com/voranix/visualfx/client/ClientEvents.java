package com.voranix.visualfx.client;

import com.voranix.visualfx.VoranixVisualFX;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mezcla el color configurado sobre la niebla (que es lo que define el tono del cielo/horizonte
 * en Minecraft vanilla) según la intensidad del efecto activo en este frame. Sin efecto activo,
 * no toca nada - se ve todo exactamente como siempre.
 *
 * OJO al compilar: el nombre exacto de EntityViewRenderEvent.FogColors#getPartialTick() puede
 * variar entre builds de Forge 1.20.1 (en algunas se llama getRenderPartialTicks()). Si tira
 * error de "cannot find symbol" en esa línea, es lo primero a revisar - no pude verificar esto
 * contra el jar real de Forge en este entorno (sin red hacia maven.minecraftforge.net acá).
 */
@Mod.EventBusSubscriber(modid = VoranixVisualFX.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        float intensidad = SkyColorEffect.intensidad(event.getPartialTick());
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
