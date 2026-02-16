package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimplySwordsBeeEntity extends BeeEntity {
    public UUID ownerUuid;
    public static int lifespan = 200;
    public SimplySwordsBeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSimplyBeeAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 35.0)
                .add(EntityAttributes.FLYING_SPEED, 1.6f)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.6f)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0);
    }
    @Override
    public void tick() {
        this.setInvulnerable(true);
        if (hasStung() || this.age > lifespan)
            this.discard();

        super.tick();
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        target.timeUntilRegen = 0;
        return super.tryAttack(world, target);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    //I think this is just Entity.getEntityWorld()? What even are mappings
    //@Override
    //public EntityView method_48926() {
        //return this.getEntityWorld();
    //} 1.21

    public void setOwner(LivingEntity livingEntity) {
        this.ownerUuid = livingEntity.getUuid();
    }
}
