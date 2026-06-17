package g_vael.cmsall.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.world.entity.item.ItemEntity;

/** Drop auto-despawn. */
public final class DespawnTracker {

    private static final class Entry {
        final ItemEntity entity;
        final long deadline;

        Entry(ItemEntity entity, long deadline) {
            this.entity = entity;
            this.deadline = deadline;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private DespawnTracker() {
    }

    public static void register(ItemEntity entity, long deadline) {
        ENTRIES.add(new Entry(entity, deadline));
    }

    /** Drops all tracked entries (server shutdown). */
    public static void clear() {
        ENTRIES.clear();
    }

    /** Called every server tick. */
    public static void tick() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.entity.removed) {
                it.remove();
            } else if (e.entity.level.getGameTime() >= e.deadline) {
                e.entity.remove();
                it.remove();
            }
        }
    }
}
