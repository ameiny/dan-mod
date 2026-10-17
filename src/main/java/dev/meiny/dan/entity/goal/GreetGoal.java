package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class GreetGoal extends Goal {
    private static final int SEARCH_CHANCE = 10;
    private static final double GREET_DISTANCE = 3.0;
    private static final int FACE_TICKS = 15;
    private static final int RESPONSE_DELAY_TICKS = 12;
    private static final int GREET_TICKS = 50;
    private static final int MAX_APPROACH_TICKS = 400;
    private static final int COOLDOWN_MIN = 100;
    private static final int COOLDOWN_RANDOM = 200;
    private static final float TURN_SPEED = 15.0f;

    private enum Phase { APPROACH, FACE, GREET }

    private final DanEntity dan;
    private final double speed;
    private final int range;

    private LivingEntity target;
    private Phase phase = Phase.APPROACH;
    private int phaseTicks;
    private int cooldown;
    private boolean responding;

    public GreetGoal(DanEntity dan, double speed, int range) {
        this.dan = dan;
        this.speed = speed;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.dan.isInspecting()) {
            return false;
        }

        DanEntity greeter = this.dan.takeIncomingGreeter();
        if (greeter != null) {
            this.target = greeter;
            this.phase = Phase.FACE;
            this.responding = true;
            return true;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (this.dan.getRandom().nextInt(SEARCH_CHANCE) != 0) {
            return false;
        }

        this.target = findTarget();
        this.phase = Phase.APPROACH;
        this.responding = false;
        return this.target != null;
    }

    @Override
    public void start() {
        this.phaseTicks = 0;
        this.dan.setGreetPartner(this.target);
        if (this.phase == Phase.APPROACH) {
            moveToTarget();
        } else {
            this.dan.getNavigation().stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }
        if (this.target instanceof DanEntity other && other.isBusyWithOther(this.dan)) {
            return false;
        }

        double maxDistance = this.range * 2.0;
        return switch (this.phase) {
            case APPROACH -> this.phaseTicks < MAX_APPROACH_TICKS
                    && this.dan.distanceToSqr(this.target) < maxDistance * maxDistance;
            case FACE -> true;
            case GREET -> this.phaseTicks < GREET_TICKS;
        };
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.phaseTicks++;
        this.dan.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());

        switch (this.phase) {
            case APPROACH -> tickApproach();
            case FACE -> {
                this.dan.getNavigation().stop();
                turnTowardsTarget();
                int waitTicks = FACE_TICKS + (this.responding ? RESPONSE_DELAY_TICKS : 0);
                if (this.phaseTicks >= waitTicks) {
                    this.dan.greet();
                    this.dan.markGreeted(this.target);
                    if (this.responding && this.target instanceof DanEntity) {
                        this.dan.announceDanMeeting();
                    }
                    switchPhase(Phase.GREET);
                }
            }
            case GREET -> {
                this.dan.getNavigation().stop();
                turnTowardsTarget();
            }
        }
    }

    @Override
    public void stop() {
        this.dan.getNavigation().stop();
        this.dan.setGreetPartner(null);
        this.target = null;
        this.responding = false;
        this.cooldown = COOLDOWN_MIN + this.dan.getRandom().nextInt(COOLDOWN_RANDOM);
    }

    private void tickApproach() {
        if (this.dan.takeIncomingGreeter() == this.target) {
            this.responding = true;
            switchPhase(Phase.FACE);
            return;
        }

        if (this.dan.distanceToSqr(this.target) > GREET_DISTANCE * GREET_DISTANCE) {
            if (this.phaseTicks % 20 == 0 || this.dan.getNavigation().isDone()) {
                moveToTarget();
            }
            return;
        }

        this.dan.getNavigation().stop();
        if (this.target instanceof DanEntity other) {
            other.receiveGreeting(this.dan);
        }
        this.responding = false;
        switchPhase(Phase.FACE);
    }

    private void switchPhase(Phase newPhase) {
        this.phase = newPhase;
        this.phaseTicks = 0;
    }

    private void turnTowardsTarget() {
        double dx = this.target.getX() - this.dan.getX();
        double dz = this.target.getZ() - this.dan.getZ();
        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yaw = Mth.approachDegrees(this.dan.getYRot(), targetYaw, TURN_SPEED);
        this.dan.setYRot(yaw);
        this.dan.yBodyRot = yaw;
    }

    private void moveToTarget() {
        this.dan.getNavigation().moveTo(this.target.getX(), this.target.getY(), this.target.getZ(), this.speed);
    }

    private LivingEntity findTarget() {
        List<LivingEntity> candidates = this.dan.level().getEntitiesOfClass(
                LivingEntity.class,
                this.dan.getBoundingBox().inflate(this.range),
                entity -> entity != this.dan
                        && entity.isAlive()
                        && isGreetable(entity)
                        && !this.dan.hasGreetedRecently(entity)
                        && !(entity instanceof DanEntity other && other.isBusyWithOther(this.dan))
                        && this.dan.hasLineOfSight(entity)
        );
        return candidates.stream()
                .min(Comparator.comparingDouble(this.dan::distanceToSqr))
                .orElse(null);
    }

    private static boolean isGreetable(LivingEntity entity) {
        return entity instanceof DanEntity
                || entity.getType() == EntityTypes.VILLAGER
                || entity.getType() == EntityTypes.WANDERING_TRADER;
    }
}