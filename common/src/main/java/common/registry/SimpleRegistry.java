package common.registry;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import common.network.SynchronizeData;
import common.util.KioskLoggerFactory;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link Registry}의 구현형 클래스이다. 특정한 레지스트리가 필요하지 않는다면 이 레지스트리를 사용하면 된다.
 * 레지스트리는 기본적으로 thread-safe 하게 작성되어야 한다. 가장 교착상태가 예상되는 클래스로, 나는 {@link ReentrantLock} 을 통해 동시성을 제어했다.
 * @param <T> 저장할 데이터 자료형
 */
public class SimpleRegistry<T extends SynchronizeData<?>> implements Registry<T> {
    protected final Logger LOGGER = KioskLoggerFactory.getLogger();
    protected final Map<String, T> idToEntry = new Object2ObjectOpenHashMap<>();
    protected final Map<T, String> entryToId = new Object2ObjectOpenHashMap<>();
    protected final Set<T> ITEMS = new ObjectLinkedOpenHashSet<>();
    protected final Reference2IntMap<T> entryToRawIndex = new Reference2IntOpenHashMap<>();
    protected final Int2ReferenceMap<T> rawIndexToEntry = new Int2ReferenceOpenHashMap<>();
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final AtomicBoolean frozen = new AtomicBoolean(false);
    private final Class<T> clazz;
    @NotNull
    private final Codec<T> codec;
    @NotNull
    private final String registryId;

    public SimpleRegistry(@NotNull String registryId, @NotNull Codec<T> codec, Class<T> clazz) {
        this.registryId = registryId;
        this.codec = codec;
        this.clazz = clazz;
    }

    @Override
    public @NotNull String getRegistryId() {
        return this.registryId;
    }

    @Override
    public Optional<T> getById(String id) {
        try {
            this.lock.readLock().lock();
            return Optional.ofNullable(idToEntry.get(id));
        } finally {
            this.lock.readLock().unlock();
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
    public T add(String id, SynchronizeData<?> entry) {
        if (this.isFrozen()) {
            throw new IllegalStateException("Registry is already frozen");
        }
        if (!clazz.isAssignableFrom(entry.getClass())) {
            throw new IllegalArgumentException("Entry is not of type " + clazz.getName());
        }
        this.lock.writeLock().lock();
        try {
            T existingEntry = idToEntry.get(id);
            if (existingEntry != null) {
                this.ITEMS.remove(existingEntry);
                this.entryToId.remove(existingEntry);
                this.entryToRawIndex.remove(existingEntry);
            }

            this.ITEMS.add((T) entry);
            this.entryToId.put((T) entry, id);
            this.idToEntry.put(id, (T) entry);
            this.rawIndexToEntry.put(size(), (T) entry);
            this.entryToRawIndex.put((T) entry, size());
        } finally {
            this.lock.writeLock().unlock();
        }
        LOGGER.info("Registered {} with id {}", entry, id);
        return (T) entry;
    }

    @Override
    public void addAll(List<SynchronizeData<?>> dataList) {
        if (this.isFrozen()) {
            throw new IllegalStateException("Registry is already frozen");
        }
        lock.writeLock().lock();
        try {
            dataList.forEach(data -> {
                if (!clazz.isAssignableFrom(data.getClass())) {
                    throw new IllegalArgumentException("Entry is not of type " + clazz.getName());
                }

                T existingEntry = idToEntry.get(data.getRegistryElementId());
                if (existingEntry != null) {
                    this.ITEMS.remove(existingEntry);
                    this.entryToId.remove(existingEntry);
                    this.entryToRawIndex.remove(existingEntry);
                }

                this.ITEMS.add((T) data);
                this.entryToId.put((T) data, data.getRegistryElementId());
                this.idToEntry.put(data.getRegistryElementId(), (T) data);
                this.rawIndexToEntry.put(size(), (T) data);
                this.entryToRawIndex.put((T) data, size());
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public @NotNull Codec<T> getCodec() {
        return this.codec;
    }

    @Override
    public List<T> getAll() {
        return ImmutableList.copyOf(this.ITEMS);
    }

    @Override
    public void clear() {
        if (this.isFrozen()) {
            throw new IllegalStateException("Registry is already frozen");
        }
        try {
            lock.writeLock().lock();
            this.ITEMS.clear();
            this.entryToId.clear();
            this.idToEntry.clear();
            this.rawIndexToEntry.clear();
            this.entryToRawIndex.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Class<T> getClazz() {
        return this.clazz;
    }

    @Override
    public int getRawId(T var1) {
        try {
            this.lock.readLock().lock();
            return entryToRawIndex.getOrDefault(var1, ABSENT_LOW_INDEX);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public @Nullable T get(int index) {
        try {
            this.lock.readLock().lock();
            return rawIndexToEntry.get(index);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        try {
            this.lock.readLock().lock();
            return this.ITEMS.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return ITEMS.iterator();
    }

    @Override
    public boolean remove(String id) {
        if (this.isFrozen()) {
            throw new IllegalStateException("Registry is already frozen");
        }
        try {
            lock.writeLock().lock();
            T item = idToEntry.get(id);
            if (item == null) {
                return false;
            }

            idToEntry.remove(id);
            entryToId.remove(item);
            ITEMS.remove(item);

            rebuildIndexMaps();

            LOGGER.info("Removed {} with id {}", item, id);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(T entry) {
        if (entry == null) return false;
        String id = entryToId.get(entry);
        if (id == null) return false;
        return remove(id);
    }

    private void rebuildIndexMaps() {
        entryToRawIndex.clear();
        rawIndexToEntry.clear();

        int index = 0;
        for (T item : ITEMS) {
            entryToRawIndex.put(item, index);
            rawIndexToEntry.put(index, item);
            index++;
        }
    }
}
