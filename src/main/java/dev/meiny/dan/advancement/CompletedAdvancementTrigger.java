package dev.meiny.dan.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CompletedAdvancementTrigger extends SimpleCriterionTrigger<CompletedAdvancementTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Identifier advancement) {
        this.trigger(player, instance -> instance.advancement().equals(advancement));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Identifier advancement)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Identifier.CODEC.fieldOf("advancement").forGetter(TriggerInstance::advancement)
        ).apply(instance, TriggerInstance::new));
    }
}
