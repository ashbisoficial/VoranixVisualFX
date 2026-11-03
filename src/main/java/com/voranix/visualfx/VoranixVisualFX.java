package com.voranix.visualfx;

import com.voranix.visualfx.network.NetworkHandler;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;

/**
 * Mod puramente visual: no agrega bloques, items ni mecánicas. Solo escucha un canal de red
 * ("voranixvisualfx:main") que le manda el plugin de Paper VoranixExtremo cuando producción
 * quiere teñir el cielo/la niebla de un color por un rato. Ver network/ y client/.
 */
@Mod(VoranixVisualFX.MODID)
public class VoranixVisualFX {

    public static final String MODID = "voranixvisualfx";

    public VoranixVisualFX() {
        // Es solo visual: que un server (o un jugador) no lo tenga instalado no lo marca
        // como "incompatible" en la pantalla de multijugador.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (incoming, isNetwork) -> true));

        NetworkHandler.register();
    }
}
