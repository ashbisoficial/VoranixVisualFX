package com.voranix.visualfx.network;

import com.voranix.visualfx.client.SkyColorEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Lo manda el plugin de Paper cuando producción pide un color de cielo. Trae DOS colores: el del
 * horizonte (lo que se ve mirando al costado - también tiñe la niebla) y el del cenit (mirando
 * para arriba). Si son el mismo color, el cielo se ve parejo; si son distintos, se arma un
 * degradado entre los dos. Formato del payload (después del byte de id, ver NetworkHandler):
 *   3 bytes horizonte: rojo, verde, azul (0-255)
 *   3 bytes cenit:     rojo, verde, azul (0-255)
 *   4 bytes duracionTicks (int, big-endian) - 0 o menos = apagar ya
 *   4 bytes fundidoTicks  (int, big-endian) - cuánto tarda en aparecer/desaparecer
 * Enteros de tamaño fijo a propósito, no VarInt: así del lado de Bukkit se arman con un
 * DataOutputStream común, sin reimplementar el VarInt de Minecraft.
 */
public record SkyColorPacket(int rojoHorizonte, int verdeHorizonte, int azulHorizonte,
                              int rojoCenit, int verdeCenit, int azulCenit,
                              int duracionTicks, int fundidoTicks) {

    public static void encode(SkyColorPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.rojoHorizonte());
        buf.writeByte(pkt.verdeHorizonte());
        buf.writeByte(pkt.azulHorizonte());
        buf.writeByte(pkt.rojoCenit());
        buf.writeByte(pkt.verdeCenit());
        buf.writeByte(pkt.azulCenit());
        buf.writeInt(pkt.duracionTicks());
        buf.writeInt(pkt.fundidoTicks());
    }

    public static SkyColorPacket decode(FriendlyByteBuf buf) {
        return new SkyColorPacket(
                buf.readByte() & 0xFF, buf.readByte() & 0xFF, buf.readByte() & 0xFF,
                buf.readByte() & 0xFF, buf.readByte() & 0xFF, buf.readByte() & 0xFF,
                buf.readInt(), buf.readInt());
    }

    public static void handle(SkyColorPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkyColorEffect.aplicar(pkt)));
        ctx.get().setPacketHandled(true);
    }
}
