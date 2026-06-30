package net.smashcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.smashcraft.entity.SmashBallEntity;

/**
 * An invisible renderer for SmashBallEntity.
 * The Smash Ball is visually represented by server-spawned particles,
 * so no model rendering is needed. This renderer exists solely to
 * prevent the client from crashing with a NullPointerException when
 * the entity is synced to the client.
 */
public class SmashBallRenderer extends EntityRenderer<SmashBallEntity, EntityRenderState> {

    public SmashBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // Intentionally empty — the Smash Ball is visualized via server-side particles only.
    }
}
