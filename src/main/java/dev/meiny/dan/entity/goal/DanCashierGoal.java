package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

public class DanCashierGoal extends Goal {
    private final DanEntity dan;

    public DanCashierGoal(DanEntity dan) {
        this.dan = dan;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.dan.isAldiCashier();
    }

    @Override
    public void start() {
        this.dan.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.dan.getNavigation().stop();
    }
}
