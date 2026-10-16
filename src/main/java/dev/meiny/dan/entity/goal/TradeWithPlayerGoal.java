package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class TradeWithPlayerGoal extends Goal {
    private final DanEntity dan;

    public TradeWithPlayerGoal(DanEntity dan) {
        this.dan = dan;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.dan.isAlive() && this.dan.getTradingPlayer() != null;
    }

    @Override
    public void start() {
        this.dan.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player player = this.dan.getTradingPlayer();
        if (player != null) {
            this.dan.getNavigation().stop();
            this.dan.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ());
        }
    }

    @Override
    public void stop() {
        this.dan.setTradingPlayer(null);
    }
}