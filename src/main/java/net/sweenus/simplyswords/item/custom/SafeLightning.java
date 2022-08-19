package net.sweenus.simplyswords.item.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.item.ModItems;

public class SafeLightning extends Entity {

    public SafeLightning(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {

    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        /*this.setFireTicks(this.fireTicks + 1);
        if (this.fireTicks == 0) {
            this.setOnFireFor(8);
        }*/

        var equips = getItemsEquipped();
        if (equips == ModItems.STORMS_EDGE) {
            this.damage(DamageSource.LIGHTNING_BOLT, 0.0F);
            this.sendMessage(Text.translatable("message.simplyswords.testlightning"));
        }
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return null;
    }
}
