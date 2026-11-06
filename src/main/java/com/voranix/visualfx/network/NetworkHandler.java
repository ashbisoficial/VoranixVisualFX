package com.voranix.visualfx.network;

import com.voranix.visualfx.VoranixVisualFX;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * El canal se llama "voranixvisualfx:main" (modid + "main"). El plugin de Paper (VoranixExtremo,
 * repo aparte, clase VisualFxBridge) le manda bytes a ESE mismo nombre de canal con
 * Bukkit#getMessenger + Player#sendPluginMessage - no hace falta que el plugin "entienda" Forge,
 * solo arma el mismo byte a byte que SkyColorPacket#decode espera.
 */
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VoranixVisualFX.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private NetworkHandler() {
    }

    public static void register() {
        int id = 0;
        // El id es el primer byte de cada paquete: tiene que coincidir con el que arma
        // VisualFxBridge.java del lado del plugin. Si se agrega un paquete nuevo va con el
        // siguiente id (id++) - nunca cambiar el id de uno que ya existe, rompe la otra punta.
        CHANNEL.registerMessage(id++, SkyColorPacket.class, SkyColorPacket::encode, SkyColorPacket::decode, SkyColorPacket::handle);
        CHANNEL.registerMessage(id++, ZoneAddPacket.class, ZoneAddPacket::encode, ZoneAddPacket::decode, ZoneAddPacket::handle);
        CHANNEL.registerMessage(id++, ZoneRemovePacket.class, ZoneRemovePacket::encode, ZoneRemovePacket::decode, ZoneRemovePacket::handle);
        CHANNEL.registerMessage(id++, ZoneClearPacket.class, ZoneClearPacket::encode, ZoneClearPacket::decode, ZoneClearPacket::handle);
    }
}
