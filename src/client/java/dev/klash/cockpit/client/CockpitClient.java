package dev.klash.cockpit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CockpitClient implements ClientModInitializer {

    private static final List<Float> snaps = List.of(36f, -36f);
    private static final float SNAP_RANGE = 4f;
    private static final float SNAP_STRENGTH = 0.35f;

    @Override
    public void onInitializeClient() {
        CockpitConfig.init();
        CockpitKeybinds.init();
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.of("cockpit", "pitch_guides"),
                CockpitClient::onHudRender
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            currentTick++;

            if(client.player == null || !client.player.isGliding()) return;
            if(CockpitConfig.enableFireworkTimer) {
                for (FireworkRocketEntity rocket :
                        client.world.getEntitiesByClass(
                                FireworkRocketEntity.class,
                                client.player.getBoundingBox().expand(5.0),
                                r -> true)) {

                    if (rocket.getOwner() == client.player) {

                        if (!trackedRocketIds.contains(rocket.getId())) {
                            trackedRocketIds.add(rocket.getId());
                            lastRocketTick = currentTick;
                        }
                    }
                }
            }

            if(CockpitConfig.enableSpeedGraph) {
                Vec3d vel = client.player.getVelocity();
                double horizontal = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                last50Speeds.add((int)(horizontal * 20.0));
                if (last50Speeds.size() > 50) last50Speeds.remove(last50Speeds.get(0));
            }

            if(CockpitConfig.enableSnapPitch) {
                for(float SNAP_TARGET : snaps) {
                    float pitch = client.player.getPitch();
                    float diff = SNAP_TARGET - pitch;

                    if (Math.abs(diff) < SNAP_RANGE) {
                        client.player.setPitch(pitch + diff * SNAP_STRENGTH);
                    }
                }
            }
        });
    }

    static Set<Integer> trackedRocketIds = new HashSet<>();
    static int lastRocketTick = 0;
    static int currentTick = 0;
    static List<Integer> last50Speeds = new ArrayList<>();

    private static void onHudRender(DrawContext drawContext, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) return;

        if (!client.player.isGliding()) return;

        if(CockpitConfig.enableInfo)
            renderSpeedOverlay(drawContext, client);
        if(CockpitConfig.enableSpeedGraph)
            renderSpeedGraph(drawContext, client);
        if(CockpitConfig.enableFireworkTimer)
            renderFireworkOverlay(drawContext, client);
        if(CockpitConfig.enablePitchGuides)
            renderPitchLines(drawContext, client);
        if(CockpitConfig.enableWarnings)
            renderWarnings(drawContext, client);
        if(CockpitConfig.enablePlayerRender)
            renderPlayerModel(drawContext, client);
    }

    private static void renderWarnings(DrawContext context, MinecraftClient client) {
        float currentPitch = client.player.getPitch();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        if (currentPitch < -50) {
            String text = "STALL";
            int color = 0xFFFFFF00;
            context.drawTextWithShadow(client.textRenderer, text, width / 2 - client.textRenderer.getWidth(text) / 2, height / 2 - 20, color);
        }
        if (currentPitch > 50) {
            String text = "DIVE";
            int color = 0xFFFF0000;
            context.drawTextWithShadow(client.textRenderer, text, width / 2 - client.textRenderer.getWidth(text) / 2, height / 2 + 20, color);
        }
    }

    private static void renderSpeedGraph(DrawContext context, MinecraftClient client) {
        int lines = 50;
        int posX = client.getWindow().getScaledWidth() - lines*2 - 10;
        int posY = 30;

//        context.drawVerticalLine(posX, posY, posY + 50, 0x80FFFFFF);
        context.drawHorizontalLine(posX, posX + lines*2, posY + 50, 0x80FFFFFF);
        int i = 1;
        for (int speed : last50Speeds) {
            int color;
            if (speed <= 10) {
                // From red to yellow
                float t = speed / 10f; // 0 to 1
                color = (0xFF << 24) | ((int)((1-t)*255 + t*255) << 16) | ((int)(t*255) << 8) | 0;
            } else if (speed <= 20) {
                // From yellow to green
                float t = (speed - 10) / 10f; // 0 to 1
                color = (0xFF << 24) | ((int)((1-t)*255) << 16) | 255 << 8 | 0;
            } else {
                color = 0xFF00FF00; // max green
            }
            context.fill(posX + i * 2, posY + 49 - speed, posX + i * 2 + 1, posY + 50, color);
            i++;
            if (i >= lines) break;
            context.fill(posX + i * 2, posY + 49, posX + i * 2 + 1, posY + 49 + 1, 0x80FFFFFF);
        }
    }

    private static void renderFireworkOverlay(DrawContext context, MinecraftClient client) {
        Vec3d vel = client.player.getVelocity();
        double horizontal = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        int width = client.getWindow().getScaledWidth();

        // for now just say seconds since
        int ticksSince = currentTick - lastRocketTick;
        double seconds = ticksSince / 20.0;
        String text = seconds > 5 ? "5s" : String.format("%.1fs", seconds);
        int textWidth = client.textRenderer.getWidth(text);
        context.drawItem(new ItemStack(Items.FIREWORK_ROCKET, 1), width-16, 10);
        int color = 0xFFFFFFFF;
        if(seconds < 5) color = 0xFF00FF00;
        if(seconds < 2.25) color = 0xFFFFFF00;
        if(seconds < 1.5) color = 0xFFFF0000;
        context.drawTextWithShadow(client.textRenderer, text, width-textWidth-16, 16, color);
    }

    private static void renderPlayerModel(DrawContext context, MinecraftClient client) {
        double mouseX = client.mouse.getX();
        double mouseY = client.mouse.getY();
        InventoryScreen.drawEntity(context, 0, -20, 40, 40, 10, 1f, (float)mouseX, (float)mouseY, client.player);
    }

    private static void renderSpeedOverlay(DrawContext context, MinecraftClient client) {
        Vec3d vel = client.player.getVelocity();
        double horizontal = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        float currentPitch = client.player.getPitch();
        int centerX = width / 2;
        int centerY = height / 2;

        TextRenderer r = client.textRenderer;
        String pitch = String.format("%.1f", currentPitch);
        String speed = String.format("%.1f", horizontal);
        int pitchWidth = r.getWidth(pitch + "deg");
        int speedWidth = r.getWidth(speed + " m/s");
        context.drawTextWithShadow(r, pitch + "deg", centerX-pitchWidth-2, 20, 0xFFFFFFFF);
        context.drawTextWithShadow(r, speed + " m/s", centerX+2, 20, 0xFFFFFFFF);
    }

    private static void renderPitchLines(DrawContext context, MinecraftClient client) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        float currentPitch = client.player.getPitch();

        float fov = (float) client.options.getFov().getValue();

        TextRenderer r = client.textRenderer;

        String pitch = String.format("%.1f", currentPitch);

        int centerX = width / 2;
        int centerY = height / 2;

        drawPitchLine(context, centerX, centerY, 80, height, currentPitch, 36f, 4f, fov, r);
        drawPitchLine(context, centerX, centerY, 80, height, currentPitch, -36f, 4f, fov, r);

    }

    private static void drawPitchLine(
            DrawContext context,
            int centerX,
            int centerY,
            int width,
            int height,
            float currentPitch,
            float targetPitch,
            float range,
            float fov,
            TextRenderer r
    ) {
        float pitchDiff = targetPitch - currentPitch;

        // Convert pitch difference to screen space
        float pixelsPerDegree = height / fov;
        float yOffset = pitchDiff * pixelsPerDegree;

        int lineY = (int)(centerY + yOffset);

        if (lineY < 0 || lineY > height) return;

        int color = 0x80FFFFFF;
        if ((Math.abs(currentPitch - targetPitch)) < 1.5f)
            color = 0x8000FF00;

        int offset = -10;
        context.drawTextWithShadow(r, "" + targetPitch, centerX-36, lineY + offset, color);

        context.fill(centerX-(width/2), lineY, centerX+(width/2), lineY + 1, color);
        // vertical lines on either side of this central line to show the range
        context.fill(centerX-(width/2)-1, lineY+(int)range, centerX-(width/2), lineY-(int)range, color);
        context.fill(centerX+(width/2)-1, lineY+(int)range, centerX+(width/2), lineY-(int)range, color);
        context.fill(centerX-1, lineY+3, centerX, lineY-2, color);
    }
}
