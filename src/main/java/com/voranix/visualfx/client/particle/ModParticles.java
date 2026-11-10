package com.voranix.visualfx.client.particle;

import com.voranix.visualfx.VoranixVisualFX;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro del tipo de partícula de humo grande (ver HumoGrandeParticle). Se registra en el
 * constructor de VoranixVisualFX - esto solo da de alta el TIPO (nombre + si ignora el límite de
 * partículas del video settings); quién la dibuja de verdad se registra aparte, del lado cliente,
 * en ParticleClientEvents.
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.Keys.PARTICLE_TYPES, VoranixVisualFX.MODID);

    /**
     * true = ignora el límite de "Partículas: reducidas/mínimas" del jugador - esto no es
     * decorativo nomás, es la única forma de ver dónde hay niebla antes de entrar, así que no
     * debería desaparecer sola porque alguien bajó ese setting por rendimiento.
     */
    public static final RegistryObject<SimpleParticleType> HUMO_GRANDE =
            REGISTER.register("humo_grande", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
