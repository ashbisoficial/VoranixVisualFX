package com.voranix.visualfx.client;

/**
 * Una zona de niebla en el cliente - caja y esfera son la misma clase, con un campo de tipo que
 * dice cómo interpretar p0..p5. No guarda a qué mundo pertenece: el plugin solo manda (y limpia/
 * reenvía al cambiar de mundo) las zonas del mundo donde está parado cada jugador, así que si una
 * zona está acá es porque es la de este mundo - no hace falta filtrar de nuevo del lado del cliente.
 */
public record SmokeZone(String nombre, Tipo tipo, double p0, double p1, double p2, double p3, double p4, double p5,
                         float rojo, float verde, float azul, float cercana, float lejana, float margen,
                         boolean creciendoActivo, long tickInicio, float velocidadBloquesPorSegundo, float radioMaximo) {

    /** CAJA: p0,p1,p2 = esquina mínima; p3,p4,p5 = esquina máxima. ESFERA: p0,p1,p2 = centro; p3 = radio (p4,p5 sin usar). */
    public enum Tipo {CAJA, ESFERA}

    /** 0 = fuera de alcance, 1 = adentro a pleno, con caída lineal en los últimos "margen" bloques. */
    public float density(double x, double y, double z, long tickActual) {
        double distancia = tipo == Tipo.CAJA ? distanciaCaja(x, y, z) : distanciaEsfera(x, y, z, tickActual);
        if (distancia <= 0) {
            return 1f;
        }
        if (distancia >= margen) {
            return 0f;
        }
        return (float) (1.0 - distancia / margen);
    }

    private double distanciaCaja(double x, double y, double z) {
        double dx = Math.max(Math.max(p0 - x, x - p3), 0);
        double dy = Math.max(Math.max(p1 - y, y - p4), 0);
        double dz = Math.max(Math.max(p2 - z, z - p5), 0);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double distanciaEsfera(double x, double y, double z, long tickActual) {
        // p3 = radio fijo; si algún día creciendoActivo llega en true (Fase 3), el radio efectivo
        // se calcula a partir del tiempo transcurrido en vez de usar p3 directo.
        double radioEfectivo = p3;
        if (creciendoActivo) {
            double transcurridoSegundos = Math.max(0, (tickActual - tickInicio) / 20.0);
            radioEfectivo = Math.min(radioMaximo, transcurridoSegundos * velocidadBloquesPorSegundo);
        }
        double dx = x - p0, dy = y - p1, dz = z - p2;
        return Math.max(0, Math.sqrt(dx * dx + dy * dy + dz * dz) - radioEfectivo);
    }
}
