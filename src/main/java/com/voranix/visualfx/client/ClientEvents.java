package com.voranix.visualfx.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.voranix.visualfx.VoranixVisualFX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Cinco cosas, todas opcionales (si el efecto correspondiente no está activo, no tocan nada):
 *  - onComputeFogColor: tiñe la niebla con el color del cielo global (SkyColorEffect) y, encima,
 *    con el color/densidad de las zonas de humo que toquen la posición de la CÁMARA en este
 *    frame (SmokeZoneRegistry.mezclar) - se recalcula en vivo cada frame, no hay rampa ni
 *    paquete de red de por medio para esto.
 *  - onRenderSky: domo de color del cielo global por ENCIMA de todo (ver SkyColorEffect) - un
 *    abanico de triángulos, cenit en la punta y horizonte en el borde, sin escribir profundidad
 *    (como el propio cielo de Minecraft), así que terreno, nubes, sol y luna se dibujan encima
 *    sin problema.
 *  - onRenderFog: achica la distancia de niebla (cuánto se ve) según la densidad de zona de
 *    humo en la posición de la cámara - esto SÍ hay que cancelar el evento para que tome efecto,
 *    lo dice el propio Javadoc de Forge para RenderFog. Fuerza FogShape.SPHERE para que mirar
 *    para arriba/abajo adentro de una zona se sienta igual de "tapado" que mirar al horizonte
 *    (el cilindro por defecto deja ver más lejos para arriba).
 *  - onOverlayHumo: la niebla de distancia sola no tapa lo que está MUY lejos (cielo, sol, nubes
 *    de fondo - eso no tiene "distancia" real para el fog). Esto dibuja una capa de color plana
 *    a pantalla completa, con alpha proporcional a la densidad, justo antes de que se dibuje el
 *    hotbar (RenderGuiOverlayEvent.Pre para VanillaGuiOverlay.HOTBAR) - el hotbar y todo lo que
 *    se pinta después en el HUD vanilla (vida, chat, scoreboard, lista de jugadores, barra de
 *    boss, títulos) queda arriba de esta capa sin que la toquemos, porque no cancelamos el
 *    evento: solo dibujamos antes y dejamos que el render normal siga su curso.
 *
 * OJO al compilar: RenderLevelStageEvent#getPoseStack()/getPartialTick() son los nombres
 * habituales en mods Forge 1.20.1 para esto, pero no los pude verificar contra el jar real de
 * Forge (sin red hacia maven.minecraftforge.net acá) - si el compilador no los encuentra, es
 * lo primero a revisar (puede que haya un getter con otro nombre, o que haya que sacarlos del
 * propio event de otra forma). ComputeFogColor, RenderFog (incluido setFogShape),
 * RenderGuiOverlayEvent.Pre/VanillaGuiOverlay.HOTBAR y Camera#getPosition() sí están verificados
 * contra la fuente real de Forge/vanilla (javadoc + uso real en mods 1.20.1).
 */
