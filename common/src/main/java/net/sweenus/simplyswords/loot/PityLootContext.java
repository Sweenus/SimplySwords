package net.sweenus.simplyswords.loot;

import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;

//
// Per-thread handoff used while a lootable inventory expands its loot table.
// Vanilla container population re-enters generateLoot through getStack and
// setStack, so each invocation needs an independent capture frame.
//
public final class PityLootContext {
    private static final ThreadLocal<Deque<CapturedTable>> CAPTURED_TABLES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private PityLootContext() {
    }

    public static void capture(Identifier table) {
        // ArrayDeque cannot contain null, so wrap the nullable table. Nested
        // generateLoot(null) calls must still occupy their own stack frame.
        CAPTURED_TABLES.get().push(new CapturedTable(table));
    }

    public static Identifier take() {
        Deque<CapturedTable> captures = CAPTURED_TABLES.get();
        if (captures.isEmpty()) {
            CAPTURED_TABLES.remove();
            return null;
        }

        Identifier table = captures.pop().table();
        if (captures.isEmpty()) {
            CAPTURED_TABLES.remove();
        }
        return table;
    }

    private record CapturedTable(Identifier table) {
    }
}
