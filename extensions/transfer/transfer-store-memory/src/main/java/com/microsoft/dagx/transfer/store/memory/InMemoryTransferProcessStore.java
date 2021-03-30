package com.microsoft.dagx.transfer.store.memory;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe process store.
 *
 * This implementation is intended for testing purposes only.
 */
public class InMemoryTransferProcessStore implements TransferProcessStore {
    private static final int TIMEOUT = 1000;

    private Map<String, TransferProcess> processesById = new HashMap<>();
    private Map<Integer, TreeSet<TransferProcess>> stateCache = new HashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public @Nullable TransferProcess find(String id) {
        return readLock(() -> processesById.get(id));
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        return readLock(() -> {
            var set = stateCache.get(state);
            return set == null ? Collections.emptyList() : set.stream().limit(max).map(TransferProcess::copy).collect(toList());
        });
    }

    @Override
    public void create(TransferProcess process) {
        writeLock(() -> {
            process.transitionInitial();
            delete(process.getId());
            TransferProcess internalCopy = process.copy();
            processesById.put(process.getId(), internalCopy);
            stateCache.computeIfAbsent(process.getState(), k -> new TreeSet<>(comparingInt(TransferProcess::getState))).add(internalCopy);
            return null;
        });
    }

    @Override
    public void update(TransferProcess process) {
        writeLock(() -> {
            delete(process.getId());
            TransferProcess internalCopy = process.copy();
            processesById.put(process.getId(), internalCopy);
            stateCache.computeIfAbsent(process.getState(), k -> new TreeSet<>(comparingLong(TransferProcess::getStateTimestamp))).add(internalCopy);
            return null;
        });
    }

    @Override
    public void delete(String processId) {
        writeLock(() -> {
            TransferProcess process = processesById.remove(processId);
            if (process != null) {
                TreeSet<TransferProcess> set = stateCache.get(process.getState());
                if (set != null) {
                    set.remove(process);
                }
            }
            return null;
        });
    }

    @Override
    public void createData(String processId, String key, Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateData(String processId, String key, Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteData(String processId, String key) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteData(String processId, Set<String> keys) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public <T> T findData(Class<T> type, String processId, String resourceDefinitionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private <T> T readLock(Supplier<T> work) {
        try {
            if (!lock.readLock().tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new DagxException("Timeout acquiring read lock");
            }
            T value = work.get();
            lock.readLock().unlock();
            return value;
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new DagxException(e);
        }
    }

    private <T> T writeLock(Supplier<T> work) {
        try {
            if (!lock.writeLock().tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new DagxException("Timeout acquiring write lock");
            }
            T value = work.get();
            lock.writeLock().unlock();
            return value;
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new DagxException(e);
        }
    }

}
