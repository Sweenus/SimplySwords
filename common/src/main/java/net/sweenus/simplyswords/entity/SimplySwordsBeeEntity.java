package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimplySwordsBeeEntity extends BeeEntity implements Tameable {
    public UUID ownerUuid;
    public static int lifespan = 200;
    public SimplySwordsBeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSimplyBeeAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 1.6f)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6f)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }
    @Override
    public void tick() {
        this.setInvulnerable(true);
        if (hasStung() || this.age > lifespan)
            this.discard();

        super.tick();
    }

    @Override
    public boolean tryAttack(Entity target) {
        target.timeUntilRegen = 0;
        return super.tryAttack(target);
    }

    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    @Override
    public EntityView method_48926() {
        return null;
    }

    public void setOwner(LivingEntity livingEntity) {
        this.ownerUuid = livingEntity.getUuid();
    }
}
