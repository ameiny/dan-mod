package dev.meiny.dan.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import dev.meiny.dan.advancement.ModCriteria;
import dev.meiny.dan.entity.goal.DanTemptGoal;
import dev.meiny.dan.entity.goal.GreetGoal;
import dev.meiny.dan.entity.goal.InspectNatureGoal;
import dev.meiny.dan.entity.goal.TradeWithPlayerGoal;
import dev.meiny.dan.sound.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DanEntity extends PathfinderMob implements GeoEntity, Merchant {
    private static final RawAnimation GREET_ANIMATION = RawAnimation.begin().thenPlay("action.greet");
    private static final long GREET_MEMORY_TICKS = 6000L;
    private static final long INCOMING_GREET_TIMEOUT = 40L;
    private static final long RESTOCK_INTERVAL_TICKS = 24000L;
    private static final long LURE_TIMEOUT_TICKS = 200L;
    private static final double LURE_DISTANCE = 100.0;
    private static final double MEETING_WITNESS_RANGE = 16.0;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Long> greetedRecently = new HashMap<>();

    private boolean inspecting;
    private LivingEntity greetPartner;
    private DanEntity incomingGreeter;
    private long incomingGreetTime;

    private Player tradingPlayer;
    private MerchantOffers offers;
    private long lastRestockTime;

    private Vec3 lureOrigin;
    private long lastLureTime;

    public DanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.TEMPT_RANGE, 10);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0f, 1.0, 1.5));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(3, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(4, new DanTemptGoal(this, 1.1, Ingredient.of(Items.IRON_CHAIN)));
        this.goalSelector.addGoal(5, new GreetGoal(this, 1.0, 8));
        this.goalSelector.addGoal(6, new InspectNatureGoal(this, 1.0, 10));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0f, 1.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || this.isTrading()) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide()) {
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        long now = level.getGameTime();
        if (this.offers != null && !this.isTrading() && now - this.lastRestockTime >= RESTOCK_INTERVAL_TICKS) {
            this.offers.forEach(MerchantOffer::resetUses);
            this.lastRestockTime = now;
        }
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public void setTradingPlayer(Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = DanTrades.create(this.getUUID());
            this.lastRestockTime = this.level().getGameTime();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        if (this.tradingPlayer instanceof ServerPlayer serverPlayer) {
            ModCriteria.TRADED_WITH_DAN.trigger(serverPlayer, offer.getResult());
            if (offer.isOutOfStock()) {
                ModCriteria.BOUGHT_OUT_DAN.trigger(serverPlayer);
            }
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tradingPlayer == player
                && this.isAlive()
                && player.isWithinEntityInteractionRange(this, 4.0);
    }

    public void greet() {
        triggerAnim("action", "greet");
        this.playSound(ModSounds.DAN_GREET1, 1.0f, this.getVoicePitch());
    }

    public void trackLure(Player player) {
        long now = this.level().getGameTime();
        if (this.lureOrigin == null || now - this.lastLureTime > LURE_TIMEOUT_TICKS) {
            this.lureOrigin = this.position();
        }
        this.lastLureTime = now;

        double dx = this.getX() - this.lureOrigin.x;
        double dz = this.getZ() - this.lureOrigin.z;
        if (player instanceof ServerPlayer serverPlayer && dx * dx + dz * dz >= LURE_DISTANCE * LURE_DISTANCE) {
            ModCriteria.LURED_DAN_HOME.trigger(serverPlayer);
        }
    }

    public void announceDanMeeting() {
        for (ServerPlayer player : this.level().getEntitiesOfClass(
                ServerPlayer.class, this.getBoundingBox().inflate(MEETING_WITNESS_RANGE))) {
            ModCriteria.SAW_DANS_MEET.trigger(player);
        }
    }

    public boolean isInspecting() {
        return this.inspecting;
    }

    public void setInspecting(boolean inspecting) {
        this.inspecting = inspecting;
    }

    public void setGreetPartner(LivingEntity partner) {
        this.greetPartner = partner;
    }

    public boolean isBusyWithOther(DanEntity asker) {
        return this.inspecting
                || this.isTrading()
                || (this.greetPartner != null && this.greetPartner != asker);
    }

    public void receiveGreeting(DanEntity from) {
        this.incomingGreeter = from;
        this.incomingGreetTime = this.level().getGameTime();
    }

    public DanEntity takeIncomingGreeter() {
        DanEntity greeter = this.incomingGreeter;
        this.incomingGreeter = null;
        if (greeter == null
                || !greeter.isAlive()
                || this.level().getGameTime() - this.incomingGreetTime > INCOMING_GREET_TIMEOUT) {
            return null;
        }
        return greeter;
    }

    public boolean hasGreetedRecently(Entity other) {
        Long greetedAt = this.greetedRecently.get(other.getUUID());
        return greetedAt != null && this.level().getGameTime() - greetedAt < GREET_MEMORY_TICKS;
    }

    public void markGreeted(Entity other) {
        long now = this.level().getGameTime();
        this.greetedRecently.values().removeIf(greetedAt -> now - greetedAt >= GREET_MEMORY_TICKS);
        this.greetedRecently.put(other.getUUID(), now);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(new AnimationController<DanEntity>("action", 5, test -> PlayState.STOP)
                .triggerableAnim("greet", GREET_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}