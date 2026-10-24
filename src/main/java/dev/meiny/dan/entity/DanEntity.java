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
import dev.meiny.dan.entity.goal.DanCashierGoal;
import dev.meiny.dan.entity.goal.DanFollowOwnerGoal;
import dev.meiny.dan.entity.goal.DanStayGoal;
import dev.meiny.dan.entity.goal.DanTemptGoal;
import dev.meiny.dan.entity.goal.GreetGoal;
import dev.meiny.dan.entity.goal.InspectNatureGoal;
import dev.meiny.dan.entity.goal.TradeWithPlayerGoal;
import dev.meiny.dan.item.ModItems;
import dev.meiny.dan.sound.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class DanEntity extends PathfinderMob implements GeoEntity, Merchant {
    private static final RawAnimation GREET_ANIMATION = RawAnimation.begin().thenPlay("action.greet");
    private static final long GREET_MEMORY_TICKS = 6000L;
    private static final long INCOMING_GREET_TIMEOUT = 40L;
    private static final long RESTOCK_INTERVAL_TICKS = 24000L;
    private static final long LURE_TIMEOUT_TICKS = 200L;
    private static final double LURE_DISTANCE = 100.0;
    private static final double MEETING_WITNESS_RANGE = 16.0;
    private static final int BEFRIEND_CHANCE = 3;
    private static final float FRIEND_HEAL_AMOUNT = 4.0f;
    private static final String ALDI_CASHIER_TAG = "aldi_cashier";
    private static final long TRADE_SOUND_COOLDOWN_TICKS = 50L;
    private static final EntityDataAccessor<Boolean> DATA_ALDI_CASHIER =
            SynchedEntityData.defineId(DanEntity.class, EntityDataSerializers.BOOLEAN);

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

    private UUID ownerId;
    private boolean staying;

    private long lastTradeSoundTime = -TRADE_SOUND_COOLDOWN_TICKS;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ALDI_CASHIER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new DanCashierGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0f, 1.0, 1.5));
        this.goalSelector.addGoal(1, new DanStayGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(3, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(4, new DanFollowOwnerGoal(this, 1.2));
        this.goalSelector.addGoal(5, new DanTemptGoal(this, 1.1, Ingredient.of(Items.IRON_CHAIN)));
        this.goalSelector.addGoal(6, new GreetGoal(this, 1.0, 8));
        this.goalSelector.addGoal(7, new InspectNatureGoal(this, 1.0, 10));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0f, 1.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
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

        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.MILK_JUMBO) && !this.isAldiCashier()) {
            if (!this.hasOwner()) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    stack.consume(1, player);
                    this.tryBefriend(serverLevel, player);
                }
                return InteractionResult.SUCCESS;
            }
            if (this.isOwnedBy(player) && this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide()) {
                    stack.consume(1, player);
                    this.heal(FRIEND_HEAL_AMOUNT);
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (this.isOwnedBy(player) && player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                this.setStaying(!this.staying);
                player.sendOverlayMessage(Component.translatable(
                        this.staying ? "message.dan.dan_stays" : "message.dan.dan_follows"));
            }
            return InteractionResult.SUCCESS;
        }

        if (!this.level().isClientSide()) {
            this.setTradingPlayer(player);
            Component title = this.isAldiCashier()
                    ? Component.translatable("merchant.dan.aldi_cashier")
                    : this.getDisplayName();
            this.openTradingScreen(player, title, 1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.entityData.set(DATA_ALDI_CASHIER, this.entityTags().contains(ALDI_CASHIER_TAG));
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
            this.offers = this.isAldiCashier() ? DanTrades.createAldi() : DanTrades.create(this.getUUID());
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
        long now = this.level().getGameTime();
        if (now - this.lastTradeSoundTime >= TRADE_SOUND_COOLDOWN_TICKS) {
            this.lastTradeSoundTime = now;
            this.playSound(ModSounds.DAN_TRADE, 1.0f, 1.0f);
        }
        if (this.tradingPlayer instanceof ServerPlayer serverPlayer) {
            ModCriteria.TRADED_WITH_DAN.trigger(serverPlayer, offer.getResult(), this.isAldiCashier());
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

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DAN_DEATH;
    }

    @Override
    public void makeSound(SoundEvent sound) {
        if (sound == ModSounds.DAN_DEATH) {
            this.playSound(sound, this.getSoundVolume(), 1.0f);
            return;
        }
        super.makeSound(sound);
    }

    public boolean isAldiCashier() {
        if (this.level().isClientSide()) {
            return this.entityData.get(DATA_ALDI_CASHIER);
        }
        return this.entityTags().contains(ALDI_CASHIER_TAG);
    }

    @Override
    public boolean isPushable() {
        return !this.isAldiCashier() && super.isPushable();
    }

    public boolean hasOwner() {
        return this.ownerId != null;
    }

    public boolean isOwnedBy(Player player) {
        return this.ownerId != null && this.ownerId.equals(player.getUUID());
    }

    public Player getOwner() {
        return this.ownerId == null ? null : this.level().getPlayerByUUID(this.ownerId);
    }

    public boolean isStaying() {
        return this.staying;
    }

    public void setStaying(boolean staying) {
        this.staying = staying;
        if (staying) {
            this.getNavigation().stop();
        }
    }

    private void tryBefriend(ServerLevel level, Player player) {
        if (this.getRandom().nextInt(BEFRIEND_CHANCE) != 0) {
            this.spawnFriendshipParticles(level, ParticleTypes.SMOKE);
            return;
        }
        this.ownerId = player.getUUID();
        this.staying = false;
        this.getNavigation().stop();
        this.greet();
        this.spawnFriendshipParticles(level, ParticleTypes.HEART);
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriteria.BEFRIENDED_DAN.trigger(serverPlayer);
        }
    }

    private void spawnFriendshipParticles(ServerLevel level, SimpleParticleType type) {
        level.sendParticles(type, this.getX(), this.getY(0.9), this.getZ(), 7, 0.4, 0.3, 0.4, 0.02);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.ownerId != null) {
            output.store("Owner", UUIDUtil.CODEC, this.ownerId);
        }
        output.putBoolean("Staying", this.staying);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.ownerId = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.staying = input.getBooleanOr("Staying", false);
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