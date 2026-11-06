package com.voranix.visualfx.network;

import com.voranix.visualfx.client.SmokeZoneRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Saca una zona por nombre (al borrarla con /fx niebla borrar). Payload: nombre (ByteUtil). */
public record ZoneRemovePacket(String nombre) {

    public static void encode(ZoneRemovePacket pkt, FriendlyByteBuf buf) {
        ByteUtil.escribirTexto(buf, pkt.nombre());
    }

    public static ZoneRemovePacket decode(FriendlyByteBuf buf) {
        return new ZoneRemovePacket(ByteUtil.leerTexto(buf));
    }

    public static void handle(ZoneRemovePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SmokeZoneRegistry.quitar(pkt.nombre())));
        ctx.get().setPacketHandled(true);
    }
}
