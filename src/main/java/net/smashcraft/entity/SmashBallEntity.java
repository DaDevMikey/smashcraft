package net.smashcraft.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.smashcraft.SmashCraft;
import net.smashcraft.SmashStateManager;

public class SmashBallEntity extends PathfinderMob {

    public SmashBallEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // floats around
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D) // 3-4 hits
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FLYING_SPEED, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
    }

    @Override
    public void tick() {
        super.tick();
        
        // Disable vanilla physics knockback entirely
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Emits glowing rainbow particles constantly to simulate a glowing orb
            double r = 0.5;
            for (int i = 0; i < 5; i++) {
                double dx = (Math.random() - 0.5) * r;
                double dy = (Math.random() - 0.5) * r;
                double dz = (Math.random() - 0.5) * r;
                
                // Rainbow color changing over time
                float time = this.tickCount + i * 10;
                float red = (float) (Math.sin(time * 0.1) * 0.5 + 0.5);
                float green = (float) (Math.sin(time * 0.1 + 2) * 0.5 + 0.5);
                float blue = (float) (Math.sin(time * 0.1 + 4) * 0.5 + 0.5);
                
                int color = ((int)(red * 255) << 16) | ((int)(green * 255) << 8) | ((int)(blue * 255));
                
                serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color, 1.5f), 
                        this.getX() + dx, this.getY() + this.getBbHeight()/2 + dy, this.getZ() + dz, 
                        1, 0, 0, 0, 0);
            }
        }
    }

    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            // Drop some particles when hit
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + this.getBbHeight()/2, this.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_HIT, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.5f);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide()) {
            if (damageSource.getEntity() instanceof Player player) {
                // Grant Final Smash
                SmashStateManager.setFinalSmashReady(player, true);
                player.sendSystemMessage(Component.literal("§d§lYou have the Final Smash! Your next hit is an instant Star KO!"));
                
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
                    this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                    
                    // Show title to all players
                    for (ServerPlayer sp : serverLevel.players()) {
                        sp.sendSystemMessage(Component.literal("§d§l" + player.getName().getString() + " got the Smash Ball!"));
                    }
                }
            }
        }
        super.die(damageSource);
    }
    
    @Override
    public boolean isInvisible() {
        return true; // The entity model is invisible, we only see the particles
    }
}
