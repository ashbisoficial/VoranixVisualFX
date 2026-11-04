package com.voranix.visualfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.voranix.visualfx.VoranixVisualFX;
import net.minecraft.client.renderer.BufferUploader;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Dos partes del mismo efecto:
 *  - onComputeFogColor: tiñe la niebla (lo que se ve mirando hacia los costados/el horizonte).
 *  - onRenderSky: dibuja un domo de color por ENCIMA de todo (incluido mirando para arriba, que
 *    la niebla sola no cubre) - un abanico de triángulos con el color del cenit en la punta y el
 *    del horizonte en el borde, así Minecraft interpola el degradado solo entre los dos.
 * El domo se dibuja sin escribir profundidad (como el propio cielo de Minecraft), así que
 * terreno, nubes y todo lo demás se siguen dibujando encima sin problema, en cualquier orden.
 * Sin efecto activo (intensidad 0) ninguno de los dos toca nada.
 *
 * OJO al compilar: RenderLevelStageEvent#getPoseStack()/getPartialTick() son los nombres
 * habituales en mods Forge 1.20.1 para esto, pero no los pude verificar contra el jar real de
 * Forge (sin red hacia maven.minecraftforge.net acá) - si el compilador no los encuentra, es
 * lo primero a revisar (puede que haya un getter con otro nombre, o que haya que sacarlos del
 * propio event de otra forma).
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
        // getPartialTick() de ViewportEvent devuelve double; SkyColorEffect trabaja en float.
        float intensidad = SkyColorEffect.intensidad((float) event.getPartialTick());
        if (intensidad <= 0f) {
            return;
        }
        event.setRed(lerp(event.getRed(), SkyColorEffect.rojoHorizonte(), intensidad));
        event.setGreen(lerp(event.getGreen(), SkyColorEffect.verdeHorizonte(), intensidad));
        event.setBlue(lerp(event.getBlue(), SkyColorEffect.azulHorizonte(), intensidad));
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
