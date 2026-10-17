package dev.meiny.dan.entity.goal;

import dev.meiny.dan.entity.DanEntity;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.ItemStack;

public class DanTemptGoal extends TemptGoal {
    private final DanEntity dan;

    public DanTemptGoal(DanEntity dan, double speed, Predicate<ItemStack> items) {
        super(dan, speed, items, false);
        this.dan = dan;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.player != null) {
            this.dan.trackLure(this.player);
        }
    }
}
