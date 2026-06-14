package G_Vael.cmsall.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.item.EntityItem;

/** Drop auto-despawn. */
public final class DespawnTracker {

    private static final class Entry {
        final EntityItem entity;
        final long deadline;

        Entry(EntityItem entity, long deadline) {
            this.entity = entity;
            this.deadline = deadline;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<Entry>();

    private DespawnTracker() {
    }

    public static void register(EntityItem entity, long deadline) {
        ENTRIES.add(new Entry(entity, deadline));
    }

    /** Drops all tracked entries (server shutdown). */
    public static void clear() {
        ENTRIES.clear();
    }

    public static void tick() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.entity.isDead) {
                it.remove();
            } else if (e.entity.world.getTotalWorldTime() >= e.deadline) {
                e.entity.setDead();
                it.remove();
            }
        }
    }
}
