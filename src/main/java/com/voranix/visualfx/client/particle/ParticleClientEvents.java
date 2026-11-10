package com.voranix.visualfx.client.particle;

import com.voranix.visualfx.VoranixVisualFX;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Este evento va en el bus de MODS (no el de Forge/juego, por eso el `bus = Bus.MOD` explícito),
 * y es donde se conecta el tipo de partícula ya registrado (ModParticles) con la clase que
 * realmente la dibuja (HumoGrandeParticle) y su sprite (la textura de humo_grande.json/png).
 */
@Mod.EventBusSubscriber(modid = VoranixVisualFX.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ParticleClientEvents {

    private ParticleClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.HUMO_GRANDE.get(), HumoGrandeParticle.Provider::new);
    }
}
