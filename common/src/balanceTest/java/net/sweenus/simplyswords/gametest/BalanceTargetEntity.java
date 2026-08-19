package net.sweenus.simplyswords.gametest;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

final class BalanceTargetEntity extends ZombieEntity {
    private static final float TEST_HEALTH = 1024.0F;
    private final DamageRecorder recorder;
    private final Vec3d anchor;

    BalanceTargetEntity(ServerWorld world, DamageRecorder recorder, Vec3d anchor) {
        super(EntityType.ZOMBIE, world);
        this.recorder = recorder;
        this.anchor = anchor;
        setAiDisabled(true);
        setPersistent();
        setNoGravity(true);
        setSilent(true);
        setBaby(false);
        setPosition(anchor);
        setAttribute(EntityAttributes.GENERIC_MAX_HEALTH, TEST_HEALTH);
        setAttribute(EntityAttributes.GENERIC_ARMOR, 0.0);
        setAttribute(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 0.0);
        setAttribute(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
        setHealth(TEST_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();
        setVelocity(Vec3d.ZERO);
        setPosition(anchor);
        setHealth(TEST_HEALTH);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        float before = getHealth();
        boolean accepted = super.damage(source, amount);
        float lost = Math.max(0.0F, before - getHealth());
        if (accepted && lost > 0.0F) {
            recorder.record(getWorld().getTime(), source, lost);
        }
        if (isRemoved() || !isAlive()) {
            setHealth(TEST_HEALTH);
        }
        return accepted;
    }

    private void setAttribute(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                              double value) {
        EntityAttributeInstance instance = getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
