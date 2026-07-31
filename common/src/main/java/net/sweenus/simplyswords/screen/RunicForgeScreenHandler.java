package net.sweenus.simplyswords.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.registry.BlocksRegistry;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.ScreenHandlerRegistry;
import net.sweenus.simplyswords.util.LegacyUniqueMigration;

public class RunicForgeScreenHandler extends ScreenHandler {
    public static final int WEAPON_SLOT = 0;
    public static final int RUNIC_GEM_SLOT = 1;
    public static final int NETHER_GEM_SLOT = 2;
    public static final int TABLET_START = 3;
    public static final int TABLET_COUNT = 8;
    public static final int FORGE_SLOT_COUNT = TABLET_START + TABLET_COUNT;
    public static final int PLAYER_SLOT_START = FORGE_SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = 36;
    public static final int PLAYER_SLOT_END = PLAYER_SLOT_START + PLAYER_SLOT_COUNT;
    public static final int PREVIEW_SLOT = PLAYER_SLOT_END;

    private final SimpleInventory forgeInventory;
    private final SimpleInventory previewInventory;
    private final PlayerEntity owner;
    private final BlockPos forgePos;
    private boolean editingLoaded;
    private boolean committing;
    private ItemStack originalStack = ItemStack.EMPTY;
    private int originalLevel;

