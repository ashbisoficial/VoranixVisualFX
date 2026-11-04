package com.voranix.visualfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.voranix.visualfx.VoranixVisualFX;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Cuatro cosas, todas opcionales (si el efecto correspondiente no está activo, no tocan nada):
 *  - onComputeFogColor: tiñe la niebla con el color del cielo global (SkyColorEffect) y, encima,
 *    con el color de la zona de niebla local (FogZoneEffect) si el jugador está adentro de una.
 *  - onRenderSky: domo de color del cielo global por ENCIMA de todo (ver SkyColorEffect) - un
 *    abanico de triángulos, cenit en la punta y horizonte en el borde, sin escribir profundidad
 *    (como el propio cielo de Minecraft), así que terreno, nubes, sol y luna se dibujan encima
 *    sin problema.
 *  - onRenderFog: achica la distancia de niebla (cuánto se ve) cuando el jugador está dentro de
 *    una zona de niebla local - esto SÍ hay que cancelar el evento para que tome efecto, lo dice
 *    el propio Javadoc de Forge para RenderFog.
 *
 * OJO al compilar: RenderLevelStageEvent#getPoseStack()/getPartialTick() son los nombres
 * habituales en mods Forge 1.20.1 para esto, pero no los pude verificar contra el jar real de
 * Forge (sin red hacia maven.minecraftforge.net acá) - si el compilador no los encuentra, es
 * lo primero a revisar (puede que haya un getter con otro nombre, o que haya que sacarlos del
 * propio event de otra forma). ComputeFogColor y RenderFog sí están verificados contra la
 * fuente real de Forge (ViewportEvent.java).
 */
@Mod.EventBusSubscriber(modid = VoranixVisualFX.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final int SEGMENTOS = 24;
    private static final float RADIO_HORIZONTE = 400f;
    private static final float ALTURA_CENIT = 400f;
    private static final float ALTURA_HORIZONTE = 0f;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        // getPartialTick() de ViewportEvent devuelve double; los efectos trabajan en float.
        float partialTick = (float) event.getPartialTick();

        float intensidadCielo = SkyColorEffect.intensidad(partialTick);
        if (intensidadCielo > 0f) {
            event.setRed(lerp(event.getRed(), SkyColorEffect.rojoHorizonte(), intensidadCielo));
            event.setGreen(lerp(event.getGreen(), SkyColorEffect.verdeHorizonte(), intensidadCielo));
            event.setBlue(lerp(event.getBlue(), SkyColorEffect.azulHorizonte(), intensidadCielo));
        }

        // la niebla de zona se mezcla ENCIMA de lo anterior, no en vez de - así un jugador puede
        // estar bajo un cielo de color Y dentro de una zona de niebla al mismo tiempo.
        float intensidadZona = FogZoneEffect.intensidad(partialTick);
        if (intensidadZona > 0f) {
            event.setRed(lerp(event.getRed(), FogZoneEffect.rojo(), intensidadZona));
            event.setGreen(lerp(event.getGreen(), FogZoneEffect.verde(), intensidadZona));
            event.setBlue(lerp(event.getBlue(), FogZoneEffect.azul(), intensidadZona));
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float intensidad = FogZoneEffect.intensidad((float) event.getPartialTick());
        if (intensidad <= 0f) {
            return;
        }
        event.setNearPlaneDistance(lerp(event.getNearPlaneDistance(), FogZoneEffect.cercana(), intensidad));
        event.setFarPlaneDistance(lerp(event.getFarPlaneDistance(), FogZoneEffect.lejana(), intensidad));
        // Forge exige cancelar el evento para que los cambios de distancia tomen efecto
        // (Javadoc de RenderFog en ViewportEvent.java) - sin esto, setear las distancias no hace nada.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        float intensidad = SkyColorEffect.intensidad(event.getPartialTick());
        if (intensidad <= 0f) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Matrix4f matriz = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matriz, 0f, ALTURA_CENIT, 0f)
                .color(SkyColorEffect.rojoCenit(), SkyColorEffect.verdeCenit(), SkyColorEffect.azulCenit(), intensidad)
                .endVertex();
        for (int i = 0; i <= SEGMENTOS; i++) {
            float angulo = (float) (i * 2 * Math.PI / SEGMENTOS);
            float x = Mth.sin(angulo) * RADIO_HORIZONTE;
            float z = Mth.cos(angulo) * RADIO_HORIZONTE;
            buffer.vertex(matriz, x, ALTURA_HORIZONTE, z)
                    .color(SkyColorEffect.rojoHorizonte(), SkyColorEffect.verdeHorizonte(), SkyColorEffect.azulHorizonte(), intensidad)
                    .endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static float lerp(float desde, float hasta, float t) {
        return desde + (hasta - desde) * t;
    }
}
