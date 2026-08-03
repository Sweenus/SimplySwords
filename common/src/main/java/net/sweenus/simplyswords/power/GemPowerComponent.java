package net.sweenus.simplyswords.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

//
// The runic and nether gem powers socketed into a weapon or stored on a gem.
//
// Powers are stored as plain Identifiers rather than RegistryEntry<GemPower>.
// Holders come from two sources that can never compare equal to each other - freshly built
// components hold Architectury RegistrySuppliers, decoded ones hold vanilla
// RegistryEntry.References - so a holder-backed component never survives an
// encode/decode round trip intact. Storage mods (Refined Storage, Create, AE2) rebuild a
// stack from its component map and match it against what they hold, so that mismatch made
// extraction inaccurate. Identifiers give correct value equality and also keep every
// getKey() call path out of this class, which is what crashes on NeoForge.
//
// See GemPowerRegistry#resolve(Identifier)
//
public record GemPowerComponent(boolean hasRunicPower, boolean hasNetherPower, Identifier runicPower, Identifier netherPower) {

	public static final Codec<GemPowerComponent> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.BOOL.fieldOf("has_runic_power").forGetter(GemPowerComponent::hasRunicPower),
						Codec.BOOL.fieldOf("has_nether_power").forGetter(GemPowerComponent::hasNetherPower),
						Identifier.CODEC.fieldOf("runic_power").forGetter(GemPowerComponent::runicPower),
						Identifier.CODEC.fieldOf("nether_power").forGetter(GemPowerComponent::netherPower)
				).apply(instance, GemPowerComponent::new)
			);


	public static final GemPowerComponent DEFAULT = new GemPowerComponent(false, false, GemPower.EMPTY_ID, GemPower.EMPTY_ID);

	// The socketed runic power, or GemPower#EMPTY if the socket is empty or the power is unknown.
	public GemPower runic() {
		return GemPowerRegistry.resolve(runicPower);
	}

	// The socketed nether power, or GemPower#EMPTY if the socket is empty or the power is unknown.
	public GemPower nether() {
		return GemPowerRegistry.resolve(netherPower);
	}

	public static GemPowerComponent runic(@NotNull Identifier power) {
		return new GemPowerComponent(true, false, power, GemPower.EMPTY_ID);
	}

	public static GemPowerComponent nether(@NotNull Identifier power) {
		return new GemPowerComponent(false, true, GemPower.EMPTY_ID, power);
	}

	public static GemPowerComponent create(@Nullable Identifier runic, @Nullable Identifier nether) {
		return new GemPowerComponent(runic != null, nether != null, runic != null ? runic : GemPower.EMPTY_ID, nether != null ? nether : GemPower.EMPTY_ID);
	}

	public static GemPowerComponent createEmpty(boolean hasRunic, boolean hasNether) {
		return new GemPowerComponent(hasRunic, hasNether, GemPower.EMPTY_ID, GemPower.EMPTY_ID);
	}

	public GemPowerComponent fill(BiFunction<Boolean, Identifier, Identifier> runicFiller, BiFunction<Boolean, Identifier, Identifier> netherFiller) {
		return new GemPowerComponent(this.hasRunicPower, this.hasNetherPower, runicFiller.apply(this.hasRunicPower, this.runicPower), netherFiller.apply(this.hasNetherPower, this.netherPower));
	}

	public boolean canBeFilled() {
		return hasRunicPower || hasNetherPower;
	}

	public boolean isEmpty() {
		return !hasRunicSlotFilled() && !hasNetherSlotFilled();
	}

	// Whether the runic socket holds an actual power rather than the empty placeholder.
	public boolean hasRunicSlotFilled() {
		return !runic().isEmpty();
	}

	// Whether the nether socket holds an actual power rather than the empty placeholder.
	public boolean hasNetherSlotFilled() {
		return !nether().isEmpty();
	}

	public boolean hasPower(Identifier power) {
		return hasRunic(power) || hasNether(power);
	}

	public boolean hasRunic(Identifier power) {
		return hasRunicSlotFilled() && this.runicPower.equals(power);
	}

	public boolean hasNether(Identifier power) {
		return hasNetherSlotFilled() && this.netherPower.equals(power);
	}

	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!AwakeningApi.areGemPowersActive(stack)) return;
		runic().postHit(stack, target, attacker);
		nether().postHit(stack, target, attacker);
	}

	public void onSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
		if (!AwakeningApi.areGemPowersActive(stack)) return;
		runic().onSwing(stack, world, user, hand);
		nether().onSwing(stack, world, user, hand);
	}

	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		if (!AwakeningApi.areGemPowersActive(itemStack)) {
			return TypedActionResult.fail(itemStack);
		}
		TypedActionResult<ItemStack> result1 = runic().use(world, user, hand, itemStack);
		TypedActionResult<ItemStack> result2 = nether().use(world, user, hand, itemStack);
		if (result1.getResult().compareTo(result2.getResult()) < 0) {
			return result1;
		} else {
			return result2;
		}
	}

	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!AwakeningApi.areGemPowersActive(stack)) return;
		runic().usageTick(world, user, stack, remainingUseTicks);
		nether().usageTick(world, user, stack, remainingUseTicks);
	}

	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!AwakeningApi.areGemPowersActive(stack)) return;
		runic().onStoppedUsing(stack, world, user, remainingUseTicks);
		nether().onStoppedUsing(stack, world, user, remainingUseTicks);
	}

	public int getMaxUseTime(ItemStack stack) {
		if (!AwakeningApi.areGemPowersActive(stack)) return 0;
		return Math.max(runic().getMaxUseTime(stack), nether().getMaxUseTime(stack));
	}

	public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
		if (!AwakeningApi.areGemPowersActive(stack)) return;
		runic().inventoryTick(stack, world, user, slot, selected);
		nether().inventoryTick(stack, world, user, slot, selected);
	}

	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
		appendTooltip(itemStack, world, tooltip, tooltipContext, false);
	}

	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
		GemPower runic = runic();
		if (runic.isGreater()) {
			tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").setStyle(Styles.RUNIC));
		}
		if (!runic.isEmpty()) {
			runic.appendTooltip(itemStack, world, tooltip, tooltipContext, isRunic);
		} else if (!isRunic && hasRunicPower) {
			tooltip.add(Text.translatable("item.simplyswords.empty_runic_slot").setStyle(Styles.RUNIC));
		}

		GemPower nether = nether();
		if (nether.isGreater()) {
			tooltip.add(Text.translatable("item.simplyswords.greater_nether_power").setStyle(Styles.NETHERFUSED));
		}
		if (!nether.isEmpty()) {
			nether.appendTooltip(itemStack, world, tooltip, tooltipContext, isRunic);
		} else if (!isRunic && hasNetherPower) {
			tooltip.add(Text.translatable("item.simplyswords.empty_nether_slot").setStyle(Styles.NETHERFUSED));
		}
	}
}
