package net.sweenus.simplyswords.config.settings;

import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemStackTooltipAppender implements Supplier<TooltipAppender> {

	@SafeVarargs
	public ItemStackTooltipAppender(Supplier<? extends Item>... itemStacks) {
		this.appenders = Arrays.stream(itemStacks).map(StackAppender::new).toList();
	}

	private final List<? extends TooltipAppender> appenders;

	private final static AttributeModifiersComponent hider = new AttributeModifiersComponent(new ArrayList<>());

	@Override
	public TooltipAppender get() {
		long threeSeconds = System.currentTimeMillis() / 3000L;

		return appenders.get((int) (threeSeconds % appenders.size()));
	}

	private record StackAppender(Supplier<? extends Item> stack) implements TooltipAppender {

		@Override
		public void appendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type, ComponentsAccess components) {
			Item s = stack.get();
			if (s == null) return;
			List<Text> list = new ArrayList<>(s.getDefaultStack().getTooltip(context, null, type));
			if (!list.isEmpty() && Objects.equals(list.get(0).getString(), "")) {
				list.remove(0);
			}
			list.forEach(tooltip);
		}
	}
}
