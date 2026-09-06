package net.sweenus.simplyswords.mixin;

import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.effect.FlameSeedEffect;
import net.sweenus.simplyswords.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void simplyswords$tickFieldManagers(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;
        if (MasteryAbsorptionTracker.hasActive(world)) {
            MasteryAbsorptionTracker.sweep(world);
        }
        if (EmberbladeAbilityManager.hasActive(world)) {
            EmberbladeAbilityManager.tick(world);
        }
        if (FrostfallIceSpikeFieldManager.hasActive(world)) {
            FrostfallIceSpikeFieldManager.tick(world);
        }
        if (IcewhisperCometManager.hasActive(world)) {
            IcewhisperCometManager.tick(world);
        }
        if (MagispearAbilityManager.hasWork(world)) {
            MagispearAbilityManager.tick(world);
        }
        if (BrimstoneClaymoreAbilityManager.hasActive(world)) {
            BrimstoneClaymoreAbilityManager.tick(world);
        }
        if (StormscaleLightningRodManager.hasActive(world)) {
            StormscaleLightningRodManager.tick(world);
        }
        if (IonboundStormscaleAbilityManager.hasActive(world)) {
            IonboundStormscaleAbilityManager.tick(world);
        }
        if (DevourerAbilityManager.hasActive(world)) {
            DevourerAbilityManager.tick(world);
        }
        if (DevourerStainManager.hasActive(world)) {
            DevourerStainManager.tick(world);
        }
        if (GloamStainManager.hasActive(world)) {
            GloamStainManager.tick(world);
        }
        if (GloamMechanicsManager.hasActive(world)) {
            GloamMechanicsManager.tick(world);
        }
        if (WraithmawAbilityManager.hasActive(world)) {
            WraithmawAbilityManager.tick(world);
        }
        if (GloampiercerAbilityManager.hasActive(world)) {
            GloampiercerAbilityManager.tick(world);
        }
        if (WraithfangAbilityManager.hasActive(world)) {
            WraithfangAbilityManager.tick(world);
        }
        if (WickpiercerMasteryStateManager.hasActive(world)) {
            WickpiercerMasteryStateManager.tick(world);
        }
        if (SoulstalkerAbilityManager.hasActive(world)) {
            SoulstalkerAbilityManager.tick(world);
        }
        if (RiftmaneAbilityManager.hasActive(world)) {
            RiftmaneAbilityManager.tick(world);
        }
        if (DawnquiverAbilityManager.hasActive(world)) {
            DawnquiverAbilityManager.tick(world);
        }
        if (DevourerReprisalManager.hasActive(world)) {
            DevourerReprisalManager.tick(world);
        }
        if (BloodwakeAbilityManager.hasActive(world)) {
            BloodwakeAbilityManager.tick(world);
        }
        if (BloodStainManager.hasActive(world)) {
            BloodStainManager.tick(world);
        }
        if (ArcanethystAssaultManager.hasActive(world)) {
            ArcanethystAssaultManager.tick(world);
        }
        if (SoulrenderMarkVisualManager.hasActive(world)) {
            SoulrenderMarkVisualManager.tick(world);
        }
        if (WatcherAbilityManager.hasActive(world)) {
            WatcherAbilityManager.tick(world);
        }
        if (FlamewindVisualManager.hasActive(world)) {
            FlamewindVisualManager.tick(world);
        }
        if (FlameSeedEffect.hasPendingDeathDetonations(world)) {
            FlameSeedEffect.tickPendingDeathDetonations(world);
        }
        if (WhisperwindVisualManager.hasActive(world)) {
            WhisperwindVisualManager.tick(world);
        }
        if (net.sweenus.simplyswords.world.DreadwhisperTrailManager.hasActive(world)) {
            net.sweenus.simplyswords.world.DreadwhisperTrailManager.tick(world);
        }
        if (DreadwhisperAbilityManager.hasActive(world)) {
            DreadwhisperAbilityManager.tick(world);
        }
        if (EmberlashSmoulderVisualManager.hasActive(world)) {
            EmberlashSmoulderVisualManager.tick(world);
        }
        if (ImplicitStatusVisualManager.hasActive(world)) {
            ImplicitStatusVisualManager.tick(world);
        }
        if (HivemindSwarmManager.hasActive(world)) {
            HivemindSwarmManager.tick(world);
        }
        if (ShadowstingShadowDanceManager.hasPendingCloneStrikes(world)) {
            ShadowstingShadowDanceManager.tickCloneStrikes(world);
        }
        if (TemporaryWorldLightManager.hasActive(world)) {
            TemporaryWorldLightManager.tick(world);
        }
        if (VerdantTrailManager.hasActive(world)) {
            VerdantTrailManager.tick(world);
        }
        if (FaultlineSunderManager.hasActive(world)) {
            FaultlineSunderManager.tick(world);
        }
        if (LivyatanWaveManager.hasActive(world)) {
            LivyatanWaveManager.tick(world);
        }
        if (LivingEntityAbilityMovementManager.hasActive(world)) {
            LivingEntityAbilityMovementManager.tick(world);
        }
        if (ThunderbrandAbilityManager.hasActive(world)) {
            ThunderbrandAbilityManager.tick(world);
        }
        if (MagibladeAbilityManager.hasActive(world)) {
            MagibladeAbilityManager.tick(world);
        }
        if (MagiscytheMasteryManager.hasActive(world)) {
            MagiscytheMasteryManager.tickWorld(world);
        }
        if (StormsEdgeAbilityManager.hasActive(world)) {
            StormsEdgeAbilityManager.tick(world);
        }
        if (StarsEdgeAbilityManager.hasActive(world)) {
            StarsEdgeAbilityManager.tick(world);
        }
        if (MoltenEdgeAbilityManager.hasActive(world)) {
            MoltenEdgeAbilityManager.tick(world);
        }
        if (TwistedBladeAbilityManager.hasActive(world)) {
            TwistedBladeAbilityManager.tick(world);
        }
        if (DeathKnellAbilityManager.hasActive(world)) {
            DeathKnellAbilityManager.tick(world);
        }
        if (MjolnirStormManager.hasActive(world)) {
            MjolnirStormManager.tick(world);
        }
        if (HearthflameAbilityManager.hasActive(world)) {
            HearthflameAbilityManager.tick(world);
        }
        if (SoulPyreAbilityManager.hasActive(world)) {
            SoulPyreAbilityManager.tick(world);
        }
        if (CaelestisBreachManager.hasActive(world)) {
            CaelestisBreachManager.tick(world);
        }
        if (WaxweaverEncasementManager.hasActive(world)) {
            WaxweaverEncasementManager.tick(world);
        }
        if (BramblethornAbilityManager.hasActive(world)) {
            BramblethornAbilityManager.tick(world);
        }
        if (NatureSwarmMasteryCombatManager.hasScheduled(world)) {
            NatureSwarmMasteryCombatManager.tick(world);
        }
        if (DeathShadowBloodMasteryCombatManager.hasScheduled(world)) {
            DeathShadowBloodMasteryCombatManager.tick(world);
        }
        if (ArcaneCosmicMasteryCombatManager.hasScheduled(world)) {
            ArcaneCosmicMasteryCombatManager.tick(world);
        }
        if (EvocationFangManager.hasActive(world)) {
            EvocationFangManager.tick(world);
        }
        RevivalCandleVisualManager.tickWorld(world);
        SoulkeeperLanternManager.tickWorld(world);
        WeaponAbilityCooldownManager.tick(world);
        RevivalCooldownManager.tick(world);
    }
}
