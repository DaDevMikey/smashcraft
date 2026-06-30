package net.smashcraft;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.smashcraft.network.SyncSmashPercentPayload;
import net.smashcraft.network.SyncSmashStatePayload;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SmashStateManager {

    private static final Map<UUID, Float> percentages = new HashMap<>();
    private static final Set<UUID> launchedEntities = new HashSet<>();
    private static final Set<UUID> starKOEntities = new HashSet<>();
    
    private static final Set<UUID> shieldingEntities = new HashSet<>();
    private static final Map<UUID, Float> shieldHealth = new HashMap<>();
    private static final Set<UUID> smashAttackReady = new HashSet<>();
    
    // Freeze frame state: ticks remaining before Star KO launch
    private static final Map<UUID, Integer> freezeFrameTicks = new HashMap<>();
    private static final Map<UUID, double[]> freezePositions = new HashMap<>(); // x, y, z of freeze origin
    
    // Last damage dealt to entity (for scaling knockback with weapon damage)
    private static final Map<UUID, Float> lastDamageAmount = new HashMap<>();

    // Ledge grabbing state
    private static final Set<UUID> ledgeGrabbingEntities = new HashSet<>();
    
    // Final Smash state
    private static final Set<UUID> finalSmashReady = new HashSet<>();

    public static float getPercent(Entity entity) {
        return percentages.getOrDefault(entity.getUUID(), 0f);
    }

    public static void setPercent(Entity entity, float percent) {
        if (percent < 0) percent = 0;
        percentages.put(entity.getUUID(), percent);

        updateScoreboardAndSync(entity, percent);
        sendActionBar(entity, percent);
    }

    public static void addPercent(Entity entity, float percent) {
        setPercent(entity, getPercent(entity) + percent);
    }

    public static void setLaunched(Entity entity, boolean launched) {
        if (launched) {
            launchedEntities.add(entity.getUUID());
        } else {
            launchedEntities.remove(entity.getUUID());
        }
    }

    public static boolean isLaunched(Entity entity) {
        return launchedEntities.contains(entity.getUUID());
    }

    public static void setStarKO(Entity entity, boolean starKO) {
        if (starKO) {
            starKOEntities.add(entity.getUUID());
        } else {
            starKOEntities.remove(entity.getUUID());
        }
        syncState(entity);
    }

    public static boolean isStarKO(Entity entity) {
        return starKOEntities.contains(entity.getUUID());
    }

    public static void setShielding(Entity entity, boolean shielding) {
        if (shielding) {
            shieldingEntities.add(entity.getUUID());
            if (!shieldHealth.containsKey(entity.getUUID())) {
                shieldHealth.put(entity.getUUID(), 100f); // Default full shield
            }
        } else {
            shieldingEntities.remove(entity.getUUID());
        }
        syncState(entity);
    }

    public static boolean isShielding(Entity entity) {
        return shieldingEntities.contains(entity.getUUID());
    }

    public static float getShieldHealth(Entity entity) {
        return shieldHealth.getOrDefault(entity.getUUID(), 100f);
    }

    public static void setShieldHealth(Entity entity, float health) {
        shieldHealth.put(entity.getUUID(), health);
        syncState(entity);
    }

    /**
     * Updates shield health locally without triggering a network sync.
     * Use this for per-tick updates where syncing every tick would be wasteful.
     */
    public static void setShieldHealthLocal(Entity entity, float health) {
        shieldHealth.put(entity.getUUID(), health);
    }

    public static void setSmashAttackReady(Entity entity, boolean ready) {
        if (ready) {
            smashAttackReady.add(entity.getUUID());
        } else {
            smashAttackReady.remove(entity.getUUID());
        }
        syncState(entity);
    }

    public static boolean isSmashAttackReady(Entity entity) {
        return smashAttackReady.contains(entity.getUUID());
    }

    // Freeze frame methods
    public static void setFreezeFrames(Entity entity, int ticks, double x, double y, double z) {
        freezeFrameTicks.put(entity.getUUID(), ticks);
        freezePositions.put(entity.getUUID(), new double[]{x, y, z});
    }

    public static int getFreezeFrameTicks(Entity entity) {
        return freezeFrameTicks.getOrDefault(entity.getUUID(), 0);
    }

    public static double[] getFreezePosition(Entity entity) {
        return freezePositions.get(entity.getUUID());
    }

    public static boolean tickFreezeFrame(Entity entity) {
        int ticks = freezeFrameTicks.getOrDefault(entity.getUUID(), 0);
        if (ticks > 0) {
            ticks--;
            freezeFrameTicks.put(entity.getUUID(), ticks);
            return ticks == 0; // Returns true when freeze is done
        }
        return false;
    }

    public static boolean isFrozen(Entity entity) {
        return freezeFrameTicks.getOrDefault(entity.getUUID(), 0) > 0;
    }

    public static void clearFreezeFrame(Entity entity) {
        freezeFrameTicks.remove(entity.getUUID());
        freezePositions.remove(entity.getUUID());
    }

    // Last damage tracking for knockback scaling
    public static void setLastDamage(Entity entity, float damage) {
        lastDamageAmount.put(entity.getUUID(), damage);
    }

    public static float getLastDamage(Entity entity) {
        return lastDamageAmount.getOrDefault(entity.getUUID(), 1f);
    }

    // Ledge grabbing methods
    public static void setLedgeGrabbing(Entity entity, boolean grabbing) {
        if (grabbing) {
            ledgeGrabbingEntities.add(entity.getUUID());
        } else {
            ledgeGrabbingEntities.remove(entity.getUUID());
        }
    }

    public static boolean isLedgeGrabbing(Entity entity) {
        return ledgeGrabbingEntities.contains(entity.getUUID());
    }

    // Final Smash methods
    public static void setFinalSmashReady(Entity entity, boolean ready) {
        if (ready) {
            finalSmashReady.add(entity.getUUID());
        } else {
            finalSmashReady.remove(entity.getUUID());
        }
    }

    public static boolean isFinalSmashReady(Entity entity) {
        return finalSmashReady.contains(entity.getUUID());
    }

    private static void sendActionBar(Entity entity, float percent) {
        if (entity instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("§c§lSmash: " + String.format("%.1f", percent) + "%"));
        }
    }

    private static void updateScoreboardAndSync(Entity entity, float percent) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            ServerScoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective("smashcraft_percent");
            if (objective == null) {
                objective = scoreboard.addObjective(
                        "smashcraft_percent",
                        ObjectiveCriteria.DUMMY,
                        Component.literal("Smash %"),
                        ObjectiveCriteria.RenderType.INTEGER,
                        true,
                        null
                );
                scoreboard.setDisplayObjective(DisplaySlot.BELOW_NAME, objective);
            }
            scoreboard.getOrCreatePlayerScore(entity, objective).set((int) percent);

            SyncSmashPercentPayload payload = new SyncSmashPercentPayload(entity.getId(), percent);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, payload);
            }

            if (!(entity instanceof Player) && percent > 0) {
                if (!entity.hasCustomName()) {
                    entity.setCustomName(entity.getType().getDescription());
                }
                entity.setCustomNameVisible(true);
            }
        }
    }

    public static void syncState(Entity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            SyncSmashStatePayload payload = new SyncSmashStatePayload(
                    entity.getId(),
                    isStarKO(entity),
                    isShielding(entity),
                    getShieldHealth(entity),
                    isSmashAttackReady(entity)
            );
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
