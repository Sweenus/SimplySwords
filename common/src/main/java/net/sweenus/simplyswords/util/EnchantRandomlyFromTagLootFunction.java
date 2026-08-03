package net.sweenus.simplyswords.util;

import com.mojang.logging.LogUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class EnchantRandomlyFromTagLootFunction implements LootFunction {

	private static final Logger LOGGER = LogUtils.getLogger();

	EnchantRandomlyFromTagLootFunction(TagKey<Enchantment> tagKey) {
		this.tagKey = tagKey;
	}

	private final TagKey<Enchantment> tagKey;
	private RegistryEntryList<Enchantment> enchants;


	@Override
	public LootFunctionType getType() {
		//This isn't typically safe (should have its own type), but since we are never using this function in data packs, should be ok
		return LootFunctionTypes.ENCHANT_RANDOMLY;
	}

	@Override
	public ItemStack apply(ItemStack itemStack, LootContext lootContext) {
		if (enchants == null) {
			enchants = Registries.ENCHANTMENT.getEntryList(tagKey).orElse(null);
		}
		if (enchants == null) {
			LOGGER.warn("Couldn't resolve enchantment tag {}", tagKey.id());
			return itemStack;
		}
		List<RegistryEntry<Enchantment>> list = enchants.stream().toList();
		Random random = lootContext.getRandom();
		Optional<RegistryEntry<Enchantment>> optional = Util.getRandomOrEmpty(list, random);
		if (optional.isEmpty()) {
			LOGGER.warn("Couldn't find a compatible enchantment for {}", itemStack);
			return itemStack;
		} else {
			return addEnchantmentToStack(itemStack, optional.get(), random);
		}
	}

	public static EnchantRandomlyFromTagLootFunction.Builder create(TagKey<Enchantment> tagKey) {
		return new Builder(tagKey);
	}

	private static ItemStack addEnchantmentToStack(ItemStack stack, RegistryEntry<Enchantment> enchantment, Random random) {
		int i = MathHelper.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
		if (stack.isOf(Items.BOOK)) {
			stack = new ItemStack(Items.ENCHANTED_BOOK);
		}

		stack.addEnchantment(enchantment.value(), i);
		return stack;
	}

	public static class Builder implements LootFunction.Builder {

		public Builder(TagKey<Enchantment> tagKey) {
			this.tagKey = tagKey;
		}

		private final TagKey<Enchantment> tagKey;

		@Override
		public LootFunction build() {
			return new EnchantRandomlyFromTagLootFunction(tagKey);
		}
	}
}
