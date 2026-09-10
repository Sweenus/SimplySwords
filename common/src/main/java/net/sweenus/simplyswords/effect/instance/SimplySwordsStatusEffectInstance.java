package net.sweenus.simplyswords.effect.instance;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.UUID;

public class SimplySwordsStatusEffectInstance extends StatusEffectInstance {

    public LivingEntity sourceEntity;
    public int additionalData;
    private float scaledDamage;
    private UUID sourceId;

    public void resolveSource(LivingEntity target) {
        if (target.getWorld().isClient() || sourceId == null || sourceEntity != null) return;
        sourceEntity = null;
        for (var world : target.getServer().getWorlds()) {
            if (world.getEntity(sourceId) instanceof LivingEntity living) {
                sourceEntity = living;
                return;
            }
        }
    }

    public NbtCompound writeSource() {
        var nbt = new NbtCompound();
        UUID id = sourceEntity == null ? sourceId : sourceEntity.getUuid();
        if (id != null) nbt.putUuid("source", id);
        nbt.putInt("additional", additionalData);
        nbt.putFloat("damage", scaledDamage);
        return nbt;
    }

    public void readSource(NbtCompound nbt) {
        sourceEntity = null;
        sourceId = nbt.containsUuid("source") ? nbt.getUuid("source") : null;
        additionalData = nbt.getInt("additional");
        scaledDamage = nbt.getFloat("damage");
        if (!Float.isFinite(scaledDamage)) scaledDamage = 0;
    }

    public SimplySwordsStatusEffectInstance(RegistryEntry<StatusEffect> type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        super(type, duration, amplifier, ambient, showParticles, showIcon);
    }

    public LivingEntity getSourceEntity() {
        if (sourceEntity != null)
            return sourceEntity;

        return null;
    }

    public void setSourceEntity(LivingEntity entity) {
        sourceEntity = entity;
        sourceId = entity == null ? null : entity.getUuid();
    }

    public int getAdditionalData() {
        if (additionalData != 0)
            return additionalData;

        return 0;
    }

    public void setAdditionalData(int data) {
        if (data != 0)
            additionalData = data;
    }

    public float getScaledDamage() {
        return scaledDamage;
    }

    public void setScaledDamage(float scaledDamage) {
        this.scaledDamage = scaledDamage;
    }

}
