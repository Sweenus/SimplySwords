package net.sweenus.simplyswords.item.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParryComponentTest {

    @Test
    void chargeGainRecordsItsCapacity() {
        ParryComponent component = ParryComponent.DEFAULT.gainStormCharges(7, 15);

        assertEquals(7, component.stormCharges());
        assertEquals(15, component.stormChargeCapacity());
        assertEquals(15, component.effectiveStormChargeCapacity(10));
        assertTrue(component.parried());
    }

    @Test
    void chargeMutationsPreserveTheCapacity() {
        ParryComponent component = new ParryComponent(true, 7, 12);

        assertEquals(12, component.consumeStormCharge().stormChargeCapacity());
        assertEquals(12, component.resetParry().stormChargeCapacity());
        assertEquals(12, component.resetFull().stormChargeCapacity());
        assertFalse(component.resetParry().parried());
    }

    @Test
    void loweringTheCapacityClampsStoredCharges() {
        ParryComponent component = new ParryComponent(true, 12, 15).withStormChargeCapacity(6);

        assertEquals(6, component.stormCharges());
        assertEquals(6, component.stormChargeCapacity());
        assertTrue(component.parried());
    }

    @Test
    void legacySerializedStateUsesTheConfiguredFallback() {
        NbtCompound legacy = new NbtCompound();
        legacy.putBoolean("parried", false);
        legacy.putInt("succession", 4);

        ParryComponent component = ParryComponent.CODEC.parse(NbtOps.INSTANCE, legacy)
                .result().orElseThrow();

        assertEquals(4, component.stormCharges());
        assertEquals(0, component.stormChargeCapacity());
        assertEquals(10, component.effectiveStormChargeCapacity(10));
    }

    @Test
    void serializedStateRetainsTheSynchronizedCapacity() {
        ParryComponent original = new ParryComponent(true, 11, 15);

        var encoded = ParryComponent.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .result().orElseThrow();
        ParryComponent decoded = ParryComponent.CODEC.parse(NbtOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(original, decoded);
    }
}
