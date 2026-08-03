package net.sweenus.simplyswords.config.settings;

import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemStackTooltipAppender implements Supplier<TooltipProvider> {

	@SafeVarargs
	public ItemStackTooltipAppender(Supplier<? extends Item>... itemStacks) {
		this.appenders = Arrays.stream(itemStacks).map(StackAppender::new).toList();
	}

	private final List<? extends TooltipProvider> appenders;

	@Override
	public TooltipProvider get() {
		long threeSeconds = System.currentTimeMillis() / 3000L;

		return appenders.get((int) (threeSeconds % appenders.size()));
	}

	private record StackAppender(Supplier<? extends Item> stack) implements TooltipProvider {

		@Override
		public void appendTooltip(Consumer<Text> tooltip) {
			Item s = stack.get();
			if (s == null) return;
			List<Text> list = new ArrayList<>();
			s.appendTooltip(s.getDefaultStack(), null, list, net.minecraft.client.item.TooltipContext.BASIC);
			if (!list.isEmpty() && Objects.equals(list.get(0).getString(), "")) {
				list.remove(0);
			}
			list.forEach(tooltip);
		}
	}
}
