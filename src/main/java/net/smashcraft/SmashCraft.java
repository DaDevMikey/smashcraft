package net.smashcraft;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.DisplaySlot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.smashcraft.network.SyncSmashPercentPayload;
import net.smashcraft.network.SmashImpactPayload;
import net.smashcraft.network.SmashActionPayload;
import net.smashcraft.network.SyncSmashStatePayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

// Removed GameRule imports

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.smashcraft.entity.SmashBallEntity;

public class SmashCraft implements ModInitializer {

    public static final Identifier STAR_KO_ID = Identifier.fromNamespaceAndPath("smashcraft", "star_ko");
    public static final SoundEvent STAR_KO_SOUND = SoundEvent.createVariableRangeEvent(STAR_KO_ID);

    public static final Set<UUID> hasDoubleJumped = new HashSet<>();
    public static final Set<UUID> hasLedgeGrabbed = new HashSet<>();

    public static final EntityType<SmashBallEntity> SMASH_BALL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("smashcraft", "smash_ball"),
            EntityType.Builder.of((EntityType.EntityFactory<SmashBallEntity>) SmashBallEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("smashcraft", "smash_ball")))
    );

    public static boolean RULE_LEDGE_GRABBING = true;
    public static boolean RULE_DIRECTIONAL_SMASH = true;
    public static boolean RULE_SMASH_BALL = true;
    public static boolean RULE_SHIELD = true;
    public static boolean RULE_SMASH_ATTACK = true;
    public static boolean RULE_DOUBLE_JUMP = true;

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(SMASH_BALL, SmashBallEntity.createAttributes());

        PayloadTypeRegistry.serverboundPlay().register(SmashActionPayload.ID, SmashActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncSmashPercentPayload.ID, SyncSmashPercentPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SmashImpactPayload.ID, SmashImpactPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncSmashStatePayload.ID, SyncSmashStatePayload.CODEC);
        
        Registry.register(BuiltInRegistries.SOUND_EVENT, STAR_KO_ID, STAR_KO_SOUND);

        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && (oldPlayer.getY() < -64 || oldPlayer.getY() > 300)) {
                SmashStateManager.setPercent(newPlayer, 0);
            } else {
                SmashStateManager.setPercent(newPlayer, SmashStateManager.getPercent(oldPlayer));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SmashActionPayload.ID, (payload, context) -> {
            net.minecraft.server.level.ServerPlayer player = context.player();
            if (player == null) return;
            context.server().execute(() -> {
                String action = payload.action();
                if (action.equals("jump")) {
                    if (RULE_DOUBLE_JUMP) {
                        if (SmashStateManager.isLedgeGrabbing(player)) {
                            SmashStateManager.setLedgeGrabbing(player, false);
                            player.setDeltaMovement(player.getDirection().getStepX() * 0.4, 0.8, player.getDirection().getStepZ() * 0.4);
                            player.hurtMarked = true;
                            player.fallDistance = 0;
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                            }
                        } else if (!player.onGround() && !hasDoubleJumped.contains(player.getUUID())) {
                            player.setDeltaMovement(player.getDeltaMovement().x, 0.8, player.getDeltaMovement().z);
                            player.hurtMarked = true;
                            player.fallDistance = 0;
                            hasDoubleJumped.add(player.getUUID());
                            
                            // Cloud particles under feet
                            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
                                serverLevel.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.WOOL_FALL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                            }
                            
                            context.server().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "advancement grant @s only smashcraft:airborne_ace");
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("§cDouble Jump is disabled on this server."));
                    }
                } else if (action.equals("attack")) {
                    if (RULE_SMASH_ATTACK) {
                        SmashStateManager.setSmashAttackReady(player, true);
                        player.sendSystemMessage(Component.literal("§cSmash Attack charged! Next hit will be devastating."));
                    } else {
                        player.sendSystemMessage(Component.literal("§cSmash Attacks are disabled on this server."));
                    }
                } else if (action.equals("shield")) {
                    if (RULE_SHIELD) {
                        if (SmashStateManager.isShielding(player)) {
                            SmashStateManager.setShielding(player, false);
                        } else if (SmashStateManager.getShieldHealth(player) > 0) {
                            SmashStateManager.setShielding(player, true);
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("§cShields are disabled on this server."));
                    }
                }
            });
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("smash")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("§6Smash Craft Commands:"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§e/smash set <targets> <percent> §7- Sets percent"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§e/smash add <targets> <percent> §7- Adds percent"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§e/smash get <target> §7- Gets percent"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§e/smash jump §7- Perform a double jump in mid-air"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§e/smash attack §7- Charge a smash attack (next hit does massive knockback)"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§e/smash shield §7- Toggle your shield"), false);
                        return 1;
                    })
                    .then(Commands.literal("set")
                            .then(Commands.argument("targets", EntityArgument.entities())
                                    .then(Commands.argument("percent", FloatArgumentType.floatArg(0))
                                            .executes(context -> {
                                                Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                                float percent = FloatArgumentType.getFloat(context, "percent");
                                                for (Entity target : targets) {
                                                    SmashStateManager.setPercent(target, percent);
                                                }
                                                context.getSource().sendSuccess(() -> Component.literal("Set smash percent to " + percent + " for " + targets.size() + " entities."), true);
                                                return targets.size();
                                            }))))
                    .then(Commands.literal("add")
                            .then(Commands.argument("targets", EntityArgument.entities())
                                    .then(Commands.argument("percent", FloatArgumentType.floatArg())
                                            .executes(context -> {
                                                Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                                float percent = FloatArgumentType.getFloat(context, "percent");
                                                for (Entity target : targets) {
                                                    SmashStateManager.addPercent(target, percent);
                                                }
                                                context.getSource().sendSuccess(() -> Component.literal("Added " + percent + " smash percent to " + targets.size() + " entities."), true);
                                                return targets.size();
                                            }))))
                    .then(Commands.literal("get")
                            .then(Commands.argument("target", EntityArgument.entity())
                                    .executes(context -> {
                                        Entity target = EntityArgument.getEntity(context, "target");
                                        float percent = SmashStateManager.getPercent(target);
                                        context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " has " + percent + "%"), false);
                                        return (int) percent;
                                    })))
                    .then(Commands.literal("scoreboard")
                            .then(Commands.literal("show").executes(context -> {
                                ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
                                Objective objective = scoreboard.getObjective("smashcraft_percent");
                                if (objective != null) {
                                    scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
                                    context.getSource().sendSuccess(() -> Component.literal("Showing smash scoreboard."), false);
                                    return 1;
                                }
                                context.getSource().sendSuccess(() -> Component.literal("Scoreboard objective not created yet. Hit someone first!"), false);
                                return 0;
                            }))
                            .then(Commands.literal("hide").executes(context -> {
                                ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
                                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
                                context.getSource().sendSuccess(() -> Component.literal("Hid smash scoreboard."), false);
                                return 1;
                            })))
                    .then(Commands.literal("jump").executes(context -> {
                        Entity entity = context.getSource().getEntity();
                        if (entity != null) {
                            if (!RULE_DOUBLE_JUMP) {
                                context.getSource().sendFailure(Component.literal("§cDouble Jump is disabled on this server."));
                                return 0;
                            }
                            if (!entity.onGround() && !hasDoubleJumped.contains(entity.getUUID())) {
                                entity.setDeltaMovement(entity.getDeltaMovement().x, 0.8, entity.getDeltaMovement().z);
                                entity.hurtMarked = true;
                                entity.fallDistance = 0;
                                hasDoubleJumped.add(entity.getUUID());
                                context.getSource().sendSuccess(() -> Component.literal("§aDouble jump!"), false);
                                
                                if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1, entity.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
                                    serverLevel.playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.WOOL_FALL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                                }
                                
                                if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                                    context.getSource().getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "advancement grant @s only smashcraft:airborne_ace");
                                }
                            } else {
                                context.getSource().sendFailure(Component.literal("§cYou must be in the air to double jump, and can only do it once!"));
                            }
                        }
                        return 1;
                    }))
                    .then(Commands.literal("attack").executes(context -> {
                        Entity entity = context.getSource().getEntity();
                        if (entity != null) {
                            if (!RULE_SMASH_ATTACK) {
                                context.getSource().sendFailure(Component.literal("§cSmash Attacks are disabled on this server."));
                                return 0;
                            }
                            SmashStateManager.setSmashAttackReady(entity, true);
                            context.getSource().sendSuccess(() -> Component.literal("§cSmash Attack charged! Next hit will be devastating."), false);
                        }
                        return 1;
                    }))
                    .then(Commands.literal("shield").executes(context -> {
                        Entity entity = context.getSource().getEntity();
                        if (entity != null) {
                            if (!RULE_SHIELD) {
                                context.getSource().sendFailure(Component.literal("§cShields are disabled on this server."));
                                return 0;
                            }
                            if (SmashStateManager.isShielding(entity)) {
                                SmashStateManager.setShielding(entity, false);
                                context.getSource().sendSuccess(() -> Component.literal("§bShield deactivated."), false);
                            } else {
                                if (SmashStateManager.getShieldHealth(entity) <= 0) {
                                    context.getSource().sendFailure(Component.literal("§cYour shield is broken! Wait for it to recharge."));
                                } else {
                                    SmashStateManager.setShielding(entity, true);
                                    context.getSource().sendSuccess(() -> Component.literal("§bShield activated! (" + SmashStateManager.getShieldHealth(entity) + " HP)"), false);
                                }
                            }
                        }
                        return 1;
                    }))
                    .then(Commands.literal("rule")
                        .then(Commands.argument("rule", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(context -> {
                                    String rule = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "rule");
                                    boolean value = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value");
                                    switch (rule.toLowerCase()) {
                                        case "ledge_grabbing": RULE_LEDGE_GRABBING = value; break;
                                        case "directional_smash": RULE_DIRECTIONAL_SMASH = value; break;
                                        case "smash_ball": RULE_SMASH_BALL = value; break;
                                        case "shield": RULE_SHIELD = value; break;
                                        case "smash_attack": RULE_SMASH_ATTACK = value; break;
                                        case "double_jump": RULE_DOUBLE_JUMP = value; break;
                                        default:
                                            context.getSource().sendFailure(Component.literal("Unknown rule: " + rule));
                                            return 0;
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("Set SmashCraft rule " + rule + " to " + value), true);
                                    return 1;
                                })
                            )
                        )
                    )
            );
        });
    }
}
