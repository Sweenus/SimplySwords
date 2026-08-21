package net.sweenus.simplyswords.compat;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;

public record SpellSchoolDisplay(Identifier schoolId, Text name) {
    public SpellSchoolDisplay {
        Objects.requireNonNull(schoolId, "schoolId");
        Objects.requireNonNull(name, "name");
    }
}
