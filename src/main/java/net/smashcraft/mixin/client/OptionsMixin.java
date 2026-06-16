package net.smashcraft.mixin.client;

import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.smashcraft.client.SmashCraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.commons.lang3.ArrayUtils;

@Mixin(Options.class)
public class OptionsMixin {
    @Shadow @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(net.minecraft.client.Minecraft minecraft, java.io.File file, CallbackInfo ci) {
        if (!ArrayUtils.contains(this.keyMappings, SmashCraftClient.jumpKey)) {
            this.keyMappings = ArrayUtils.addAll(this.keyMappings, 
                SmashCraftClient.jumpKey, 
                SmashCraftClient.attackKey, 
                SmashCraftClient.shieldKey
            );
        }
    }
}
