package net.sweenus.simplyswords.power;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.GemPowerRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Runic, Nether, or other type of gem power
 * <p>
 * For the pre-existing gem types, it's best to use the subclasses made for that purpose
 * @see RunicGemPower
 * @see RunefusedGemPower
 * @see NetherGemPower
 */
public class GemPower implements TooltipAppender {

	public GemPower(boolean isGreater, PowerType... applicableTypes) {
		this.isGreater = isGreater;
		this.applicableTypes = Arrays.stream(applicableTypes).toList();
	}

	private final boolean isGreater;
	private final List<PowerType> applicableTypes;

	public boolean isGreater() { return isGreater; }
	public List<PowerType> applicableTypes() { return applicableTypes; }

	@Override
	public void appendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type, net.minecraft.component.ComponentsAccess components) {
		List<Text> list = new ArrayList<>();
		appendTooltip(ItemStack.EMPTY, context, list, type, false);
		for (Text text : list) {
			tooltip.accept(text);
		}
	}

	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {}
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {}
	public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {}

	ActionResult use(World world, PlayerEntity user, Hand hand, ItemStack itemStack) { return ActionResult.FAIL; }
	void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {}
	boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) { return false; }
	int getMaxUseTime(ItemStack stack) { return 0; }

	public boolean isEmpty() { return false; }

	//////////////////////////////

	public static GemPower EMPTY = new EmptyGemPower();

	private static class EmptyGemPower extends GemPower {

		public EmptyGemPower() {
			super(false);
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	}

	public final static class GemPowerCodec implements Codec<RegistryEntry<GemPower>> {

		@Override
		public <T> DataResult<Pair<RegistryEntry<GemPower>, T>> decode(DynamicOps<T> ops, T input) {
			return  Identifier.CODEC.decode(ops, input).flatMap(pair -> {
				Identifier identifier = pair.getFirst();
				RegistryEntry<GemPower> entry = GemPowerRegistry.REGISTRY.getHolder(identifier);
				if (entry != null) {
					return DataResult.success(Pair.of(entry, pair.getSecond()));
				} else {
					return DataResult.error(() -> "Unknown decoded power type " + input + " in registry " + GemPowerRegistry.REGISTRY.key());
				}
			});

		}

		@Override
		public <T> DataResult<T> encode(RegistryEntry<GemPower> input, DynamicOps<T> ops, T prefix) {
			try {
				Identifier id = GemPowerRegistry.REGISTRY.getId(input.value());
				if (id == null) {
					return DataResult.error(() -> "Unknown encoded power type " + input + " in registry " + GemPowerRegistry.REGISTRY.key());
				}
				return Identifier.CODEC.encode(id, ops, prefix);
			} catch (Throwable e) {
				return DataResult.error(() -> "Can't access registry " + GemPowerRegistry.REGISTRY.key());
			}
		}
	}
}
