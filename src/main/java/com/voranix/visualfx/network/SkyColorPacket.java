package com.voranix.visualfx.network;

import com.voranix.visualfx.client.SkyColorEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Lo manda el plugin de Paper cuando producción pide un color de cielo. Formato del payload
 * (después del byte de id que ya consume el SimpleChannel, ver NetworkHandler):
 *   1 byte  rojo   (0-255)
 *   1 byte  verde  (0-255)
 *   1 byte  azul   (0-255)
 *   4 bytes duracionTicks (int, big-endian) - 0 o menos = apagar ya
 *   4 bytes fundidoTicks  (int, big-endian) - cuánto tarda en aparecer/desaparecer
 * Son enteros de tamaño fijo a propósito, no VarInt: así del lado de Bukkit se arman con un
 * DataOutputStream común, sin reimplementar el VarInt de Minecraft.
 */
public record SkyColorPacket(int rojo, int verde, int azul, int duracionTicks, int fundidoTicks) {

    public static void encode(SkyColorPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.rojo());
        buf.writeByte(pkt.verde());
        buf.writeByte(pkt.azul());
        buf.writeInt(pkt.duracionTicks());
        buf.writeInt(pkt.fundidoTicks());
    }

    public static SkyColorPacket decode(FriendlyByteBuf buf) {
        return new SkyColorPacket(buf.readByte() & 0xFF, buf.readByte() & 0xFF, buf.readByte() & 0xFF,
                buf.readInt(), buf.readInt());
    }

    public static void handle(SkyColorPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkyColorEffect.aplicar(pkt)));
        ctx.get().setPacketHandled(true);
    }
}
