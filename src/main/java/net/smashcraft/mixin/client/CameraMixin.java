package net.smashcraft.mixin.client;

import net.minecraft.client.Camera;
import net.smashcraft.client.SmashCraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private float xRot;
    @Shadow private float yRot;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    private final Random random = new Random();

    @Inject(method = "update", at = @At("TAIL"))
    private void applyScreenshake(net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SmashCraftClient.shakeIntensity > 0) {
            float intensity = SmashCraftClient.shakeIntensity;
            float yawOffset = (random.nextFloat() - 0.5f) * intensity * 5f;
            float pitchOffset = (random.nextFloat() - 0.5f) * intensity * 5f;
            
            this.setRotation(this.yRot + yawOffset, this.xRot + pitchOffset);
        }
    }
}
