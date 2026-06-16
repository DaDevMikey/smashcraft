package net.smashcraft.mixin.client;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        guiGraphicsExtractor.text(
            this.font, 
            "§6§lSMASHCRAFT ACTIVE!", 
            2, 
            2, 
            0xFFFFAA, 
            true
        );
        guiGraphicsExtractor.text(
            this.font, 
            "§eGet ready to brawl!", 
            2, 
            12, 
            0xFFFFFF, 
            true
        );
    }
}