@Mod.EventBusSubscriber(modid = VoranixVisualFX.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final int SEGMENTOS = 24;
    private static final float RADIO_HORIZONTE = 300f;
    private static final float ALTURA_CENIT = 280f;
    /**
     * Alpha máximo (0-1) de la capa de pantalla completa, con densidad 1 (bien adentro de la
     * zona). Probado en el juego en los dos extremos: 0.7 dejaba colarse un 30% del cielo/sol de
     * fondo; 1.0 (opacidad total) tapaba todo pero dejaba la pantalla completamente plana, sin
     * ninguna variación ni siquiera cerca de la cámara - se sentía como un vacío, no como estar
     * parado adentro de humo espeso. 0.55 es el punto medio: el cielo/sol quedan bastante
     * apagados (una claridad difusa, no la forma real) pero conservando algo de variación cerca.
     */
    private static final float ALPHA_HUMO_MAXIMO = 0.55f;
    // Ojo acá: tiene que quedar CLARAMENTE por debajo de la altura de la cámara (0), no en 0 ni
    // cerca. Un anillo exactamente a la altura de los ojos queda "de canto" mirando al frente
    // (nivel) - la cámara ve ese borde del abanico de triángulos de perfil, en vez de ver la cara
    // inclinada del domo, y el degradado se nota recién mirando bastante para arriba ("muy alto").
    // Bajándolo bien por debajo, la cara inclinada queda a la vista enseguida, mirando al frente.
    private static final float ALTURA_HORIZONTE = -90f;

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

        // el humo se mezcla ENCIMA de lo anterior, no en vez de - un jugador puede estar bajo un
        // cielo de color Y dentro de una zona de humo al mismo tiempo. Se recalcula en vivo
        // contra la posición de la cámara de este frame, sin rampa ni delay de red.
        Vec3 camara = event.getCamera().getPosition();
        SmokeZoneRegistry.Mezcla zona = SmokeZoneRegistry.mezclar(camara.x, camara.y, camara.z);
        if (zona.densidad() > 0f) {
            event.setRed(lerp(event.getRed(), zona.rojo(), zona.densidad()));
            event.setGreen(lerp(event.getGreen(), zona.verde(), zona.densidad()));
            event.setBlue(lerp(event.getBlue(), zona.azul(), zona.densidad()));
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Vec3 camara = event.getCamera().getPosition();
        SmokeZoneRegistry.Mezcla zona = SmokeZoneRegistry.mezclar(camara.x, camara.y, camara.z);
        if (zona.densidad() <= 0f) {
            return;
        }
        event.setNearPlaneDistance(lerp(event.getNearPlaneDistance(), zona.cercana(), zona.densidad()));
        event.setFarPlaneDistance(lerp(event.getFarPlaneDistance(), zona.lejana(), zona.densidad()));
        // Esférico en vez del cilindro por defecto: que mirar para arriba/abajo adentro de la
        // zona se vea igual de tapado que mirar al horizonte (el Javadoc de setFogShape dice que
        // el cambio de forma también necesita el evento cancelado para tomar efecto).
        event.setFogShape(FogShape.SPHERE);
        // Forge exige cancelar el evento para que los cambios de distancia tomen efecto
        // (Javadoc de RenderFog en ViewportEvent.java) - sin esto, setear las distancias no hace nada.
        event.setCanceled(true);
    }

    /**
     * Capa de color plana a pantalla completa cuando la cámara está adentro de una zona de humo -
     * la niebla de distancia sola no alcanza a tapar lo que está MUY lejos (cielo, sol, nubes de
     * fondo), porque esas cosas no tienen una "distancia" real contra la que aplicar fog. Se
     * dibuja justo antes del hotbar (ver Javadoc de la clase) para no tapar el resto del HUD.
     */
    @SubscribeEvent
    public static void onOverlayHumo(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        Vec3 camara = mc.gameRenderer.getMainCamera().getPosition();
        SmokeZoneRegistry.Mezcla zona = SmokeZoneRegistry.mezclar(camara.x, camara.y, camara.z);
        if (zona.densidad() <= 0f) {
            return;
        }
        int alpha = Math.round(zona.densidad() * ALPHA_HUMO_MAXIMO * 255f);
        int rojo = Math.round(zona.rojo() * 255f);
        int verde = Math.round(zona.verde() * 255f);
        int azul = Math.round(zona.azul() * 255f);
        int color = (alpha << 24) | (rojo << 16) | (verde << 8) | azul;

        GuiGraphics gui = event.getGuiGraphics();
        gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), color);
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
        // Clave: el domo se ve desde ADENTRO (la cámara está rodeada por él). Si no se desactiva
        // el culling, la GPU descarta la mitad de los triángulos según el orden de los vértices
        // y el domo queda invisible mirando en ciertas direcciones (por eso se veía bien yendo
        // hacia el horizonte pero casi nada mirando para arriba).
        RenderSystem.disableCull();
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

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static float lerp(float desde, float hasta, float t) {
        return desde + (hasta - desde) * t;
    }
}
