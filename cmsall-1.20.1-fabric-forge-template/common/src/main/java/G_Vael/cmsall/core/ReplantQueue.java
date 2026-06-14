package G_Vael.cmsall.core;

import java.util.ArrayList;
import java.util.List;

/** Deferred auto-replant (paired with Replanter). */
public final class ReplantQueue {

    private static final List<Replanter> PENDING = new ArrayList<>();

    private ReplantQueue() {
    }

    static void schedule(Replanter replanter) {
        if (replanter != null) {
            PENDING.add(replanter);
        }
    }

    /** Drops all pending replants (server shutdown). */
    public static void clear() {
        PENDING.clear();
    }

    /** Called every server tick. */
    public static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Replanter r : PENDING) {
            r.finish();
        }
        PENDING.clear();
    }
}
