package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class DanFollowOwnerGoal extends Goal {
    private static final double START_DISTANCE = 8.0;
    private static final double STOP_DISTANCE = 3.0;
    private static final double TELEPORT_DISTANCE = 16.0;
    private static final int REPATH_TICKS = 10;
    private static final int TELEPORT_ATTEMPTS = 10;

    private final DanEntity dan;
    private final double speed;

    private Player owner;
    private int timeToRecalcPath;

    public DanFollowOwnerGoal(DanEntity dan, double speed) {
        this.dan = dan;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.dan.isStaying()) {
            return false;
        }
        Player candidate = this.dan.getOwner();
        if (!isFollowable(candidate) || this.dan.distanceToSqr(candidate) < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        this.owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.dan.isStaying()
                && isFollowable(this.owner)
                && this.owner.level() == this.dan.level()
                && this.dan.distanceToSqr(this.owner) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.dan.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.dan.getLookControl().setLookAt(this.owner, 10.0f, this.dan.getMaxHeadXRot());
        if (--this.timeToRecalcPath > 0) {
            return;
        }
        this.timeToRecalcPath = this.adjustedTickDelay(REPATH_TICKS);
        if (this.dan.distanceToSqr(this.owner) >= TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            tryTeleportToOwner();
        } else {
            this.dan.getNavigation().moveTo(this.owner, this.speed);
        }
    }

    private static boolean isFollowable(Player player) {
        return player != null && player.isAlive() && !player.isSpectator();
    }

    private void tryTeleportToOwner() {
        BlockPos ownerPos = this.owner.blockPosition();
        RandomSource random = this.dan.getRandom();
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            int dx = random.nextInt(7) - 3;
            int dy = random.nextInt(3) - 1;
            int dz = random.nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos target = ownerPos.offset(dx, dy, dz);
            if (canStandAt(target)) {
                this.dan.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                        this.dan.getYRot(), this.dan.getXRot());
                this.dan.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canStandAt(BlockPos pos) {
        Level level = this.dan.level();
        BlockPos below = pos.below();
        if (!level.getBlockState(below).entityCanStandOn(level, below, this.dan)) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        AABB box = this.dan.getBoundingBox().move(
                pos.getX() + 0.5 - this.dan.getX(),
                pos.getY() - this.dan.getY(),
                pos.getZ() + 0.5 - this.dan.getZ());
        return level.noCollision(this.dan, box);
    }
}
