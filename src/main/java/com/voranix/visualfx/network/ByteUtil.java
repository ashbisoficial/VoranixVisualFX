package com.voranix.visualfx.network;

import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Texto en los paquetes de zona: 1 byte de longitud + UTF-8 - a propósito NO el writeUtf de
 * Minecraft (ese usa VarInt para la longitud), así del lado del plugin se arma con un
 * DataOutputStream común sin reimplementar el VarInt de Minecraft. Un nombre de zona nunca
 * necesita más de 255 bytes.
 */
public final class ByteUtil {

    private ByteUtil() {
    }

    public static void escribirTexto(FriendlyByteBuf buf, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 255) {
            throw new IllegalArgumentException("Texto demasiado largo para el formato de 1 byte: " + texto);
        }
        buf.writeByte(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String leerTexto(FriendlyByteBuf buf) {
        int longitud = buf.readUnsignedByte();
        byte[] bytes = new byte[longitud];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
