package com.voranix.visualfx.network;

import com.voranix.visualfx.client.SmokeZoneRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sin datos: "borrá todas las zonas que tengas". Se manda antes de reenviar las del mundo nuevo
 * (al conectarse, registrar el canal, o cambiar de mundo) para no arrastrar zonas de otro mundo.
 */
public record ZoneClearPacket() {

    public static void encode(ZoneClearPacket pkt, FriendlyByteBuf buf) {
        // sin payload
    }

    public static ZoneClearPacket decode(FriendlyByteBuf buf) {
        return new ZoneClearPacket();
    }

    public static void handle(ZoneClearPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SmokeZoneRegistry::limpiar));
        ctx.get().setPacketHandled(true);
    }
}
