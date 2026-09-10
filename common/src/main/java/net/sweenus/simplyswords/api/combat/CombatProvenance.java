package net.sweenus.simplyswords.api.combat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

public record CombatProvenance(UUID identity, UUID player, UUID weapon, Identifier group,
                               String destinationKind, UUID destination, Identifier item,
                               int weaponPercent, int origins, int deliveries) {
    public static final int MELEE = 1;
    public static final int ABILITY = 2;
    public static final int PROJECTILE = 4;
    public static final int SUMMON = 8;
    public static final int DAMAGE_OVER_TIME = 16;
    public static final Codec<CombatProvenance> CODEC = NbtCompound.CODEC.xmap(CombatProvenance::read, CombatProvenance::write);

    public CombatProvenance {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(player);
        Objects.requireNonNull(weapon);
        Objects.requireNonNull(group);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(item);
        if (!destinationKind.equals("WEAPON") && !destinationKind.equals("PLAYER")) throw new IllegalArgumentException("Unknown destination");
        if (weaponPercent < 0 || weaponPercent > 10000 || origins < 1 || origins > 31 || deliveries < 0 || deliveries > 31) {
            throw new IllegalArgumentException("Invalid combat provenance");
        }
    }

    public CombatProvenance deliveredBy(int source) {
        return new CombatProvenance(identity, player, weapon, group, destinationKind, destination, item,
                weaponPercent, origins, deliveries | source);
    }

    public NbtCompound write() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("version", 1);
        nbt.putUuid("identity", identity);
        nbt.putUuid("player", player);
        nbt.putUuid("weapon", weapon);
        nbt.putString("group", group.toString());
        nbt.putString("kind", destinationKind);
        nbt.putUuid("destination", destination);
        nbt.putString("item", item.toString());
        nbt.putInt("percent", weaponPercent);
        nbt.putInt("origins", origins);
        nbt.putInt("deliveries", deliveries);
        return nbt;
    }

    public static CombatProvenance read(NbtCompound nbt) {
        if (nbt.getInt("version") != 1) throw new IllegalArgumentException("Unknown combat provenance version");
        return new CombatProvenance(nbt.getUuid("identity"), nbt.getUuid("player"), nbt.getUuid("weapon"),
                Identifier.of(nbt.getString("group")), nbt.getString("kind"), nbt.getUuid("destination"),
                Identifier.of(nbt.getString("item")), nbt.getInt("percent"), nbt.getInt("origins"), nbt.getInt("deliveries"));
    }
}
