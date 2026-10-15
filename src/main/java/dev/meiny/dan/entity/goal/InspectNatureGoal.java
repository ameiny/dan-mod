package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class InspectNatureGoal extends Goal {
    private static final int SEARCH_CHANCE = 20;
    private static final int COOLDOWN_MIN = 50;
    private static final int COOLDOWN_RANDOM = 100;
    private static final long FORGET_AFTER_TICKS = 24000L;

    private final PathfinderMob mob;
    private final double speed;
    private final int range;
    private final Map<UUID, Long> seenAnimals = new HashMap<>();
    private final Map<BlockPos, Long> seenFlowers = new HashMap<>();

    private Mob targetAnimal;
    private BlockPos targetFlower;
    private int stayTicks;
    private int runTicks;
    private int cooldown;
    private boolean arrived;

    public InspectNatureGoal(PathfinderMob mob, double speed, int range) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        if (this.mob.getRandom().nextInt(SEARCH_CHANCE) != 0) {
            return false;
        }

        if (this.mob.getRandom().nextBoolean()) {
            this.targetAnimal = findAnimal();
            if (this.targetAnimal == null) {
                this.targetFlower = findFlower();
            }
        } else {
            this.targetFlower = findFlower();
            if (this.targetFlower == null) {
                this.targetAnimal = findAnimal();
            }
        }
        return this.targetAnimal != null || this.targetFlower != null;
    }

    @Override
    public void start() {
        this.stayTicks = 100 + this.mob.getRandom().nextInt(100);
        this.runTicks = 0;
        this.arrived = false;
        moveToTarget();

        if (this.targetAnimal != null) {
            spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.targetAnimal.getX(), this.targetAnimal.getEyeY() + 0.5, this.targetAnimal.getZ());
        } else {
            spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.targetFlower.getX() + 0.5, this.targetFlower.getY() + 0.8, this.targetFlower.getZ() + 0.5);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.stayTicks <= 0 || this.runTicks > 600) {
            return false;
        }
        if (this.targetAnimal != null) {
            double maxDistance = this.range * 2.0;
            return this.targetAnimal.isAlive()
                    && this.mob.distanceToSqr(this.targetAnimal) < maxDistance * maxDistance;
        }
        return this.targetFlower != null
                && isFlower(this.mob.level().getBlockState(this.targetFlower));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.runTicks++;

        double x;
        double y;
        double z;
        double lookY;
        double closeEnough;

        if (this.targetAnimal != null) {
            x = this.targetAnimal.getX();
            y = this.targetAnimal.getY();
            z = this.targetAnimal.getZ();
            lookY = this.targetAnimal.getEyeY();
            closeEnough = 3.0;
        } else {
            x = this.targetFlower.getX() + 0.5;
            y = this.targetFlower.getY();
            z = this.targetFlower.getZ() + 0.5;
            lookY = this.targetFlower.getY() + 0.3;
            closeEnough = 2.0;
        }

        this.mob.getLookControl().setLookAt(x, lookY, z);

        if (this.mob.distanceToSqr(x, y, z) > closeEnough * closeEnough) {
            if (this.runTicks % 20 == 0 || this.mob.getNavigation().isDone()) {
                moveToTarget();
            }
        } else {
            this.mob.getNavigation().stop();
            this.stayTicks--;

            if (!this.arrived) {
                this.arrived = true;
                remember();
                spawnParticles(ParticleTypes.HEART, this.mob.getX(), this.mob.getEyeY() + 0.6, this.mob.getZ());
            }
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.targetAnimal = null;
        this.targetFlower = null;
        this.cooldown = COOLDOWN_MIN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM);
        forgetOld();
    }

    private static boolean isFlower(BlockState state) {
        return state.is(BlockTags.FLOWERS)
                && !state.is(Blocks.CHERRY_LEAVES)
                && !state.is(Blocks.FLOWERING_AZALEA_LEAVES)
                && !state.is(Blocks.SPORE_BLOSSOM)
                && !state.is(Blocks.MANGROVE_PROPAGULE)
                && !state.is(Blocks.CHORUS_FLOWER);
    }

    private void remember() {
        long now = this.mob.level().getGameTime();
        if (this.targetAnimal != null) {
            this.seenAnimals.put(this.targetAnimal.getUUID(), now);
        } else if (this.targetFlower != null) {
            this.seenFlowers.put(this.targetFlower, now);
        }
    }

    private <K> boolean isRemembered(Map<K, Long> memory, K key) {
        Long seenAt = memory.get(key);
        return seenAt != null && this.mob.level().getGameTime() - seenAt < FORGET_AFTER_TICKS;
    }

    private void forgetOld() {
        long now = this.mob.level().getGameTime();
        this.seenAnimals.values().removeIf(seenAt -> now - seenAt >= FORGET_AFTER_TICKS);
        this.seenFlowers.values().removeIf(seenAt -> now - seenAt >= FORGET_AFTER_TICKS);
    }

    private void moveToTarget() {
        if (this.targetAnimal != null) {
            this.mob.getNavigation().moveTo(this.targetAnimal.getX(), this.targetAnimal.getY(), this.targetAnimal.getZ(), this.speed);
        } else if (this.targetFlower != null) {
            this.mob.getNavigation().moveTo(this.targetFlower.getX() + 0.5, this.targetFlower.getY(), this.targetFlower.getZ() + 0.5, this.speed);
        }
    }

    private void spawnParticles(SimpleParticleType type, double x, double y, double z) {
        if (this.mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(type, x, y, z, 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private Mob findAnimal() {
        List<Mob> animals = this.mob.level().getEntitiesOfClass(
                Mob.class,
                this.mob.getBoundingBox().inflate(this.range),
                entity -> entity != this.mob
                        && !(entity instanceof DanEntity)
                        && entity.isAlive()
                        && entity.getType().getCategory() == MobCategory.CREATURE
                        && !isRemembered(this.seenAnimals, entity.getUUID())
        );
        if (animals.isEmpty()) {
            return null;
        }
        return animals.get(this.mob.getRandom().nextInt(animals.size()));
    }

    private BlockPos findFlower() {
        BlockPos origin = this.mob.blockPosition();
        List<BlockPos> flowers = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-this.range, -2, -this.range), origin.offset(this.range, 2, this.range))) {
            if (isFlower(this.mob.level().getBlockState(pos))
                    && !isRemembered(this.seenFlowers, pos)) {
                flowers.add(pos.immutable());
            }
        }
        if (flowers.isEmpty()) {
            return null;
        }
        return flowers.get(this.mob.getRandom().nextInt(flowers.size()));
    }
}