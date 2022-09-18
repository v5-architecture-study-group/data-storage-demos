package foo.v5archstudygroup.demos.distributedlog.wal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * TODO Document me
 */
public class WriteAheadLog {

    private final Config config;
    private final List<Entry> entries = new ArrayList<>();
    private final AtomicLong nextEntryId = new AtomicLong(1L);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @param config
     */
    public WriteAheadLog(Config config) {
        this.config = requireNonNull(config);
        config.reader().readEntries().forEachRemaining(entries::add);
        if (entries.size() > 0) {
            nextEntryId.set(entries.get(entries.size() - 1).id() + 1);
        }
    }

    /**
     * @param data
     * @param type
     */
    public void appendEntry(byte[] data, String type) {
        if (data.length > config.maxDataSizeBytes()) {
            throw new IllegalArgumentException("The data is too large");
        }
        if (type.length() > config.maxTypeStringLength()) {
            throw new IllegalArgumentException("The type is too long");
        }
        if (size() >= config.maxLogSize()) {
            throw new IllegalStateException("The log is full");
        }
        lock.writeLock().lock();
        try {
            var id = nextEntryId.getAndIncrement();
            var timestamp = config.clock().instant();
            var entry = new Entry(id, data, type, timestamp);
            if (config.writer().writeEntry(entry)) {
                this.entries.add(entry);
            } else {
                throw new IllegalStateException("No entries can be appended right now");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param consumer
     */
    public void forEach(Consumer<Entry> consumer) {
        for (var i = 0; i < size(); ++i) {
            consumer.accept(get(i));
        }
    }

    /**
     * @return
     */
    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addAppendListener(WriteAheadLogAppendListener listener, long lastSeenEntryId) {

    }

    private Entry get(int index) {
        lock.readLock().lock();
        try {
            return entries.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }
}
