package foo.v5archstudygroup.demos.distributedlog.wal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

public class WriteAheadLogImpl implements Iterable<Entry> {

    private static final int MAX_DATA_SIZE_BYTES = 400 * 1024;
    private static final int MAX_TYPE_LENGTH = 400;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicLong nextEntryId = new AtomicLong(1L);
    private final Config config;
    private final List<Entry> entries = new ArrayList<>(); // TODO Get rid of this cache and read directly from streams; add caching to the config instead.

    public WriteAheadLogImpl(Config config) {
        this.config = Objects.requireNonNull(config);
        loadEntries();
    }

    private static Checksum createChecksum() {
        return new CRC32();
    }

    private void loadEntries() {
        var readBytes = 0;
        try (var is = config.getInputStream()) {
            var checksum = createChecksum();
            while (is.available() > 0) {
                checksum.reset();
                var dis = new DataInputStream(new CheckedInputStream(is, checksum));
                var id = dis.readLong();
                var dataLength = dis.readInt();
                var data = dis.readNBytes(dataLength);
                var typeLength = dis.readUnsignedShort();
                var type = dis.readNBytes(typeLength);
                var timestamp = dis.readLong();
                var actualChecksum = checksum.getValue();
                var expectedChecksum = dis.readLong();
                if (expectedChecksum != actualChecksum) {
                    throw new IllegalStateException("Checksum does not match - log is corrupt");
                }
                this.entries.add(new Entry(
                        id,
                        data,
                        new String(type, StandardCharsets.UTF_8),
                        Instant.ofEpochMilli(timestamp)));

                //            id           dataLength      data          typeLength    type          timestamp    checksum
                readBytes += (Long.BYTES + Integer.BYTES + data.length + Short.BYTES + type.length + Long.BYTES + Long.BYTES);
            }
        } catch (EOFException ex) {
            // If this happens, the log is corrupt, and we have to discard the last entry read.
            config.resetOutputStream(readBytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load entries", ex);
        }
        if (this.entries.size() > 0) {
            nextEntryId.set(this.entries.get(this.entries.size() - 1).id() + 1);
        }
    }

    /**
     * TODO Document me!
     *
     * @param data
     * @param type
     */
    public void writeEntry(byte[] data, String type) {
        if (data.length > MAX_DATA_SIZE_BYTES) {
            throw new IllegalArgumentException("The data is too large");
        }
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException("The type is too long");
        }
        lock.writeLock().lock();
        try {
            var id = nextEntryId.getAndIncrement();
            var timestamp = config.clock().instant();
            var entry = beforeWrite(new Entry(id, data, type, timestamp));
            writeEntry(entry);
            afterWrite(entry);
            this.entries.add(entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected Entry beforeWrite(Entry entry) {
        return entry;
    }

    protected void afterWrite(Entry entry) {
    }

    private void writeEntry(Entry entry) {
        var checksum = createChecksum();
        try (var os = config.getOutputStream()) {
            var dos = new DataOutputStream(new CheckedOutputStream(os, checksum));
            var entryTypeBytes = entry.type().getBytes(StandardCharsets.UTF_8);
            dos.writeLong(entry.id());
            dos.writeInt(entry.data().length);
            dos.write(entry.data());
            dos.writeShort(entryTypeBytes.length);
            dos.write(entryTypeBytes);
            dos.writeLong(entry.timestamp().toEpochMilli());
            dos.writeLong(checksum.getValue());
            os.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write entry", ex);
        }
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<>() {
            private int position = 0;

            @Override
            public boolean hasNext() {
                lock.readLock().lock();
                try {
                    return position < entries.size();
                } finally {
                    lock.readLock().unlock();
                }
            }

            @Override
            public Entry next() {
                lock.readLock().lock();
                try {
                    return entries.get(position++);
                } catch (IndexOutOfBoundsException ex) {
                    throw new NoSuchElementException();
                } finally {
                    lock.readLock().unlock();
                }
            }
        };
    }

    /**
     * TODO Document me
     */
    public interface Config {
        Clock clock();

        InputStream getInputStream();

        OutputStream getOutputStream();

        void resetOutputStream(int offsetInBytes);
    }

}
