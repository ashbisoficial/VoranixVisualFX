package com.voranix.visualfx.network;

import com.voranix.visualfx.client.SmokeZone;
import com.voranix.visualfx.client.SmokeZoneRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Agrega (o reemplaza, si ya había una con el mismo nombre) una zona de niebla. El plugin lo
 * manda al crear una zona nueva, y de nuevo por cada zona del mundo cuando un jugador se conecta,
 * registra el canal, o cambia de mundo - ver FogZoneManager del otro repo. El cliente siempre
 * tiene el estado completo (y solo) del mundo donde está parado.
 *
 * Formato del payload (después del byte de id, ver NetworkHandler):
 *   nombre: texto (ByteUtil)
 *   1 byte  tipo (0 = caja, 1 = esfera)
 *   6 x 8 bytes (double) p0..p5 - caja: minX,minY,minZ,maxX,maxY,maxZ · esfera: cx,cy,cz,radio,-,-
 *   3 bytes color (0-255)
 *   4 bytes cercana (float)
 *   4 bytes lejana (float)
 *   4 bytes margen (float) - bloques de caída antes del borde
 *   1 byte  creciendoActivo (0/1) - sin usar todavía (Fase 3)
 *   8 bytes tickInicio (long, gameTime del mundo) - sin usar todavía
 *   4 bytes velocidadBloquesPorSegundo (float) - sin usar todavía
 *   4 bytes radioMaximo (float) - sin usar todavía
 */
public record ZoneAddPacket(String nombre, int tipo, double p0, double p1, double p2, double p3, double p4, double p5,
                             int rojo, int verde, int azul, float cercana, float lejana, float margen,
                             boolean creciendoActivo, long tickInicio, float velocidadBloquesPorSegundo, float radioMaximo) {

    public static void encode(ZoneAddPacket pkt, FriendlyByteBuf buf) {
        ByteUtil.escribirTexto(buf, pkt.nombre());
        buf.writeByte(pkt.tipo());
        buf.writeDouble(pkt.p0());
        buf.writeDouble(pkt.p1());
        buf.writeDouble(pkt.p2());
        buf.writeDouble(pkt.p3());
        buf.writeDouble(pkt.p4());
        buf.writeDouble(pkt.p5());
        buf.writeByte(pkt.rojo());
        buf.writeByte(pkt.verde());
        buf.writeByte(pkt.azul());
        buf.writeFloat(pkt.cercana());
        buf.writeFloat(pkt.lejana());
        buf.writeFloat(pkt.margen());
        buf.writeBoolean(pkt.creciendoActivo());
        buf.writeLong(pkt.tickInicio());
        buf.writeFloat(pkt.velocidadBloquesPorSegundo());
        buf.writeFloat(pkt.radioMaximo());
    }

    public static ZoneAddPacket decode(FriendlyByteBuf buf) {
        String nombre = ByteUtil.leerTexto(buf);
        int tipo = buf.readUnsignedByte();
        double p0 = buf.readDouble(), p1 = buf.readDouble(), p2 = buf.readDouble();
        double p3 = buf.readDouble(), p4 = buf.readDouble(), p5 = buf.readDouble();
        int rojo = buf.readUnsignedByte(), verde = buf.readUnsignedByte(), azul = buf.readUnsignedByte();
        float cercana = buf.readFloat(), lejana = buf.readFloat(), margen = buf.readFloat();
        boolean creciendoActivo = buf.readBoolean();
        long tickInicio = buf.readLong();
        float velocidad = buf.readFloat(), radioMaximo = buf.readFloat();
        return new ZoneAddPacket(nombre, tipo, p0, p1, p2, p3, p4, p5, rojo, verde, azul,
                cercana, lejana, margen, creciendoActivo, tickInicio, velocidad, radioMaximo);
    }

    public static void handle(ZoneAddPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            SmokeZone.Tipo tipo = pkt.tipo() == 1 ? SmokeZone.Tipo.ESFERA : SmokeZone.Tipo.CAJA;
            SmokeZoneRegistry.agregar(new SmokeZone(pkt.nombre(), tipo, pkt.p0(), pkt.p1(), pkt.p2(), pkt.p3(), pkt.p4(), pkt.p5(),
                    pkt.rojo() / 255f, pkt.verde() / 255f, pkt.azul() / 255f,
                    pkt.cercana(), pkt.lejana(), pkt.margen(),
                    pkt.creciendoActivo(), pkt.tickInicio(), pkt.velocidadBloquesPorSegundo(), pkt.radioMaximo()));
        }));
        ctx.get().setPacketHandled(true);
    }
}