    public RunicForgeScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readBlockPos());
    }

    public RunicForgeScreenHandler(int syncId, PlayerInventory inventory, BlockPos forgePos) {
        this(syncId, inventory, new SimpleInventory(FORGE_SLOT_COUNT), forgePos);
    }

    private RunicForgeScreenHandler(int syncId, PlayerInventory playerInventory,
                                    SimpleInventory forgeInventory, BlockPos forgePos) {
        super(ScreenHandlerRegistry.RUNIC_FORGE.get(), syncId);
        this.forgeInventory = forgeInventory;
        this.previewInventory = new SimpleInventory(1);
        this.owner = playerInventory.player;
        this.forgePos = forgePos;
        forgeInventory.onOpen(owner);
        forgeInventory.addListener(this::onContentChanged);

        addSlot(new WeaponSlot(forgeInventory, WEAPON_SLOT, 80, 22));
        addSlot(new RunicGemSlot(forgeInventory, RUNIC_GEM_SLOT, 21, 22));
        addSlot(new NetherGemSlot(forgeInventory, NETHER_GEM_SLOT, 139, 22));
        for (int i = 0; i < TABLET_COUNT; i++) {
            addSlot(new TabletSlot(forgeInventory, TABLET_START + i, 17 + i * 18, 56));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        7 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 7 + column * 18, 142));
        }
        addSlot(new PreviewSlot(previewInventory, 0));
    }

    public Inventory getForgeInventory() {
        return forgeInventory;
    }

    public ItemStack getPreviewStack() {
        return previewInventory.getStack(0);
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        if (inventory != forgeInventory || owner.getWorld().isClient() || committing) return;
        loadInsertedWeapon();
        refreshPreview();
    }

    private void loadInsertedWeapon() {
        if (committing || owner.getWorld().isClient()) return;
        ItemStack weapon = forgeInventory.getStack(WEAPON_SLOT);
        if (!weapon.isEmpty() && !editingLoaded && AwakeningApi.usesAwakeningProgression(weapon)) {
            committing = true;
            try {
                ItemStack converted = LegacyUniqueMigration.convertStack(weapon);
                if (converted != weapon) {
                    forgeInventory.setStack(WEAPON_SLOT, converted);
                    weapon = converted;
                }
                AwakeningFormRegistry.ensureInitialized(weapon);
                originalStack = weapon.copy();
                originalLevel = AwakeningApi.getLevel(weapon);
                extractWeaponContents(weapon);
                editingLoaded = true;
            } finally {
                committing = false;
            }
        } else if (weapon.isEmpty()) {
            editingLoaded = false;
            originalStack = ItemStack.EMPTY;
            originalLevel = 0;
            previewInventory.setStack(0, ItemStack.EMPTY);
        }
    }

    private void extractWeaponContents(ItemStack weapon) {
        int level = AwakeningApi.getLevel(weapon);
        GemPowerComponent gems = weapon.getOrDefault(
                ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.DEFAULT);
        for (int i = 0; i < TABLET_COUNT; i++) {
            forgeInventory.setStack(TABLET_START + i,
                    i < level ? new ItemStack(ItemsRegistry.RUNIC_TABLET.get()) : ItemStack.EMPTY);
        }
        if (!gems.runicPower().value().isEmpty()) {
            ItemStack runic = new ItemStack(ItemsRegistry.RUNEFUSED_GEM.get());
            runic.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.runic(gems.runicPower()));
            forgeInventory.setStack(RUNIC_GEM_SLOT, runic);
        }
        if (!gems.netherPower().value().isEmpty()) {
            ItemStack nether = new ItemStack(ItemsRegistry.NETHERFUSED_GEM.get());
            nether.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.nether(gems.netherPower()));
            forgeInventory.setStack(NETHER_GEM_SLOT, nether);
        }
        weapon.set(ComponentTypeRegistry.GEM_POWER.get(),
                GemPowerComponent.createEmpty(gems.hasRunicPower(), gems.hasNetherPower()));
        AwakeningApi.setLevel(weapon, 0);
    }

    private void commitWeapon() {
        if (owner.getWorld().isClient()) return;
        ItemStack weapon = forgeInventory.getStack(WEAPON_SLOT);
        if (committing || weapon.isEmpty() || !editingLoaded) return;
        ItemStack sourceSnapshot = originalStack.isEmpty() ? weapon.copy() : originalStack.copy();
        ItemStack committedStack = ItemStack.EMPTY;
        int committedOriginalLevel = originalLevel;
        int targetLevel = originalLevel;
        boolean changed = false;
        committing = true;
        try {
            ItemStack preview = previewInventory.getStack(0);
            if (preview.isEmpty()) {
                preview = buildConfiguredPreview(weapon);
            }
            if (!preview.isEmpty()) {
                committedStack = preview.copyWithCount(weapon.getCount());
                targetLevel = AwakeningApi.getLevel(committedStack);
                changed = !ItemStack.areEqual(sourceSnapshot, committedStack);
                forgeInventory.setStack(WEAPON_SLOT, committedStack);
            }
            for (int i = 1; i < FORGE_SLOT_COUNT; i++) {
                forgeInventory.setStack(i, ItemStack.EMPTY);
            }
            editingLoaded = false;
            originalStack = ItemStack.EMPTY;
            originalLevel = 0;
            previewInventory.setStack(0, committedStack.isEmpty()
                    ? forgeInventory.getStack(WEAPON_SLOT).copy()
                    : committedStack.copy());
        } finally {
            committing = false;
        }
        if (changed && !committedStack.isEmpty() && owner instanceof ServerPlayerEntity serverPlayer) {
            AwakeningFormRegistry.notifyCommitted(
                    sourceSnapshot,
                    committedStack,
                    committedOriginalLevel,
                    targetLevel,
                    serverPlayer,
                    forgePos
            );
        }
        sendContentUpdates();
    }

    private void refreshPreview() {
        if (owner.getWorld().isClient() || committing) return;
        ItemStack weapon = forgeInventory.getStack(WEAPON_SLOT);
        if (weapon.isEmpty()) {
            previewInventory.setStack(0, ItemStack.EMPTY);
            sendContentUpdates();
            return;
        }

        committing = true;
        try {
            previewInventory.setStack(0, editingLoaded
                    ? buildConfiguredPreview(weapon)
                    : weapon.copy());
            forgeInventory.markDirty();
        } finally {
            committing = false;
        }
        sendContentUpdates();
    }

    private ItemStack buildConfiguredPreview(ItemStack weapon) {
        if (weapon.isEmpty()) return ItemStack.EMPTY;
        ItemStack preview = weapon.copy();
        int level = 0;
        for (int i = 0; i < TABLET_COUNT; i++) {
            if (forgeInventory.getStack(TABLET_START + i)
                    .isOf(ItemsRegistry.RUNIC_TABLET.get())) {
                level++;
            }
        }

        GemPowerComponent sockets = preview.getOrDefault(
                ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.DEFAULT);
        GemPowerComponent runicGem = identifyGem(
                forgeInventory.getStack(RUNIC_GEM_SLOT), PowerType.RUNEFUSED);
        GemPowerComponent netherGem = identifyGem(
                forgeInventory.getStack(NETHER_GEM_SLOT), PowerType.NETHER);
        preview.set(ComponentTypeRegistry.GEM_POWER.get(), new GemPowerComponent(
                sockets.hasRunicPower(),
                sockets.hasNetherPower(),
                sockets.hasRunicPower() && !runicGem.runicPower().value().isEmpty()
                        ? runicGem.runicPower() : GemPowerRegistry.EMPTY,
                sockets.hasNetherPower() && !netherGem.netherPower().value().isEmpty()
                        ? netherGem.netherPower() : GemPowerRegistry.EMPTY
        ));
        AwakeningApi.setLevel(preview, level);
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            preview = AwakeningFormRegistry.resolvePreview(
                    preview,
                    originalStack.isEmpty() ? weapon : originalStack,
                    originalLevel,
                    level,
                    serverPlayer,
                    forgePos
            );
        }
        return preview;
    }

    private GemPowerComponent identifyGem(ItemStack stack, PowerType type) {
        if (stack.isEmpty()) return GemPowerComponent.DEFAULT;
        GemPowerComponent component = stack.get(ComponentTypeRegistry.GEM_POWER.get());
        if (component != null && !component.isEmpty()) return component;
        GemPowerComponent identified = type == PowerType.NETHER
                ? GemPowerComponent.nether(GemPowerRegistry.gemRandomPower(PowerType.NETHER))
                : GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNEFUSED));
        stack.set(ComponentTypeRegistry.GEM_POWER.get(), identified);
        return identified;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size() || slotIndex == PREVIEW_SLOT) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        if (slotIndex == WEAPON_SLOT) {
            commitWeapon();
        }
        ItemStack source = slot.getStack();
        ItemStack copy = source.copy();
        if (slotIndex < FORGE_SLOT_COUNT) {
            if (!insertItem(source, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_SLOT_START
                && slotIndex < PLAYER_SLOT_END
                && AwakeningApi.usesAwakeningProgression(source)) {
            if (!insertItem(source, WEAPON_SLOT, WEAPON_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (slotIndex >= PLAYER_SLOT_START
                && slotIndex < PLAYER_SLOT_END
                && source.isOf(ItemsRegistry.RUNEFUSED_GEM.get())) {
            if (!insertItem(source, RUNIC_GEM_SLOT, RUNIC_GEM_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (slotIndex >= PLAYER_SLOT_START
                && slotIndex < PLAYER_SLOT_END
                && source.isOf(ItemsRegistry.NETHERFUSED_GEM.get())) {
            if (!insertItem(source, NETHER_GEM_SLOT, NETHER_GEM_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (slotIndex >= PLAYER_SLOT_START
                && slotIndex < PLAYER_SLOT_END
                && source.isOf(ItemsRegistry.RUNIC_TABLET.get())) {
            if (!insertItem(source, TABLET_START, TABLET_START + TABLET_COUNT, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (source.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient()) {
            commitWeapon();
            dropInventory(player, forgeInventory);
            forgeInventory.onClose(player);
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getWorld().getBlockState(forgePos).isOf(BlocksRegistry.RUNIC_FORGE.get())
                && player.squaredDistanceTo(forgePos.getX() + 0.5D,
                forgePos.getY() + 0.5D, forgePos.getZ() + 0.5D) <= 64.0D;
    }

    private final class WeaponSlot extends Slot {
        private WeaponSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return getStack().isEmpty() && AwakeningApi.usesAwakeningProgression(stack);
        }

        @Override
        public void setStack(ItemStack stack) {
            super.setStack(stack);
            loadInsertedWeapon();
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            commitWeapon();
            super.onTakeItem(player, stack);
        }

        @Override
        public ItemStack takeStack(int amount) {
            commitWeapon();
            return super.takeStack(amount);
        }
    }

    private final class RunicGemSlot extends Slot {
        private RunicGemSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override public boolean canInsert(ItemStack stack) {
            return stack.isOf(ItemsRegistry.RUNEFUSED_GEM.get()) && socketAvailable(true);
        }
        @Override public int getMaxItemCount() { return 1; }
    }

    private final class NetherGemSlot extends Slot {
        private NetherGemSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override public boolean canInsert(ItemStack stack) {
            return stack.isOf(ItemsRegistry.NETHERFUSED_GEM.get()) && socketAvailable(false);
        }
        @Override public int getMaxItemCount() { return 1; }
    }

    private boolean socketAvailable(boolean runic) {
        ItemStack weapon = forgeInventory.getStack(WEAPON_SLOT);
        if (weapon.isEmpty()) return false;
        GemPowerComponent sockets = weapon.getOrDefault(
                ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.DEFAULT);
        return runic ? sockets.hasRunicPower() : sockets.hasNetherPower();
    }

    private final class TabletSlot extends Slot {
        private TabletSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
        @Override public boolean canInsert(ItemStack stack) {
            return !forgeInventory.getStack(WEAPON_SLOT).isEmpty()
                    && stack.isOf(ItemsRegistry.RUNIC_TABLET.get());
        }
        @Override public int getMaxItemCount() { return 1; }
    }

    private static final class PreviewSlot extends Slot {
        private PreviewSlot(Inventory inventory, int index) {
            super(inventory, index, -1000, -1000);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }
}
