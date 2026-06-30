package net.smashcraft.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.smashcraft.SmashStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.smashcraft.network.SmashImpactPayload;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow public abstract boolean hasEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect);


    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServerHead(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.level().isClientSide() && amount > 0 && SmashStateManager.isShielding(this)) {
            if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return;
            
            float currentShield = SmashStateManager.getShieldHealth(this);
            float shieldDamage = amount * 10f;
            
            if (currentShield - shieldDamage <= 0) {
                SmashStateManager.setShielding(this, false);
                SmashStateManager.setShieldHealth(this, 0);
                
                this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                ((LivingEntity) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100, 4));
                ((LivingEntity) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 4));
                
                if ((Object) this instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal("§cYour shield broke!"));
                }
                
                Entity attacker = source.getEntity();
                if (attacker instanceof ServerPlayer attackerPlayer) {
                    ((ServerLevel) this.level()).getServer().getCommands().performPrefixedCommand(attackerPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:shield_breaker");
                }
            } else {
                SmashStateManager.setShieldHealth(this, currentShield - shieldDamage);
                this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                
                if (amount >= 15 && (Object) this instanceof ServerPlayer serverPlayer) {
                    ((ServerLevel) this.level()).getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:invincible");
                }
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void onHurtServer(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && amount > 0 && this.isAlive()) {
            float addedPercent = amount * 3f;

            Entity attacker = source.getEntity();
            if (attacker instanceof Player playerAttacker) {
                boolean isCrit = playerAttacker.fallDistance > 0.0F 
                        && !playerAttacker.onGround() 
                        && !playerAttacker.onClimbable() 
                        && !playerAttacker.isInWater() 
                        && !playerAttacker.hasEffect(MobEffects.BLINDNESS) 
                        && !playerAttacker.isPassenger();
                
                if (isCrit) {
                    addedPercent *= 1.5f;
                    if (playerAttacker instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.literal("§6§lCRITICAL HIT!"));
                    }
                }
                
                if (playerAttacker instanceof ServerPlayer serverPlayer) {
                    ((ServerLevel) this.level()).getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:root");
                    if (SmashStateManager.getPercent(this) + addedPercent >= 300) {
                        ((ServerLevel) this.level()).getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:over_300");
                    }
                }
            }

            SmashStateManager.setLastDamage(this, amount);
            SmashStateManager.addPercent(this, addedPercent);
        }
    }

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void onKnockback(double strength, double x, double z, CallbackInfo ci) {
        float percent = SmashStateManager.getPercent(this);
        if (percent > 0 && strength > 0) {
            ci.cancel();

            this.hurtMarked = true;
            
            boolean isStarKO = false;
            
            Entity attacker = ((LivingEntity) (Object) this).getLastHurtByMob();
            if (attacker != null && SmashStateManager.isFinalSmashReady(attacker)) {
                isStarKO = true;
                SmashStateManager.setFinalSmashReady(attacker, false);
                
                if (attacker instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d§lFINAL SMASH LANDED!"));
                }
            } else if (percent > 150) {
                double chance = (percent - 150) / 100.0;
                if (Math.random() < chance) {
                    isStarKO = true;
                }
            }

            if (isStarKO && !this.level().isClientSide()) {
                // Get last damage to scale everything
                float lastDmg = SmashStateManager.getLastDamage(this);
                float dmgScale = Math.max(1.0f, lastDmg / 3.0f); // 3 dmg = baseline (wood sword)
                
                // Determine if we do a freeze frame (40% chance, longer for heavier weapons)
                boolean doFreeze = Math.random() < 0.4;
                int freezeTicks = (int) (6 + dmgScale * 3); // 6-15 ticks based on weapon
                
                // Terrain destruction - crater scales with weapon damage
                if (this.level() instanceof ServerLevel serverLevel) {
                    net.minecraft.core.BlockPos center = this.blockPosition();
                    int radius = (int) Math.min(5, 1 + dmgScale); // 1-5 block radius
                    int yRadius = (int) Math.min(3, 1 + dmgScale * 0.5); // vertical radius
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -yRadius; dy <= yRadius; dy++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                                    net.minecraft.core.BlockPos pos = center.offset(dx, dy, dz);
                                    net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(pos);
                                    if (!state.isAir() && state.getDestroySpeed(serverLevel, pos) >= 0 
                                            && state.getDestroySpeed(serverLevel, pos) < 50) {
                                        serverLevel.destroyBlock(pos, true);
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Knock the attacker back from the impact — scales with damage
                Entity hurtBy = ((LivingEntity) (Object) this).getLastHurtByMob();
                if (hurtBy != null) {
                    double dx = hurtBy.getX() - this.getX();
                    double dz = hurtBy.getZ() - this.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.01) {
                        double recoil = 1.0 + dmgScale * 0.5;
                        hurtBy.setDeltaMovement(hurtBy.getDeltaMovement().add(
                                dx / dist * recoil, 0.2 + dmgScale * 0.1, dz / dist * recoil));
                        hurtBy.hurtMarked = true;
                    }
                    if (hurtBy instanceof ServerPlayer serverPlayer) {
                        ((ServerLevel) this.level()).getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:star_ko");
                    }
                }
                
                // Particles scale with damage
                if (this.level() instanceof ServerLevel serverLevel) {
                    int particleCount = (int) (30 + dmgScale * 20);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), (int) (3 + dmgScale * 2), 0.5, 0.5, 0.5, 0.1);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, this.getX(), this.getY(0.5), this.getZ(), particleCount, 0.5, 0.5, 0.5, 0.2);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX(), this.getY(0.5), this.getZ(), (int) (15 + dmgScale * 10), 0.5, 0.5, 0.5, 0.3);
                    // Shockwave ring scales with damage
                    double ringRadius = 2.0 + dmgScale;
                    int ringParticles = (int) (30 + dmgScale * 10);
                    for (int i = 0; i < ringParticles; i++) {
                        double angle = (i / (double) ringParticles) * 2 * Math.PI;
                        double px = this.getX() + Math.cos(angle) * ringRadius;
                        double pz = this.getZ() + Math.sin(angle) * ringRadius;
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, px, this.getY() + 0.5, pz, 1, 0.0, 0.1, 0.0, 0.05);
                    }
                    // Screen shake intensity scales with damage
                    float shakeIntensity = Math.min(2.0f, 0.5f + dmgScale * 0.3f);
                    for (ServerPlayer player : serverLevel.players()) {
                        if (player.distanceToSqr(this) < 64 * 64) {
                            ServerPlayNetworking.send(player, new SmashImpactPayload(shakeIntensity, true));
                        }
                    }
                }
                
                // Sound — pitch drops for heavier weapons (sounds more impactful)
                float pitch = Math.max(0.5f, 1.2f - dmgScale * 0.1f);
                this.level().playSound(null, this.blockPosition(), net.smashcraft.SmashCraft.STAR_KO_SOUND, net.minecraft.sounds.SoundSource.PLAYERS, 3.0f, pitch);
                this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f + dmgScale * 0.3f, 0.6f + (float)(Math.random() * 0.2));
                
                // Launch speed scales with weapon damage
                double launchSpeed = 4.0 + dmgScale * 1.5;
                
                if (doFreeze) {
                    SmashStateManager.setFreezeFrames(this, freezeTicks, this.getX(), this.getY(), this.getZ());
                    this.setDeltaMovement(0, 0, 0);
                    if (attacker != null) {
                        SmashStateManager.setFreezeFrames(attacker, freezeTicks, attacker.getX(), attacker.getY(), attacker.getZ());
                    }
                    SmashStateManager.setStarKO(this, true);
                } else {
                    this.noPhysics = true;
                    this.setDeltaMovement(0, launchSpeed, 0);
                    this.hurtMarked = true;
                    this.fallDistance = 0;
                    ((LivingEntity) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 200, (int)(30 + dmgScale * 10), false, false, false));
                    SmashStateManager.setStarKO(this, true);
                }
                
                return;
            }

            // Regular knockback — also scales with weapon damage
            float lastDmg = SmashStateManager.getLastDamage(this);
            float dmgScale = Math.max(1.0f, lastDmg / 3.0f);
            double knockbackMultiplier = (1.0 + (percent / 40.0)) * (0.8 + dmgScale * 0.2);
            
            // Directional Smash attack multipliers
            double vMultiplier = 0.5; // default vertical multiplier
            double hMultiplier = 1.0; // default horizontal multiplier
            
            attacker = ((LivingEntity) (Object) this).getLastHurtByMob();
            if (attacker != null && SmashStateManager.isSmashAttackReady(attacker)) {
                knockbackMultiplier *= 3.0;
                SmashStateManager.setSmashAttackReady(attacker, false);
                
                if (net.smashcraft.SmashCraft.RULE_DIRECTIONAL_SMASH) {
                    if (attacker.getXRot() < -45) { // Looking up -> Up Smash
                        vMultiplier = 1.5;
                        hMultiplier = 0.2;
                        if (attacker instanceof ServerPlayer sp) {
                            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eUp Smash!"));
                        }
                    } else if (attacker.isCrouching()) { // Sneaking -> Down Smash
                        vMultiplier = 0.0;
                        hMultiplier = 1.8;
                        if (attacker instanceof ServerPlayer sp) {
                            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eDown Smash!"));
                        }
                    } else { // Normal -> Forward Smash
                        if (attacker instanceof ServerPlayer sp) {
                            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eForward Smash!"));
                        }
                    }
                }
                
                if (attacker instanceof ServerPlayer serverPlayer) {
                    ((ServerLevel) this.level()).getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:home_run");
                }

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, this.getX(), this.getY(0.5), this.getZ(), 50, 0.5, 0.5, 0.5, 0.2);
                    for (ServerPlayer player : serverLevel.players()) {
                        if (player.distanceToSqr(this) < 64 * 64) {
                            ServerPlayNetworking.send(player, new SmashImpactPayload(0.8f, true));
                        }
                    }
                }
            } else if (knockbackMultiplier > 2.0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, this.getX(), this.getY(0.5), this.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(this) < 64 * 64) {
                        ServerPlayNetworking.send(player, new SmashImpactPayload(0.4f, false));
                    }
                }
            }
            
            Vec3 velocity = this.getDeltaMovement();
            Vec3 newVelocity = new Vec3(velocity.x / 2.0, this.onGround() ? Math.min(0.4, velocity.y / 2.0) : velocity.y, velocity.z / 2.0);

            while (x * x + z * z < 1.0E-5F) {
                x = (Math.random() - Math.random()) * 0.01;
                z = (Math.random() - Math.random()) * 0.01;
            }
            Vec3 direction = new Vec3(x, 0.0, z).normalize().scale(strength * knockbackMultiplier * hMultiplier);

            double finalVy = this.onGround() ? Math.min(0.4, newVelocity.y + (strength * knockbackMultiplier * vMultiplier)) : newVelocity.y + (strength * knockbackMultiplier * vMultiplier);
            
            this.setDeltaMovement(
                    newVelocity.x - direction.x,
                    finalVy,
                    newVelocity.z - direction.z
            );

            if (!this.level().isClientSide()) {
                SmashStateManager.setLaunched(this, true);
            }
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void onCauseFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!this.level().isClientSide() && (SmashStateManager.isLaunched(this) || SmashStateManager.isStarKO(this))) {
            // Cancel fall damage but do NOT clear launched state here — let onGround() in tick handle that
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        if (!this.level().isClientSide() && SmashStateManager.isStarKO(this)) {
            this.noPhysics = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            if (!SmashStateManager.isShielding(this)) {
                float shield = SmashStateManager.getShieldHealth(this);
                if (shield < 100) {
                    // Update locally without network sync — sync periodically below
                    SmashStateManager.setShieldHealthLocal(this, Math.min(100, shield + 0.5f));
                }
            } else {
                float shield = SmashStateManager.getShieldHealth(this);
                // Update locally without network sync — sync periodically below
                SmashStateManager.setShieldHealthLocal(this, shield - 0.2f);
                
                // Spawn visual shield particles (reduced count for performance)
                if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
                    double radius = this.getBbWidth();
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double px = this.getX() + Math.cos(angle) * radius;
                        double pz = this.getZ() + Math.sin(angle) * radius;
                        double py = this.getY() + Math.random() * this.getBbHeight();
                        serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x3399FF, 1.5f), px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }

                if (shield <= 0) {
                    SmashStateManager.setShielding(this, false);
                    this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                    ((LivingEntity) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100, 4));
                    ((LivingEntity) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 4));
                }
            }

            // Periodic shield state sync — every 20 ticks (1 second) instead of every tick
            if (this.tickCount % 20 == 0 && (SmashStateManager.isShielding(this) || SmashStateManager.getShieldHealth(this) < 100)) {
                SmashStateManager.syncState(this);
            }

            if (this.onGround()) {
                if (SmashStateManager.isLaunched(this)) {
                    SmashStateManager.setLaunched(this, false);
                    this.fallDistance = 0;
                }
                if ((Object) this instanceof Player) {
                    net.smashcraft.SmashCraft.hasDoubleJumped.remove(this.getUUID());
                    net.smashcraft.SmashCraft.hasLedgeGrabbed.remove(this.getUUID());
                }
            }
            // Handle freeze frames
            if (SmashStateManager.isFrozen(this)) {
                // Hold entity in place during freeze
                double[] freezePos = SmashStateManager.getFreezePosition(this);
                if (freezePos != null) {
                    this.setPos(freezePos[0], freezePos[1], freezePos[2]);
                }
                this.setDeltaMovement(0, 0, 0);
                
                // Dramatic particles during freeze
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xFF4444, 2.0f),
                            this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                            5, 0.5, 0.5, 0.5, 0.0);
                }
                
                boolean freezeDone = SmashStateManager.tickFreezeFrame(this);
                if (freezeDone && SmashStateManager.isStarKO(this)) {
                    // Freeze is over — NOW launch!
                    SmashStateManager.clearFreezeFrame(this);
                    this.noPhysics = true;
                    this.setDeltaMovement(0, 5.0, 0);
                    this.hurtMarked = true;
                    this.fallDistance = 0;
                    ((LivingEntity) (Object) this).addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.LEVITATION, 200, 50, false, false, false));
                    
                    // Extra launch burst particles
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                                this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                } else if (freezeDone) {
                    // Non-Star-KO frozen entity (attacker) — just unfreeze
                    SmashStateManager.clearFreezeFrame(this);
                }
            }
            
            if (SmashStateManager.isStarKO(this) && !SmashStateManager.isFrozen(this)) {
                this.noPhysics = true;
                this.hurtMarked = true;
                this.fallDistance = 0;
                if (this.getY() > 320) {
                    this.noPhysics = false;
                    ((LivingEntity) (Object) this).removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                    ((LivingEntity) (Object) this).hurt(((ServerLevel) this.level()).damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                }
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        this.noPhysics = false;
        if (!this.level().isClientSide()) {
            // Only reset percent on void death (Star KO or falling into void)
            if (damageSource.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                    || SmashStateManager.isStarKO(this)) {
                SmashStateManager.setPercent(this, 0);
            }
            SmashStateManager.setStarKO(this, false);
            SmashStateManager.setLaunched(this, false);
        }
    }

    @Inject(method = "completeUsingItem", at = @At("RETURN"))
    private void onCompleteUsingItem(CallbackInfo ci) {
        if (!this.level().isClientSide() && (Object) this instanceof Player) {
            float percent = SmashStateManager.getPercent(this);
            if (percent > 0) {
                SmashStateManager.setPercent(this, Math.max(0, percent - 15f));
            }
        }
    }
}

