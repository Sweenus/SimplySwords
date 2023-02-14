package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class RunicSwordItem extends SwordItem {
    public RunicSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings.fireproof());
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {

        if(stack.getOrCreateNbt().getInt("runic_power") == 0) {

            int choose = (int) (Math.random() * 100);
            stack.getOrCreateNbt().putInt("runic_power", choose);
        }

        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            HelperMethods.playHitSounds(attacker, target);

            //FREEZE
            if (stack.getOrCreateNbt().getInt("runic_power") <= 10 && stack.getOrCreateNbt().getInt("runic_power") >= 1) {

                int fhitchance = (int) SimplySwordsConfig.getFloatValue("freeze_chance");
                int fduration = (int) SimplySwordsConfig.getFloatValue("freeze_duration");
                int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 1), attacker);

                if (attacker.getRandom().nextInt(100) <= fhitchance) {
                    target.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), fduration, 1), attacker);
                }
            }
            //WILDFIRE
            if (stack.getOrCreateNbt().getInt("runic_power") > 10 && stack.getOrCreateNbt().getInt("runic_power") <= 20) {
                int phitchance = (int) SimplySwordsConfig.getFloatValue("wildfire_chance");
                int pduration = (int) SimplySwordsConfig.getFloatValue("wildfire_duration");

                if (attacker.getRandom().nextInt(100) <= phitchance) {
                    target.addStatusEffect(new StatusEffectInstance(EffectRegistry.WILDFIRE.get(), pduration, 3), attacker);
                }
            }
            //SLOW
            if (stack.getOrCreateNbt().getInt("runic_power") > 20 && stack.getOrCreateNbt().getInt("runic_power") <= 30) {
                int shitchance = (int) SimplySwordsConfig.getFloatValue("slowness_chance");
                int sduration = (int) SimplySwordsConfig.getFloatValue("slowness_duration");

                if (attacker.getRandom().nextInt(100) <= shitchance) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, sduration, 3), attacker);
                }
            }
            //SPEED
            if (stack.getOrCreateNbt().getInt("runic_power") > 30 && stack.getOrCreateNbt().getInt("runic_power") <= 40) {
                int shitchance = (int) SimplySwordsConfig.getFloatValue("speed_chance");
                int sduration = (int) SimplySwordsConfig.getFloatValue("speed_duration");

                if (attacker.getRandom().nextInt(100) <= shitchance) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, sduration, 1), attacker);
                }
            }
            //LEVITATION
            if (stack.getOrCreateNbt().getInt("runic_power") > 40 && stack.getOrCreateNbt().getInt("runic_power") <= 50) {
                int lhitchance = (int) SimplySwordsConfig.getFloatValue("levitation_chance");
                int lduration = (int) SimplySwordsConfig.getFloatValue("levitation_duration");

                if (attacker.getRandom().nextInt(100) <= lhitchance) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration, 3), attacker);
                }
            }
            //ZEPHYR
            if (stack.getOrCreateNbt().getInt("runic_power") > 50 && stack.getOrCreateNbt().getInt("runic_power") <= 60) {
                int lhitchance = (int) SimplySwordsConfig.getFloatValue("zephyr_chance");
                int lduration = (int) SimplySwordsConfig.getFloatValue("zephyr_duration");

                if (attacker.getRandom().nextInt(100) <= lhitchance) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, lduration, 3), attacker);
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 1), attacker);
                }
            }
            //SHIELDING
            if (stack.getOrCreateNbt().getInt("runic_power") > 60 && stack.getOrCreateNbt().getInt("runic_power") <= 70) {
                int lhitchance = (int) SimplySwordsConfig.getFloatValue("shielding_chance");
                int lduration = (int) SimplySwordsConfig.getFloatValue("shielding_duration");

                if (attacker.getRandom().nextInt(100) <= lhitchance) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, lduration, 1), attacker);
                }
            }
            //STONESKIN
            if (stack.getOrCreateNbt().getInt("runic_power") > 70 && stack.getOrCreateNbt().getInt("runic_power") <= 75) {
                int lhitchance = (int) SimplySwordsConfig.getFloatValue("stoneskin_chance");
                int lduration = (int) SimplySwordsConfig.getFloatValue("stoneskin_duration");

                if (attacker.getRandom().nextInt(100) <= lhitchance) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, lduration, 2), attacker);
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, lduration, 1), attacker);
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 1), attacker);
                }
            }
            //TRAILBLAZE
            if (stack.getOrCreateNbt().getInt("runic_power") > 80 && stack.getOrCreateNbt().getInt("runic_power") <= 85) {
                int lhitchance = (int) SimplySwordsConfig.getFloatValue("trailblaze_chance");
                int lduration = (int) SimplySwordsConfig.getFloatValue("trailblaze_duration");

                if (attacker.getRandom().nextInt(100) <= lhitchance) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration, 3), attacker);
                    attacker.setOnFireFor(lduration / 20);
                }
            }
            //WEAKEN
            if (stack.getOrCreateNbt().getInt("runic_power") > 90 && stack.getOrCreateNbt().getInt("runic_power") <= 95) {
                int lhitchance = (int) SimplySwordsConfig.getFloatValue("weaken_chance");
                int lduration = (int) SimplySwordsConfig.getFloatValue("weaken_duration");

                if (attacker.getRandom().nextInt(100) <= lhitchance) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, lduration, 1), attacker);
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration, 1), attacker);
                }
            }
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

            int choose = (int) (Math.random() * 100);
            stack.getOrCreateNbt().putInt("runic_power", choose);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        if(itemStack.getOrCreateNbt().getInt("runic_power") == 0) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") <= 10 && itemStack.getOrCreateNbt().getInt("runic_power") >= 1) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 10 && itemStack.getOrCreateNbt().getInt("runic_power") <= 20) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 20 && itemStack.getOrCreateNbt().getInt("runic_power") <= 30) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 30 && itemStack.getOrCreateNbt().getInt("runic_power") <= 40) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 40 && itemStack.getOrCreateNbt().getInt("runic_power") <= 50) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 50 && itemStack.getOrCreateNbt().getInt("runic_power") <= 60) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 60 && itemStack.getOrCreateNbt().getInt("runic_power") <= 70) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 70 && itemStack.getOrCreateNbt().getInt("runic_power") <= 75) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 75 && itemStack.getOrCreateNbt().getInt("runic_power") <= 80) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 80 && itemStack.getOrCreateNbt().getInt("runic_power") <= 85) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 85 && itemStack.getOrCreateNbt().getInt("runic_power") <= 90) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 90 && itemStack.getOrCreateNbt().getInt("runic_power") <= 95) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3"));

        }
        if(itemStack.getOrCreateNbt().getInt("runic_power") > 95 && itemStack.getOrCreateNbt().getInt("runic_power") <= 100) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3"));

        }

    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity.age % 4 == 0 && SimplySwordsConfig.getBooleanValue("enable_passive_particles") && (entity instanceof PlayerEntity player)) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                float randomx = (float) (Math.random() * 6);
                float randomz = (float) (Math.random() * 6);

                world.addParticle(ParticleTypes.ENCHANT, player.getX() + player.getHandPosOffset(this).getX(),
                        player.getY() + player.getHandPosOffset(this).getY() + 1.3,
                        player.getZ() + player.getHandPosOffset(this).getZ(),
                        -3 + randomx, 0.0, -3 + randomz);

            }
        }
        if (!world.isClient && (entity instanceof PlayerEntity player)) {

            //UNSTABLE
            if(stack.getOrCreateNbt().getInt("runic_power") > 95 && stack.getOrCreateNbt().getInt("runic_power") <= 100) {
                if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                    int lduration = (int) SimplySwordsConfig.getFloatValue("unstable_duration");
                    int lfrequency = (int) SimplySwordsConfig.getFloatValue("unstable_frequency");
                    if (player.age % lfrequency == 0) {
                        int random = (int) (Math.random() * 100);
                        if (random >= 0 && random < 10)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, lduration));
                        if (random >= 10 && random < 20)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, lduration));
                        if (random >= 20 && random < 30)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, lduration));
                        if (random >= 30 && random < 40)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, lduration));
                        if (random >= 40 && random < 50)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, lduration));
                        if (random >= 50 && random < 60)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, lduration));
                        if (random >= 60 && random < 70)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, lduration));
                        if (random >= 70 && random < 80)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, lduration));
                        if (random >= 80 && random < 90)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, lduration));
                        if (random >= 90 && random < 95)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, lduration));
                        if (random >= 95 && random < 100)
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, lduration));
                    }
                }
            }
            //ACTIVE DEFENCE
            if(stack.getOrCreateNbt().getInt("runic_power") > 85 && stack.getOrCreateNbt().getInt("runic_power") <= 90) {
                if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                    int lfrequency = (int) SimplySwordsConfig.getFloatValue("active_defence_frequency");
                    if (player.age % lfrequency == 0) {
                        int sradius = (int) SimplySwordsConfig.getFloatValue("active_defence_radius");
                        int vradius = (int) (SimplySwordsConfig.getFloatValue("active_defence_radius") / 2);
                        double x = player.getX();
                        double y = player.getY();
                        double z = player.getZ();
                        ServerWorld sworld = (ServerWorld) player.world;
                        Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
                        for (Entity entities : sworld.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                            if (entities != null) {
                                if (entities instanceof LivingEntity le && player.getInventory().contains(Items.ARROW.getDefaultStack()) && HelperMethods.checkFriendlyFire(le, player)) {

                                    var arrowstack = player.getInventory().getSlotWithStack(Items.ARROW.getDefaultStack());
                                    var astack = player.getInventory().getStack(arrowstack);
                                    int randomc = (int) (Math.random() * 100);
                                    if (randomc < 15)
                                        astack.setCount(astack.getCount()-1);

                                    if (le.distanceTo(player) < sradius) {
                                        double ex = le.getX();
                                        double ey = le.getY();
                                        double ez = le.getZ();
                                        BlockPos position = (player.getBlockPos());
                                        Vec3d rotation = le.getRotationVec(1f);
                                        Vec3d newPos = player.getPos().add(rotation);

                                        ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, (ServerWorld) world);
                                        arrow.updatePosition(player.getX(), (player.getY() + 1.5), player.getZ());
                                        arrow.setOwner(player);
                                        arrow.setVelocity( le.getX() - player.getX(), (le.getY() - player.getY()) - 1, le.getZ() - player.getZ());
                                        sworld.spawnEntity(arrow);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //FROST WARD
            if(stack.getOrCreateNbt().getInt("runic_power") > 75 && stack.getOrCreateNbt().getInt("runic_power") <= 80) {
                if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                    int lfrequency = (int) SimplySwordsConfig.getFloatValue("frostward_frequency");
                    int lduration = (int) SimplySwordsConfig.getFloatValue("frostward_slow_duration");
                    if (player.age % lfrequency == 0) {
                        int sradius = (int) SimplySwordsConfig.getFloatValue("frostward_radius");
                        int vradius = (int) (SimplySwordsConfig.getFloatValue("frostward_radius") / 2);
                        double x = player.getX();
                        double y = player.getY();
                        double z = player.getZ();
                        ServerWorld sworld = (ServerWorld) player.world;
                        Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
                        for (Entity entities : sworld.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                            if (entities != null) {
                                if (entities instanceof LivingEntity le && HelperMethods.checkFriendlyFire(le, player)) {

                                    if (le.distanceTo(player) < sradius) {
                                        double ex = le.getX();
                                        double ey = le.getY();
                                        double ez = le.getZ();
                                        BlockPos position = (player.getBlockPos());
                                        Vec3d rotation = le.getRotationVec(1f);
                                        Vec3d newPos = player.getPos().add(rotation);

                                        SnowballEntity snowball = new SnowballEntity(EntityType.SNOWBALL, (ServerWorld) world);
                                        snowball.updatePosition(player.getX(), (player.getY() + 1.5), player.getZ());
                                        snowball.setOwner(player);
                                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, lduration));
                                        snowball.setVelocity( le.getX() - player.getX(), (le.getY() - player.getY()) - 1, le.getZ() - player.getZ());
                                        sworld.spawnEntity(snowball);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            super.inventoryTick(stack, world, entity, slot, selected);
        }
    }


}
