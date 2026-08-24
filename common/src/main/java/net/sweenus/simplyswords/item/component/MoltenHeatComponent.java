package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record MoltenHeatComponent(int heat, boolean venting, long ventStartedAt) {

    public static final int MAX_HEAT = 100;
    public static final MoltenHeatComponent DEFAULT = new MoltenHeatComponent(0, false, 0L);

    public static final Codec<MoltenHeatComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("heat").forGetter(MoltenHeatComponent::heat),
                    Codec.BOOL.fieldOf("venting").forGetter(MoltenHeatComponent::venting),
                    Codec.LONG.optionalFieldOf("vent_started_at", 0L).forGetter(MoltenHeatComponent::ventStartedAt)
            ).apply(instance, MoltenHeatComponent::new));

    public static final PacketCodec<RegistryByteBuf, MoltenHeatComponent> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            MoltenHeatComponent::heat,
            PacketCodecs.BOOL,
            MoltenHeatComponent::venting,
            PacketCodecs.VAR_LONG,
            MoltenHeatComponent::ventStartedAt,
            MoltenHeatComponent::new
    );

    public MoltenHeatComponent {
        heat = Math.clamp(heat, 0, MAX_HEAT);
        if (heat <= 0) {
            venting = false;
        }
        ventStartedAt = venting ? Math.max(0L, ventStartedAt) : 0L;
    }

    public MoltenHeatComponent(int heat, boolean venting) {
        this(heat, venting, 0L);
    }

    public MoltenHeatComponent addHeat(int amount) {
        return new MoltenHeatComponent(
                Math.clamp(this.heat + Math.max(0, amount), 0, MAX_HEAT),
                this.venting,
                this.ventStartedAt
        );
    }

    public MoltenHeatComponent startVenting(long worldTime) {
        return new MoltenHeatComponent(this.heat, this.heat > 0, worldTime);
    }

    public int heatAt(long worldTime, int drainPerTick) {
        if (!this.venting) {
            return this.heat;
        }

        int drain = Math.max(1, drainPerTick);
        long elapsed = Math.max(0L, worldTime - this.ventStartedAt);
        long ticksToEmpty = (this.heat + (long) drain - 1L) / drain;
        if (elapsed >= ticksToEmpty) {
            return 0;
        }
        return Math.max(0, this.heat - (int) (elapsed * drain));
    }

    public boolean isVentingAt(long worldTime, int drainPerTick) {
        return this.venting && heatAt(worldTime, drainPerTick) > 0;
    }

    public MoltenHeatComponent normalizedAt(long worldTime, int drainPerTick) {
        int effectiveHeat = heatAt(worldTime, drainPerTick);
        return effectiveHeat <= 0 ? DEFAULT : new MoltenHeatComponent(effectiveHeat, false, 0L);
    }
}
