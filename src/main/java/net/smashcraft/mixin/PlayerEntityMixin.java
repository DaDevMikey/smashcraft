package net.smashcraft.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.smashcraft.SmashStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        // Keep noPhysics and fallDistance in sync for Star KO and launched players
        if (!this.level().isClientSide()) {
            if (SmashStateManager.isStarKO(this)) {
                this.noPhysics = true;
                this.fallDistance = 0;
            }
            if (SmashStateManager.isLaunched(this)) {
                this.fallDistance = 0;
            }

            // Ledge Grabbing Logic
            if (net.smashcraft.SmashCraft.RULE_LEDGE_GRABBING) {
                if (SmashStateManager.isLedgeGrabbing(this)) {
                    // Player is currently hanging on a ledge
                    this.setDeltaMovement(0, 0, 0);
                    this.fallDistance = 0;
                    this.hurtMarked = true;
                    
                    // If they crouch, drop down
                    if (this.isCrouching()) {
                        SmashStateManager.setLedgeGrabbing(this, false);
                    }
                } else if (!this.onGround() && this.getDeltaMovement().y < -0.1 && this.horizontalCollision && !net.smashcraft.SmashCraft.hasLedgeGrabbed.contains(this.getUUID())) {
                    // Check if we can grab a ledge
                    net.minecraft.core.Direction facing = this.getDirection();
                    net.minecraft.core.BlockPos pos = this.blockPosition();
                    net.minecraft.core.BlockPos blockInFront = pos.relative(facing);
                    net.minecraft.core.BlockPos blockAboveInFront = blockInFront.above();
                    
                    net.minecraft.world.level.block.state.BlockState stateFront = this.level().getBlockState(blockInFront);
                    net.minecraft.world.level.block.state.BlockState stateAboveFront = this.level().getBlockState(blockAboveInFront);
                    
                    if (!stateFront.getCollisionShape(this.level(), blockInFront).isEmpty() 
                            && stateAboveFront.getCollisionShape(this.level(), blockAboveInFront).isEmpty()) {
                        
                        // We found a ledge! Snap to it.
                        SmashStateManager.setLedgeGrabbing(this, true);
                        net.smashcraft.SmashCraft.hasLedgeGrabbed.add(this.getUUID());
                        net.smashcraft.SmashCraft.hasDoubleJumped.remove(this.getUUID());
                        this.setDeltaMovement(0, 0, 0);
                        this.fallDistance = 0;
                        
                        if ((Object) this instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7[Ledge Grabbed] Press Jump to climb up, or Sneak to drop."));
                        }
                    }
                }
            }

            // Random Smash Ball Spawning
            if (!this.level().isClientSide() && net.smashcraft.SmashCraft.RULE_SMASH_BALL) {
                if (Math.random() < 0.0001) { // roughly every 8.3 minutes per player
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        net.smashcraft.entity.SmashBallEntity ball = net.smashcraft.SmashCraft.SMASH_BALL.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                        if (ball != null) {
                            double dx = (Math.random() - 0.5) * 20;
                            double dy = 5 + Math.random() * 5;
                            double dz = (Math.random() - 0.5) * 20;
                            ball.setPos(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
                            serverLevel.addFreshEntity(ball);
                            
                            // Announce to player
                            if ((Object) this instanceof net.minecraft.server.level.ServerPlayer sp) {
                                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d§lA Smash Ball has appeared nearby!"));
                                serverLevel.playSound(null, ball.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 0.5f, 1.5f);
                            }
                        }
                    }
                }
            }
        }
    }
}
