package net.sweenus.simplyswords.item.custom;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrimstoneClaymoreItemTest {

    @Test
    void usesStrictPercentageProcChance() {
        assertEquals(0, passingRolls(0));
        assertEquals(15, passingRolls(15));
        assertEquals(100, passingRolls(100));
    }

    private static long passingRolls(int chance) {
        return IntStream.range(0, 100)
                .filter(roll -> BrimstoneClaymoreItem.passesEruptionRoll(chance, roll))
                .count();
    }
}
