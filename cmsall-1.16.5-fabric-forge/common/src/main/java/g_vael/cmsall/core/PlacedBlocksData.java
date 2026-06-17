package g_vael.cmsall.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-level persistence of player-placed positions plus the recorded block identity, one ordered map per function. */
public final class PlacedBlocksData extends SavedData {

    public static final String NAME = "cmsall_placed";

    private static final String[] KEYS = { "mine", "cut", "dig" };

    /** insertion-ordered so eviction is FIFO; packed-pos -> stable block-key hash; one per Kind ordinal. */
    @SuppressWarnings("unchecked")
    private final LinkedHashMap<Long, Integer>[] sets = new LinkedHashMap[]{
            new LinkedHashMap<Long, Integer>(), new LinkedHashMap<Long, Integer>(), new LinkedHashMap<Long, Integer>()
    };

    /** rolling snapshot + cursor per kind for the bounded orphan sweep (runtime only). */
    private final long[][] scanBuf = new long[3][];
    private final int[] scanIdx = new int[3];

    public PlacedBlocksData() {
        super(NAME);
    }

    /** Stable identity for a block state: registry-name hashCode (0 = needs-migration sentinel). Rare 32-bit collisions over-protect at most one position and self-heal on break/eviction. */
    public static int blockKey(BlockState state) {
        return Registry.BLOCK.getKey(state.getBlock()).hashCode();
    }

    @Override
    public void load(CompoundTag tag) {
        for (int i = 0; i < KEYS.length; i++) {
            sets[i].clear();
            long[] positions = tag.getLongArray(KEYS[i]);
            int[] keys = tag.getIntArray(KEYS[i] + "_id");
            boolean hasKeys = keys.length == positions.length;
            for (int j = 0; j < positions.length; j++) {
                sets[i].put(positions[j], hasKeys ? keys[j] : 0); // old saves migrate lazily via sentinel 0
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (int i = 0; i < KEYS.length; i++) {
            LinkedHashMap<Long, Integer> set = sets[i];
            long[] positions = new long[set.size()];
            int[] keys = new int[set.size()];
            int j = 0;
            for (Map.Entry<Long, Integer> e : set.entrySet()) {
                positions[j] = e.getKey();
                keys[j] = e.getValue();
                j++;
            }
            tag.putLongArray(KEYS[i], positions);
            tag.putIntArray(KEYS[i] + "_id", keys);
        }
        return tag;
    }

    public static PlacedBlocksData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlacedBlocksData::new, NAME);
    }

    public void add(Functions.Kind kind, long packed, int key) {
        LinkedHashMap<Long, Integer> set = sets[kind.ordinal()];
        if (set.containsKey(packed)) {
            return;
        }
        int max = RuntimeConfig.trackMax[kind.ordinal()];
        if (set.size() >= max) {
            if (RuntimeConfig.trackEvict) {
                while (set.size() >= max) {
                    Iterator<Long> it = set.keySet().iterator();
                    it.next();
                    it.remove();
                }
            } else {
                return;
            }
        }
        set.put(packed, key);
        setDirty();
    }

    public boolean remove(Functions.Kind kind, long packed) {
        if (sets[kind.ordinal()].remove(packed) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean contains(Functions.Kind kind, long packed) {
        return sets[kind.ordinal()].containsKey(packed);
    }

    /** Recorded block identity for a tracked position, or 0 if absent/unmigrated. */
    public int key(Functions.Kind kind, long packed) {
        return sets[kind.ordinal()].getOrDefault(packed, 0);
    }

    public int count(Functions.Kind kind) {
        return sets[kind.ordinal()].size();
    }

    /** true when nothing is tracked for any kind — a cheap early-out for the per-setBlock hook. */
    public boolean isEmpty() {
        return sets[0].isEmpty() && sets[1].isEmpty() && sets[2].isEmpty();
    }

    /** Evicts the oldest entries until at most max remain. */
    public void trimTo(Functions.Kind kind, int max) {
        LinkedHashMap<Long, Integer> set = sets[kind.ordinal()];
        boolean removed = false;
        Iterator<Long> it = set.keySet().iterator();
        while (set.size() > max && it.hasNext()) {
            it.next();
            it.remove();
            removed = true;
        }
        if (removed) {
            scanBuf[kind.ordinal()] = null;
            setDirty();
        }
    }

    public void reset(Functions.Kind kind) {
        if (!sets[kind.ordinal()].isEmpty()) {
            sets[kind.ordinal()].clear();
            scanBuf[kind.ordinal()] = null;
            setDirty();
        }
    }

    public void resetAll() {
        for (Functions.Kind kind : Functions.Kind.values()) {
            reset(kind);
        }
    }

    /** prune orphaned positions of one kind — identity-based: a record is dropped only when the physical block changed
     *  (broken/burned/replaced), independent of the current function block list. */
    public void sweep(ServerLevel level, Functions.Kind kind, int budget) {
        int ord = kind.ordinal();
        LinkedHashMap<Long, Integer> set = sets[ord];
        if (set.isEmpty()) {
            scanBuf[ord] = null;
            return;
        }
        if (scanBuf[ord] == null || scanIdx[ord] >= scanBuf[ord].length) {
            long[] arr = new long[set.size()];
            int j = 0;
            for (long v : set.keySet()) {
                arr[j++] = v;
            }
            scanBuf[ord] = arr;
            scanIdx[ord] = 0;
        }
        long[] buf = scanBuf[ord];
        int end = Math.min(buf.length, scanIdx[ord] + budget);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (; scanIdx[ord] < end; scanIdx[ord]++) {
            long packed = buf[scanIdx[ord]];
            if (!set.containsKey(packed)) {
                continue; // already removed since the snapshot
            }
            cursor.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (!level.isLoaded(cursor)) {
                continue; // don't force-load a chunk just to check
            }
            int rec = set.get(packed);
            int cur = blockKey(level.getBlockState(cursor));
            if (rec == 0) {
                // unmigrated old record: adopt the present block's identity if still this function's block, else genuine orphan
                if (Functions.trackedKind(level.getBlockState(cursor).getBlock()) == kind) {
                    set.put(packed, cur);
                    setDirty();
                } else {
                    set.remove(packed);
                    setDirty();
                }
            } else if (cur != rec) {
                set.remove(packed); // the physical block actually changed
                setDirty();
            }
        }
    }
}
