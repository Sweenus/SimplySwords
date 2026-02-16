package net.sweenus.simplyswords.config.settings;

import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.util.Walkable;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A settings block that will provide a tooltip from the supplied TooltipAppender
 */
public class TooltipSettings implements Translatable, Walkable {

	public TooltipSettings(@Nullable Supplier<? extends TooltipAppender> appender) {
		this.appender = appender;
	}

	public TooltipSettings(ItemStack stack) {
		this(() -> new ItemStackAppender(stack));
	}

	public TooltipSettings() {
		this((Supplier<? extends TooltipAppender>) null);
	}

	@Nullable
	Supplier<? extends TooltipAppender> appender;

	@NotNull
	@Override
	public String translationKey() {
		return "";
	}

	@NotNull
	@Override
	public String descriptionKey() {
		return "";
	}

	@NotNull
	@Override
	public MutableText translation(@Nullable String fallback) {
		return Translatable.super.translation(fallback);
	}

	@NotNull
	@Override
	public MutableText description(@Nullable String fallback) {
		//System.out.println("description");
		if (appender == null) return Text.empty();
		final MutableText[] desc = {null};
		appender.get().appendTooltip(Item.TooltipContext.DEFAULT, (text) -> {
			if (desc[0] == null) {
				desc[0] = text.copy();
			} else {
				desc[0].append(Text.literal("\n")).append(text);
			}
		}, TooltipType.BASIC, ItemStack.EMPTY);
		return desc[0];
	}

	@Override
	public boolean hasTranslation() {
		return false;
	}

	@Override
	public boolean hasDescription() {
		//System.out.println("description check??");
		return appender != null;
	}

	private static record ItemStackAppender(ItemStack stack) implements TooltipAppender {

		@Override
		public void appendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type, ComponentsAccess components) {
			stack.getTooltip(context, null, type).forEach(tooltip);
		}
	}
}
