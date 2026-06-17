package g_vael.cmsall.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/** Per-world persistence of player-placed positions plus the recorded block identity, one ordered map per function. */
public final class PlacedBlocksData extends WorldSavedData {

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

    public PlacedBlocksData(String name) {
        super(name);
    }

    /** Stable identity for a block state: registry-name hashCode (0 = needs-migration sentinel). Rare 32-bit collisions over-protect at most one position and self-heal on break/eviction. */
    public static int blockKey(IBlockState state) {
        return Block.REGISTRY.getNameForObject(state.getBlock()).hashCode();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        for (int i = 0; i < KEYS.length; i++) {
            sets[i].clear();
            int[] arr = nbt.getIntArray(KEYS[i]);
            int[] keys = nbt.getIntArray(KEYS[i] + "_id");
            int count = arr.length / 2;
            boolean hasKeys = keys.length == count; // old saves lack _id -> migrate lazily via sentinel 0
            for (int j = 0, k = 0; j + 1 < arr.length; j += 2, k++) {
                long hi = ((long) arr[j]) << 32;
                long lo = ((long) arr[j + 1]) & 0xFFFFFFFFL;
                sets[i].put(Long.valueOf(hi | lo), Integer.valueOf(hasKeys ? keys[k] : 0));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        for (int i = 0; i < KEYS.length; i++) {
            LinkedHashMap<Long, Integer> set = sets[i];
            int[] arr = new int[set.size() * 2];
            int[] keys = new int[set.size()];
            int j = 0, k = 0;
            for (Map.Entry<Long, Integer> e : set.entrySet()) {
                long v = e.getKey().longValue();
                arr[j++] = (int) (v >>> 32);
                arr[j++] = (int) (v & 0xFFFFFFFFL);
                keys[k++] = e.getValue().intValue();
            }
            nbt.setIntArray(KEYS[i], arr);
            nbt.setIntArray(KEYS[i] + "_id", keys);
        }
        return nbt;
    }

    public static PlacedBlocksData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        PlacedBlocksData data = (PlacedBlocksData) storage.getOrLoadData(PlacedBlocksData.class, NAME);
        if (data == null) {
            data = new PlacedBlocksData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    public void add(Functions.Kind kind, long packed, int key) {
        LinkedHashMap<Long, Integer> set = sets[kind.ordinal()];
        Long pos = Long.valueOf(packed);
        if (set.containsKey(pos)) {
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
        set.put(pos, Integer.valueOf(key));
        markDirty();
    }

    public boolean remove(Functions.Kind kind, long packed) {
        if (sets[kind.ordinal()].remove(Long.valueOf(packed)) != null) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean contains(Functions.Kind kind, long packed) {
        return sets[kind.ordinal()].containsKey(Long.valueOf(packed));
    }

    /** Recorded block identity for a tracked position, or 0 if absent/unmigrated. */
    public int key(Functions.Kind kind, long packed) {
        Integer v = sets[kind.ordinal()].get(Long.valueOf(packed));
        return v == null ? 0 : v.intValue();
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
            markDirty();
        }
    }

    public void reset(Functions.Kind kind) {
        if (!sets[kind.ordinal()].isEmpty()) {
            sets[kind.ordinal()].clear();
            scanBuf[kind.ordinal()] = null;
            markDirty();
        }
    }

    public void resetAll() {
        for (Functions.Kind kind : Functions.Kind.values()) {
            reset(kind);
        }
    }

    /** prune orphaned positions of one kind — identity-based: a record is dropped only when the physical block changed
     *  (broken/burned/replaced), independent of the current function block list. */
    public void sweep(World world, Functions.Kind kind, int budget) {
        int ord = kind.ordinal();
        LinkedHashMap<Long, Integer> set = sets[ord];
        if (set.isEmpty()) {
            scanBuf[ord] = null;
            return;
        }
        if (scanBuf[ord] == null || scanIdx[ord] >= scanBuf[ord].length) {
            long[] arr = new long[set.size()];
            int k = 0;
            for (Long p : set.keySet()) {
                arr[k++] = p.longValue();
            }
            scanBuf[ord] = arr;
            scanIdx[ord] = 0;
        }
        long[] buf = scanBuf[ord];
        int end = Math.min(buf.length, scanIdx[ord] + budget);
        for (; scanIdx[ord] < end; scanIdx[ord]++) {
            long packed = buf[scanIdx[ord]];
            Long pos = Long.valueOf(packed);
            if (!set.containsKey(pos)) {
                continue; // already removed since the snapshot
            }
            BlockPos bp = BlockPos.fromLong(packed);
            if (!world.isBlockLoaded(bp)) {
                continue; // don't force-load a chunk just to check
            }
            int rec = set.get(pos).intValue();
            IBlockState cur = world.getBlockState(bp);
            if (rec == 0) {
                // unmigrated old record: adopt the present block's identity if still this function's block, else genuine orphan
                if (Functions.trackedKind(cur.getBlock()) == kind) {
                    set.put(pos, Integer.valueOf(blockKey(cur)));
                    markDirty();
                } else {
                    set.remove(pos);
                    markDirty();
                }
            } else if (blockKey(cur) != rec) {
                set.remove(pos); // the physical block actually changed
                markDirty();
            }
        }
    }
}
