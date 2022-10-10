package foo.v5archstudygroup.demos.distributedlog.store;

import java.util.Optional;

/**
 * Need to take care of thread safety!
 */
public interface KeyValueStoreBackend extends AutoCloseable {

    void write(byte[] key, byte[] value);

    boolean hasKey(byte[] key);

    Optional<byte[]> read(byte[] key);

    void remove(byte[] key);

    long readLastSeenWalEntry();

    void writeLastSeenWalEntryId(long entryId);

    long size();

    long capacity();
}
