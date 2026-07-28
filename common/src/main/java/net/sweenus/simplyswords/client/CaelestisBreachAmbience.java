package net.sweenus.simplyswords.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.CaelestisBreachVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;

public final class CaelestisBreachAmbience {

    private static BreachAmbienceSound activeSound;
    private static ClientWorld activeWorld;
    private static boolean changingWorld;

    private CaelestisBreachAmbience() {
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(CaelestisBreachAmbience::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world != activeWorld) {
            activeWorld = client.world;
            if (activeSound != null) {
                activeSound.setTargetStrength(0.0F);
                changingWorld = true;
            }
        }
        if (changingWorld) {
            if (activeSound != null && !activeSound.isDone()) {
                return;
            }
            activeSound = null;
            changingWorld = false;
        }

        float strength = findStrongestBreach(client);
        if (strength > 0.0F) {
            if (activeSound == null || activeSound.isDone()) {
                activeSound = new BreachAmbienceSound();
                client.getSoundManager().play(activeSound);
            }
            activeSound.setTargetStrength(strength);
        } else if (activeSound != null) {
            activeSound.setTargetStrength(0.0F);
            if (activeSound.isDone()) {
                activeSound = null;
            }
        }
    }

    private static float findStrongestBreach(MinecraftClient client) {
        if (client.world == null || client.gameRenderer == null) {
            return 0.0F;
        }
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        double horizontalSearch = Math.max(2.0, Config.uniqueEffects.caelestis.maxRadius + 2.0);
        double verticalSearch = Math.max(2.0, Config.uniqueEffects.caelestis.verticalRange + 2.0);
        Box search = new Box(cameraPos, cameraPos).expand(
                horizontalSearch, verticalSearch, horizontalSearch);
        float strongest = 0.0F;
        for (CaelestisBreachVisualEntity breach : client.world.getEntitiesByClass(
                CaelestisBreachVisualEntity.class, search, entity -> entity.getRadius() > 0.05F)) {
            if (breach.getPhase() == CaelestisBreachVisualEntity.PHASE_COLLAPSING) {
                continue;
            }
            double dx = cameraPos.x - breach.getX();
            double dz = cameraPos.z - breach.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > breach.getRadius()
                    || Math.abs(cameraPos.y - breach.getY()) > breach.getVerticalRange()) {
                continue;
            }
            float edgeStrength = MathHelper.clamp(
                    (float) ((breach.getRadius() - distance) / 2.25), 0.0F, 1.0F);
            float radiusStrength = MathHelper.clamp(
                    breach.getRadius() / breach.getMaxRadius(), 0.0F, 1.0F);
            float strength = (0.55F + edgeStrength * 0.45F)
                    * (0.5F + radiusStrength * 0.5F);
            strongest = Math.max(strongest, strength);
        }
        return strongest;
    }

    private static final class BreachAmbienceSound extends MovingSoundInstance {

        private static final float MAX_VOLUME = 0.32F;
        private static final float FADE_STEP = MAX_VOLUME / 10.0F;
        private float targetStrength;

        private BreachAmbienceSound() {
            super(SoundRegistry.CAELESTIS_BREACH_AMBIENCE.get(), SoundCategory.AMBIENT, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.pitch = 0.78F;
            this.volume = 0.001F;
        }

        private void setTargetStrength(float strength) {
            this.targetStrength = MathHelper.clamp(strength, 0.0F, 1.0F);
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }

        @Override
        public void tick() {
            float targetVolume = this.targetStrength * MAX_VOLUME;
            if (this.volume < targetVolume) {
                this.volume = Math.min(targetVolume, this.volume + FADE_STEP);
            } else if (this.volume > targetVolume) {
                this.volume = Math.max(targetVolume, this.volume - FADE_STEP);
            }
            if (this.targetStrength <= 0.0F && this.volume <= 0.0F) {
                this.setDone();
            }
        }
    }
}
