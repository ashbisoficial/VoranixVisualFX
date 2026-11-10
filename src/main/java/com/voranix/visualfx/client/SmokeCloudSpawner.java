package com.voranix.visualfx.client;

import com.voranix.visualfx.VoranixVisualFX;
import com.voranix.visualfx.client.particle.HumoGrandeParticle;
import com.voranix.visualfx.client.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hace que cada SmokeZone activa se vea COMO UNA NUBE desde afuera, spawneando HumoGrandeParticle
 * a puntos al azar dentro de su volumen - sin esto, una zona es invisible hasta que ya estás dentro,
 * lo cual es un golpe brusco y no deja ver dónde hay niebla y dónde no (pedido explícito). La
 * pérdida de visión del que está DENTRO la maneja ClientEvents aparte (niebla de distancia + capa
 * de pantalla completa); esto es puramente lo que se ve desde afuera.
 */
@Mod.EventBusSubscriber(modid = VoranixVisualFX.MODID, value = Dist.CLIENT)
public final class SmokeCloudSpawner {

    // Ajustado para la vida larga de HumoGrandeParticle (300-500 ticks, ver ahí) - con el mismo
    // ritmo que antes (vida corta) se habrían acumulado muchísimas más partículas activas a la vez.
    private static final int INTERVALO_TICKS = 6;
    private static final int PARTICULAS_POR_ZONA_POR_INTERVALO = 1;
    /** No gastar partículas (ni ciclos) en zonas tan lejos de la cámara que ni se van a notar. */
    private static final double DISTANCIA_MAXIMA = 48.0;
    /** Deriva vertical lenta, como humo de verdad subiendo despacio. */
    private static final double VELOCIDAD_Y = 0.004;

    private SmokeCloudSpawner() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.level.getGameTime() % INTERVALO_TICKS != 0) {
            return;
        }
        Vec3 camara = mc.gameRenderer.getMainCamera().getPosition();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (SmokeZone zona : SmokeZoneRegistry.todas()) {
            if (distanciaAZona(zona, camara) > DISTANCIA_MAXIMA) {
                continue;
            }
            for (int i = 0; i < PARTICULAS_POR_ZONA_POR_INTERVALO; i++) {
                double[] punto = puntoAleatorioEnZona(zona, rnd);
                HumoGrandeParticle.proximoRojo = zona.rojo();
                HumoGrandeParticle.proximoVerde = zona.verde();
                HumoGrandeParticle.proximoAzul = zona.azul();
                Particle particula = mc.particleEngine.createParticle(ModParticles.HUMO_GRANDE.get(),
                        punto[0], punto[1], punto[2], 0, VELOCIDAD_Y, 0);
                if (particula != null) {
                    mc.particleEngine.add(particula);
                }
            }
        }
    }

    /** Distancia de la cámara a la SUPERFICIE de la zona (0 si está adentro) - misma idea que SmokeZone.density del otro lado. */
    private static double distanciaAZona(SmokeZone zona, Vec3 camara) {
        if (zona.tipo() == SmokeZone.Tipo.CAJA) {
            double dx = Math.max(Math.max(zona.p0() - camara.x, camara.x - zona.p3()), 0);
            double dy = Math.max(Math.max(zona.p1() - camara.y, camara.y - zona.p4()), 0);
            double dz = Math.max(Math.max(zona.p2() - camara.z, camara.z - zona.p5()), 0);
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        double dx = camara.x - zona.p0(), dy = camara.y - zona.p1(), dz = camara.z - zona.p2();
        return Math.max(0, Math.sqrt(dx * dx + dy * dy + dz * dz) - zona.p3());
    }

    /** Punto uniforme dentro del volumen (caja o esfera) - mismo muestreo que tenía el plugin antes de sacarle las partículas vanilla. */
    private static double[] puntoAleatorioEnZona(SmokeZone zona, ThreadLocalRandom rnd) {
        if (zona.tipo() == SmokeZone.Tipo.CAJA) {
            double x = zona.p0() + rnd.nextDouble() * (zona.p3() - zona.p0());
            double y = zona.p1() + rnd.nextDouble() * (zona.p4() - zona.p1());
            double z = zona.p2() + rnd.nextDouble() * (zona.p5() - zona.p2());
            return new double[]{x, y, z};
        }
        double radio = zona.p3() * Math.cbrt(rnd.nextDouble());
        double theta = rnd.nextDouble() * Math.PI * 2;
        double phi = Math.acos(2 * rnd.nextDouble() - 1);
        double x = zona.p0() + radio * Math.sin(phi) * Math.cos(theta);
        double y = zona.p1() + radio * Math.sin(phi) * Math.sin(theta);
        double z = zona.p2() + radio * Math.cos(phi);
        return new double[]{x, y, z};
    }
}
