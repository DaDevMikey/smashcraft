package net.smashcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.smashcraft.SmashCraft;
import net.smashcraft.client.renderer.SmashBallRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.smashcraft.network.SyncSmashPercentPayload;
import net.smashcraft.network.SmashImpactPayload;
import net.smashcraft.network.SmashActionPayload;
import net.smashcraft.network.SyncSmashStatePayload;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class SmashCraftClient implements ClientModInitializer {

    public static final Map<Integer, Float> clientPercentages = new HashMap<>();
    public static final Map<Integer, Boolean> clientStarKO = new HashMap<>();
    public static final Map<Integer, Boolean> clientShielding = new HashMap<>();
    public static final Map<Integer, Float> clientShieldHealth = new HashMap<>();
    public static final Map<Integer, Boolean> clientSmashReady = new HashMap<>();
    
    public static float shakeIntensity = 0f;
    public static int impactFrameTicks = 0;

    public static KeyMapping jumpKey;
    public static KeyMapping attackKey;
    public static KeyMapping shieldKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SmashCraft.SMASH_BALL, SmashBallRenderer::new);
        jumpKey = new KeyMapping(
                "key.smashcraft.jump",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                net.minecraft.client.KeyMapping.Category.GAMEPLAY
        );
        attackKey = new KeyMapping(
                "key.smashcraft.attack",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                net.minecraft.client.KeyMapping.Category.GAMEPLAY
        );
        shieldKey = new KeyMapping(
                "key.smashcraft.shield",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                net.minecraft.client.KeyMapping.Category.GAMEPLAY
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shakeIntensity > 0) {
                shakeIntensity -= 0.1f;
                if (shakeIntensity < 0) shakeIntensity = 0;
            }
            if (impactFrameTicks > 0) {
                impactFrameTicks--;
            }

            while (jumpKey.consumeClick()) {
                ClientPlayNetworking.send(new SmashActionPayload("jump"));
            }
            while (attackKey.consumeClick()) {
                ClientPlayNetworking.send(new SmashActionPayload("attack"));
            }
            while (shieldKey.consumeClick()) {
                ClientPlayNetworking.send(new SmashActionPayload("shield"));
            }

            if (client.player != null) {
                if (clientStarKO.getOrDefault(client.player.getId(), false)) {
                    client.player.noPhysics = true;
                    client.player.setDeltaMovement(0, 5.0, 0);
                    client.player.fallDistance = 0;
                } else {
                    // Only reset noPhysics if we were in Star KO before
                    // (don't interfere with spectator mode etc)
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncSmashPercentPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                clientPercentages.put(payload.entityId(), payload.percent());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SmashImpactPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                shakeIntensity = payload.intensity();
                if (payload.doImpactFrame()) {
                    impactFrameTicks = 3; // 3 ticks of impact frame
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncSmashStatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                clientStarKO.put(payload.entityId(), payload.isStarKO());
                clientShielding.put(payload.entityId(), payload.isShielding());
                clientShieldHealth.put(payload.entityId(), payload.shieldHealth());
                clientSmashReady.put(payload.entityId(), payload.isSmashAttackReady());
                
                if (context.client().level != null) {
                    net.minecraft.world.entity.Entity entity = context.client().level.getEntity(payload.entityId());
                    if (entity != null) {
                        entity.noPhysics = payload.isStarKO();
                    }
                }
            });
        });
    }
}
