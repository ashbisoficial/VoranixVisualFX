package com.voranix.visualfx.network;

import com.voranix.visualfx.client.FogZoneEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Niebla de verdad (reduce cuánto se ve, no solo tiñe el color) para cuando un jugador está
 * DENTRO de una zona específica del mapa (el plugin es quien decide quién está adentro y le
 * manda esto - ver FogZoneManager.java del otro repo). A diferencia de SkyColorPacket, esto no
 * tiene "duración": queda activo hasta que llega otro paquete (con lejana=0 para apagarlo, al
 * salir de la zona), y "fundido" es cuánto tarda en entrar/salir de la niebla, no cuánto dura.
 * Formato del payload (después del byte de id, ver NetworkHandler):
 *   3 bytes color: rojo, verde, azul (0-255)
 *   4 bytes cercana (float, big-endian) - distancia en bloques donde arranca la niebla
 *   4 bytes lejana  (float, big-endian) - distancia en bloques donde ya no se ve nada; 0 = apagar
 *   4 bytes fundidoTicks (int, big-endian)
 */
public record FogZonePacket(int rojo, int verde, int azul, float cercana, float lejana, int fundidoTicks) {

    public static void encode(FogZonePacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.rojo());
        buf.writeByte(pkt.verde());
        buf.writeByte(pkt.azul());
        buf.writeFloat(pkt.cercana());
        buf.writeFloat(pkt.lejana());
        buf.writeInt(pkt.fundidoTicks());
    }

    public static FogZonePacket decode(FriendlyByteBuf buf) {
        return new FogZonePacket(buf.readByte() & 0xFF, buf.readByte() & 0xFF, buf.readByte() & 0xFF,
                buf.readFloat(), buf.readFloat(), buf.readInt());
    }

    public static void handle(FogZonePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FogZoneEffect.aplicar(pkt)));
        ctx.get().setPacketHandled(true);
    }
}
