package net.smashcraft.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.smashcraft.client.SmashCraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        float percent = SmashCraftClient.clientPercentages.getOrDefault(client.player.getId(), 0f);

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        if (SmashCraftClient.impactFrameTicks > 0) {
            float alpha = SmashCraftClient.impactFrameTicks / 3f;
            int a = (int) (alpha * 120);
            guiGraphicsExtractor.fill(0, 0, width, height, (a << 24) | 0xFF0000);
        }

        int barWidth = 100;
        int barHeight = 10;
        int x = (width - barWidth) / 2;
        int y = height - 50;

        int fillWidth = (int) Math.min(barWidth, (percent / 300f) * barWidth);
        
        int color = 0xFFFFFF;
        if (percent > 0) {
            float hue = Math.max(0, 120 - (percent / 300f) * 120);
            // Manual HSB to RGB (avoids java.awt which conflicts with LWJGL/fullscreen)
            float h = hue / 60f;
            int hi = (int) h % 6;
            float f = h - hi;
            int v = 255;
            int q = (int) (255 * (1 - f));
            int t = (int) (255 * f);
            color = switch (hi) {
                case 0 -> (v << 16) | (t << 8);
                case 1 -> (q << 16) | (v << 8);
                case 2 -> (v << 8) | t;
                case 3 -> (v << 8) | (q << 16) | v;
                case 4 -> (t << 16) | v;
                default -> (v << 16) | q;
            };
        }

        guiGraphicsExtractor.fill(x, y, x + barWidth, y + barHeight, 0x80000000);
        guiGraphicsExtractor.fill(x, y, x + fillWidth, y + barHeight, color | 0xFF000000);

        String text = String.format("%.1f%%", percent);
        guiGraphicsExtractor.centeredText(client.font, text, x + barWidth / 2, y + 1, 0xFFFFFF);
        
        boolean isShielding = SmashCraftClient.clientShielding.getOrDefault(client.player.getId(), false);
        float shieldHealth = SmashCraftClient.clientShieldHealth.getOrDefault(client.player.getId(), 0f);
        boolean isSmashReady = SmashCraftClient.clientSmashReady.getOrDefault(client.player.getId(), false);
        
        if (isShielding && shieldHealth > 0) {
            int shieldBarWidth = 80;
            int shieldBarHeight = 4;
            int sx = (width - shieldBarWidth) / 2;
            int sy = y - 10;
            int sFillWidth = (int) Math.min(shieldBarWidth, (shieldHealth / 100f) * shieldBarWidth);
            guiGraphicsExtractor.fill(sx, sy, sx + shieldBarWidth, sy + shieldBarHeight, 0x80000000);
            guiGraphicsExtractor.fill(sx, sy, sx + sFillWidth, sy + shieldBarHeight, 0xFF00AAFF);
        }
        
        if (isSmashReady) {
            int time = (int) (System.currentTimeMillis() % 1000);
            int smashColor = time > 500 ? 0xFFFF55 : 0xFF5555;
            guiGraphicsExtractor.centeredText(client.font, "§lSMASH READY", x + barWidth / 2, y + barHeight + 5, smashColor);
        }
    }
}
