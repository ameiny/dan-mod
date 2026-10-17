package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

public class DanStayGoal extends Goal {
    private final DanEntity dan;

    public DanStayGoal(DanEntity dan) {
        this.dan = dan;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.dan.isStaying() && !this.dan.isInWater() && this.dan.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dan.isStaying();
    }

    @Override
    public void start() {
        this.dan.getNavigation().stop();
    }
}
