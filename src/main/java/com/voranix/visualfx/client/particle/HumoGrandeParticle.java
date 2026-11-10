package com.voranix.visualfx.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Una nube de humo grande y suave (2.5-4.5 bloques, textura difusa, alpha bajo) - esto es lo que
 * hace que una SmokeZone se vea DESDE AFUERA antes de entrar, en vez de aparecer de golpe recién
 * cuando ya estás adentro. Spawneada por SmokeCloudSpawner, nunca directo.
 *
 * El tamaño exacto (quadSize) y el alpha de régimen (ALPHA_PLENO) son valores de partida sin
 * probar en el juego todavía - puede hacer falta ajustarlos, igual que pasó con el alpha de la
 * capa de pantalla completa en ClientEvents.
 */
public final class HumoGrandeParticle extends TextureSheetParticle {

    /**
     * SimpleParticleType no lleva datos propios por spawn (sería necesario un ParticleOptions
     * custom con su propio códec, que no se pudo verificar con certeza para esta versión sin
     * poder compilar). Como alternativa más simple y de bajo riesgo: SmokeCloudSpawner escribe
     * acá el color justo antes de pedir la partícula, y el constructor lo lee enseguida - ambos
     * corren en el mismo hilo (el tick del cliente), así que no hay condición de carrera real.
     */
    public static float proximoRojo = 1f;
    public static float proximoVerde = 1f;
    public static float proximoAzul = 1f;

    private static final float ALPHA_PLENO = 0.5f;
    private static final int TICKS_FADE_IN = 20;
    private static final float FRACCION_FADE_OUT = 0.25f;

    private HumoGrandeParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.rCol = proximoRojo;
        this.gCol = proximoVerde;
        this.bCol = proximoAzul;
        this.alpha = 0f;
        this.lifetime = 100 + this.random.nextInt(80);
        this.quadSize = 2.5f + this.random.nextFloat() * 2f;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.friction = 1f;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        // fade-in/fade-out suave en vez de aparecer/desaparecer de golpe - si no, con decenas
        // naciendo y muriendo todo el tiempo se nota un parpadeo constante.
        float fadeIn = Math.min(1f, (float) this.age / TICKS_FADE_IN);
        float vidaRestante = 1f - (float) this.age / this.lifetime;
        float fadeOut = Math.min(1f, vidaRestante / FRACCION_FADE_OUT);
        this.alpha = ALPHA_PLENO * Math.min(fadeIn, fadeOut);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            return new HumoGrandeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
