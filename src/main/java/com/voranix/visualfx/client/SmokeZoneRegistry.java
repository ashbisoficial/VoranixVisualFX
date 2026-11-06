package com.voranix.visualfx.client;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * Qué zonas de niebla hay activas AHORA en el mundo donde está el jugador. El plugin ya se
 * encarga de mandar solo las del mundo correcto (limpia y reenvía al cambiar de mundo - ver
 * FogZoneManager del otro repo), así que acá no hace falta filtrar nada más: lo que esté en este
 * mapa es, por definición, del mundo actual.
 */
public final class SmokeZoneRegistry {

    /** Lo que ComputeFogColor/RenderFog necesitan ya resuelto: densidad (0-1), color mezclado, y near/far de la zona dominante. */
    public record Mezcla(float densidad, float rojo, float verde, float azul, float cercana, float lejana) {
        public static final Mezcla VACIA = new Mezcla(0f, 0f, 0f, 0f, 0f, 0f);
    }

    private static final Map<String, SmokeZone> zonas = new HashMap<>();

    private SmokeZoneRegistry() {
    }

    public static void agregar(SmokeZone zona) {
        zonas.put(zona.nombre(), zona);
    }

    public static void quitar(String nombre) {
        zonas.remove(nombre);
    }

    public static void limpiar() {
        zonas.clear();
    }

    /**
     * Densidad = la más alta entre todas las zonas que tocan este punto (no se suman: estar en el
     * solapamiento de dos no deja "más ciego" que la más fuerte de las dos sola). Color = mezcla
     * ponderada por densidad de cada zona que esté aportando (una transición entre dos zonas de
     * distinto color superpuestas se ve gradual, no un salto brusco). near/far = los de la zona
     * dominante (la de mayor densidad en este punto), no tiene mucho sentido promediarlos.
     */
    public static Mezcla mezclar(double x, double y, double z) {
        if (zonas.isEmpty()) {
            return Mezcla.VACIA;
        }
        long tickActual = tickActual();
        float densidadMaxima = 0f;
        float cercanaDominante = 0f, lejanaDominante = 0f;
        float sumaR = 0f, sumaG = 0f, sumaB = 0f, sumaPesos = 0f;
        for (SmokeZone zona : zonas.values()) {
            float densidad = zona.density(x, y, z, tickActual);
            if (densidad <= 0f) {
                continue;
            }
            sumaR += zona.rojo() * densidad;
            sumaG += zona.verde() * densidad;
            sumaB += zona.azul() * densidad;
            sumaPesos += densidad;
            if (densidad > densidadMaxima) {
                densidadMaxima = densidad;
                cercanaDominante = zona.cercana();
                lejanaDominante = zona.lejana();
            }
        }
        if (sumaPesos <= 0f) {
            return Mezcla.VACIA;
        }
        return new Mezcla(densidadMaxima, sumaR / sumaPesos, sumaG / sumaPesos, sumaB / sumaPesos, cercanaDominante, lejanaDominante);
    }

    private static long tickActual() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0;
    }
}
