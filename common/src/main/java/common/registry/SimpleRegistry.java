package common.registry;

import com.mojang.serialization.Codec;
import common.network.Serializable;
import common.network.SynchronizeData;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleRegistry<T extends SynchronizeData<?>> implements Registry<T> {
    private final Map<String, T> idToEntry = new Object2ObjectOpenHashMap<>();
    private final Map<T, String> entryToId = new Object2ObjectOpenHashMap<>();
    private final Set<T> ITEMS = new ObjectLinkedOpenHashSet<>();
    private final Reference2IntMap<T> entryToRawIndex = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<T> rawIndexToEntry = new Int2ReferenceOpenHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean frozen = new AtomicBoolean(false);

    @NotNull
    private final Codec<T> codec;
    @NotNull
    private final String registryId;

    public SimpleRegistry(@NotNull String registryId, @NotNull Codec<T> codec) {
        this.registryId = registryId;
        this.codec = codec;
    }

    @Override
    public String getRegistryId() {
        return this.registryId;
    }

    @Override
    public Optional<T> getById(String id) {
        synchronized (lock) {
            return Optional.ofNullable(idToEntry.get(id));
        }
    }

    @Override
    public boolean isFrozen() {
        return this.frozen.get();
    }

    @Override
    public void freeze() {
        this.frozen.set(true);
    }

    @Override
    public void unfreeze() {
        this.frozen.set(false);
    }

    @Override
    public T add(String id, T entry) {
        try {
            this.lock.lock();
            this.ITEMS.add(entry);
            this.entryToId.put(entry, id);
            this.idToEntry.put(id, entry);
            this.rawIndexToEntry.put(size(), entry);
            this.entryToRawIndex.put(entry, size());
        } finally {
            this.lock.unlock();
        }

        return entry;
    }

    @Override
    public @NotNull Codec<T> getCodec() {
        return this.codec;
    }

    @Override
    public int getRawId(T var1) {
        synchronized (lock) {
            return entryToRawIndex.getOrDefault(var1, ABSENT_LOW_INDEX);
        }
    }

    @Override
    public @Nullable T get(int index) {
        synchronized (lock) {
            return rawIndexToEntry.get(index);
        }
    }

    @Override
    public int size() {
        return this.ITEMS.size();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return ITEMS.iterator();
    }
}
