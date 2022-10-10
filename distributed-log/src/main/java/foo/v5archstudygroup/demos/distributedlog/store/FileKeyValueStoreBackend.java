package foo.v5archstudygroup.demos.distributedlog.store;

import java.io.*;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class FileKeyValueStoreBackend implements KeyValueStoreBackend {

    // TODO The code could be made cleaner and more functional in style by removing all IOExceptions and using unchecked exceptions instead
    // TODO Add checks to ensure there are no overflows or wraparounds
    // TODO Optimize for speed

    private static final String INDEX_FILE_NAME = "index.dat";
    private static final String DATA_FILE_NAME = "data.dat";
    private final IndexFile index;
    private final DataFile data;

    public FileKeyValueStoreBackend(File directory, long initialCapacity) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        this.index = new IndexFile(new File(directory, INDEX_FILE_NAME), initialCapacity);
        this.data = new DataFile(new File(directory, DATA_FILE_NAME));
    }

    @Override
    public synchronized void close() throws Exception {
        index.close();
        data.close();
    }

    @Override
    public synchronized void write(byte[] key, byte[] value) {
        try {
            var dataAddress = index.get(key);
            if (dataAddress.isPresent()) {
                data.write(dataAddress.get(), value);
            } else {
                var newAddress = data.allocate(value.length);
                data.write(newAddress, value);
                index.put(key, newAddress);
            }
        } catch (IOException ex) {
            throw new KeyValueStoreBackendException(ex);
        }
    }

    @Override
    public synchronized boolean hasKey(byte[] key) {
        try {
            return index.contains(key);
        } catch (IOException ex) {
            throw new KeyValueStoreBackendException(ex);
        }
    }

    @Override
    public synchronized Optional<byte[]> read(byte[] key) {
        try {
            var dataAddress = index.get(key);
            if (dataAddress.isPresent()) {
                return data.read(dataAddress.get());
            }
            return Optional.empty();
        } catch (IOException ex) {
            throw new KeyValueStoreBackendException(ex);
        }
    }

    @Override
    public synchronized void remove(byte[] key) {
        try {
            var dataAddress = index.remove(key);
            if (dataAddress.isPresent()) {
                data.free(dataAddress.get());
            }
        } catch (IOException ex) {
            throw new KeyValueStoreBackendException(ex);
        }
    }

    @Override
    public synchronized long readLastSeenWalEntry() {
        return 0;
    }

    @Override
    public synchronized void writeLastSeenWalEntryId(long entryId) {

    }

    @Override
    public synchronized long size() {
        return index.size();
    }

    @Override
    public synchronized long capacity() {
        return index.capacity();
    }

    static final class IndexFile implements AutoCloseable {

        private static final int FP_CAPACITY = 0;
        private static final int FP_SIZE = FP_CAPACITY + Long.BYTES;
        private static final int FP_INDEX_SECTION = FP_SIZE + Long.BYTES;

        private final RandomAccessFile file;
        private long capacity;
        private long size;

        public IndexFile(File file, long initialCapacity) throws IOException {
            this.file = new RandomAccessFile(file, "rwd");

            try {
                this.capacity = this.file.readLong();
                this.size = this.file.readLong();
            } catch (EOFException ex) {
                this.capacity = initialCapacity;
                this.size = 0;
                initializeIndex();
            }
        }

        private void initializeIndex() throws IOException {
            file.seek(0);
            file.writeLong(capacity);
            file.writeLong(size);
            for (var i = 0L; i < capacity; ++i) {
                file.writeLong(0L);
            }
        }

        public synchronized boolean contains(byte[] key) throws IOException {
            return get(key).isPresent();
        }

        public synchronized Optional<DataAddress> remove(byte[] key) throws IOException {
            return put(key, null);
        }

        public synchronized Optional<DataAddress> put(byte[] key, DataAddress address) throws IOException {
            var bucketAddress = getBucketAddress(key);
            var bucket = new Bucket(bucketAddress);
            if (address == null) {
                var result = bucket.remove(key);
                if (result.isPresent()) {
                    setSize(size() - 1);
                }
                return result;
            } else {
                var result = bucket.put(key, address);
                if (result.isEmpty()) {
                    setSize(size() + 1);
                }
                return result;
            }
        }

        private void setSize(long size) throws IOException {
            if (size != this.size) {
                file.seek(FP_SIZE);
                file.writeLong(size);
                this.size = size;
            }
        }

        public synchronized Optional<DataAddress> get(byte[] key) throws IOException {
            var bucketAddress = getBucketAddress(key);
            var bucket = new Bucket(bucketAddress);
            return bucket.findByKey(key).map(Node::dataAddress);
        }

        public synchronized long capacity() {
            return capacity;
        }

        public synchronized long size() {
            return size;
        }

        @Override
        public synchronized void close() throws Exception {
            file.close();
        }

        private long hashCode(byte[] key) {
            return Math.abs((long) Arrays.hashCode(key));
        }

        private long getBucketAddress(byte[] key) {
            return FP_INDEX_SECTION + (hashCode(key) % capacity) * Long.BYTES;
        }

        private long getFreeAddress(long requiredSpaceInBytes) throws IOException {
            return file.length(); // TODO be smarter than always writing to the end of the file
        }

        private void markAsFreeSpace(long address, long sizeInBytes) throws IOException {
            // TODO do something with this
        }

        // TODO Defragmentation
        // TODO Increase capacity

        private class Bucket {
            private final long address;
            private long headNodeAddress;

            public Bucket(long address) throws IOException {
                this.address = address;
                file.seek(address);
                headNodeAddress = file.readLong();
            }

            public Optional<Node> findByKey(byte[] key) throws IOException {
                requireNonNull(key);
                var node = head().orElse(null);
                while (node != null) {
                    if (node.keyEquals(key)) {
                        return Optional.of(node);
                    } else {
                        node = node.nextNode().orElse(null);
                    }
                }
                return Optional.empty();
            }

            public Optional<Node> head() throws IOException {
                return headNodeAddress == 0 ? Optional.empty() : Optional.of(new Node(headNodeAddress));
            }

            public Optional<DataAddress> remove(byte[] key) throws IOException {
                requireNonNull(key);
                var node = head().orElse(null);
                Node previousNode = null;
                while (node != null) {
                    if (node.keyEquals(key)) {
                        if (previousNode == null) {
                            setHead(null);
                        } else {
                            previousNode.setNextNodeAddress(node.nextNodeAddress());
                        }
                        markAsFreeSpace(node.address(), node.size());
                        return Optional.of(node.dataAddress());
                    }
                    previousNode = node;
                    node = node.nextNode().orElse(null);
                }
                return Optional.empty();
            }

            public Optional<DataAddress> put(byte[] key, DataAddress address) throws IOException {
                requireNonNull(key);
                requireNonNull(address);
                var node = head().orElse(null);
                Node previousNode = null;
                while (node != null) {
                    if (node.keyEquals(key)) {
                        var old = node.dataAddress();
                        node.setDataAddress(address);
                        return Optional.of(old);
                    }
                    previousNode = node;
                    node = node.nextNode().orElse(null);
                }
                node = new Node(getFreeAddress(Node.calculateSize(key)), key, address);
                if (previousNode == null) {
                    setHead(node);
                } else {
                    previousNode.setNextNode(node);
                }
                return Optional.empty();
            }

            private void setHead(Node head) throws IOException {
                var newHeadNodeAddress = head == null ? 0 : head.address();
                file.seek(address);
                file.writeLong(newHeadNodeAddress);
                this.headNodeAddress = newHeadNodeAddress;
            }
        }

        private class Node {
            private final long address;
            private final byte[] key;
            private long dataAddress;
            private long nextNodeAddress;

            public Node(long address) throws IOException {
                this.address = address;
                file.seek(address);
                var keyLength = file.readInt();
                key = new byte[keyLength];
                file.read(key);
                dataAddress = file.readLong();
                nextNodeAddress = file.readLong();
            }

            public Node(long address, byte[] key, DataAddress dataAddress) throws IOException {
                requireNonNull(key);
                requireNonNull(dataAddress);
                this.address = address;
                this.key = key;
                this.dataAddress = dataAddress.address;
                this.nextNodeAddress = 0;
                file.seek(this.address);
                file.writeInt(this.key.length);
                file.write(this.key);
                file.writeLong(this.dataAddress);
                file.writeLong(this.nextNodeAddress);
            }

            public long address() {
                return address;
            }

            public boolean keyEquals(byte[] key) {
                return Arrays.equals(this.key, key);
            }

            public DataAddress dataAddress() {
                return new DataAddress(dataAddress);
            }

            public void setDataAddress(DataAddress dataAddress) throws IOException {
                requireNonNull(dataAddress);
                file.seek(this.address + Integer.BYTES + this.key.length);
                file.writeLong(dataAddress.address);
                this.dataAddress = dataAddress.address;
            }

            public Optional<Node> nextNode() throws IOException {
                return nextNodeAddress == 0 ? Optional.empty() : Optional.of(new Node(nextNodeAddress));
            }

            public long nextNodeAddress() {
                return nextNodeAddress;
            }

            public void setNextNode(Node node) throws IOException {
                setNextNodeAddress(node == null ? 0 : node.address);
            }

            public void setNextNodeAddress(long nodeAddress) throws IOException {
                file.seek(this.address + Integer.BYTES + this.key.length + Long.BYTES);
                file.writeLong(nodeAddress);
                this.nextNodeAddress = nodeAddress;
            }

            public long size() {
                return calculateSize(key);
            }

            public static long calculateSize(byte[] key) {
                return Integer.BYTES + key.length + Long.BYTES + Long.BYTES;
            }
        }
    }

    static final class DataAddress {
        private final long address;

        public DataAddress(long address) {
            this.address = address;
        }

        public DataAddress(int pageIndex, int blockIndex) {
            this.address = ((long) pageIndex << 32) + blockIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataAddress that = (DataAddress) o;
            return address == that.address;
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }

        public int pageIndex() {
            return (int) (this.address >> 32);
        }

        public int blockIndex() {
            return (int) this.address;
        }
    }

    static final class DataFile implements AutoCloseable {

        static final int BLOCK_SIZE_BYTES = 256;
        //                                                           size of data in block   address to next block
        static final int BLOCK_PAYLOAD_SIZE_BYTES = BLOCK_SIZE_BYTES - Short.BYTES - Long.BYTES;
        static final int PAGE_SIZE_BLOCKS = 256;
        private static final int PAGE_HEADER_SIZE_BYTES = PAGE_SIZE_BLOCKS / 8;
        static final int PAGE_SIZE_BYTES = PAGE_HEADER_SIZE_BYTES + PAGE_SIZE_BLOCKS * BLOCK_SIZE_BYTES;
        static final int PAGE_PAYLOAD_SIZE_BYTES = PAGE_SIZE_BLOCKS * BLOCK_PAYLOAD_SIZE_BYTES;

        private final RandomAccessFile file;
        private int pages;
        private final Map<Integer, Page> pageCache = new HashMap<>();

        public DataFile(File file) throws IOException {
            this.file = new RandomAccessFile(file, "rwd");
        }

        @Override
        public synchronized void close() throws Exception {
            file.close();
        }

        public synchronized void write(DataAddress address, byte[] data) throws IOException {
            requireNonNull(address);
            requireNonNull(data);
            var page = getPage(address.pageIndex());
            page.write(address.blockIndex(), data);
        }

        public synchronized DataAddress allocate(int bytes) throws IOException {
            for (var i = 0; i < pages; ++i) {
                var page = getPage(i);
                var address = page.tryAllocate(bytes);
                if (address.isPresent()) {
                    return address.get();
                }
            }
            var newPage = getPage(pages);
            pages++;
            return newPage.tryAllocate(bytes).orElseThrow(() -> new IOException("Could not allocate space in new block"));
        }

        public synchronized void free(DataAddress address) throws IOException {
            requireNonNull(address);
            var page = getPage(address.pageIndex());
            page.free(address.blockIndex());
        }

        public synchronized Optional<byte[]> read(DataAddress address) throws IOException {
            requireNonNull(address);
            var page = getPage(address.pageIndex());
            return page.read(address.blockIndex());
        }

        private Page getPage(int index) throws IOException {
            var page = pageCache.get(index);
            if (page == null) {
                page = new Page(index);
                pageCache.put(index, page);
            }
            return page;
        }

        public synchronized int pageCount() {
            return pages;
        }

        public synchronized int freeBlocksInPage(int pageIndex) throws IOException {
            return getPage(pageIndex).freeBlocks;
        }

        private class Page {
            private final long address;
            private final int index;
            private final byte[] header;
            private int freeBlocks;

            public Page(int index) throws IOException {
                if (index < 0) {
                    throw new IndexOutOfBoundsException("Page index cannot be negative");
                } else if (index > pages) {
                    throw new IndexOutOfBoundsException("Page index cannot jump over a nonexistent page");
                }
                this.index = index;
                address = (long) index * PAGE_SIZE_BYTES;
                header = new byte[PAGE_HEADER_SIZE_BYTES];
                file.seek(address);
                if (file.length() < address) {
                    file.write(header);
                    freeBlocks = PAGE_SIZE_BLOCKS;
                } else {
                    file.read(header);
                    for (int i = 0; i < PAGE_SIZE_BLOCKS; ++i) {
                        if (isFree(i)) {
                            freeBlocks++;
                        }
                    }
                }
            }

            private boolean isFree(int blockIndex) {
                var byteIndex = blockIndex / 8;
                var bitIndex = blockIndex % 8;
                var mask = (byte) ~(0b1 << (7 - bitIndex)); // 0 = free, 1 = allocated
                return (header[byteIndex] | mask) == mask;
            }

            private void setFree(int blockIndex, boolean free) throws IOException {
                var byteIndex = blockIndex / 8;
                var bitIndex = blockIndex % 8;
                byte maskedHeaderByte;
                if (free) {
                    maskedHeaderByte = (byte) (header[byteIndex] & (byte) ~(0b1 << (7 - bitIndex)));
                    if (header[byteIndex] != maskedHeaderByte) {
                        freeBlocks++;
                        header[byteIndex] = maskedHeaderByte;
                    }
                } else {
                    maskedHeaderByte = (byte) (header[byteIndex] | (byte) 0b1 << 7 - bitIndex);
                    if (header[byteIndex] != maskedHeaderByte) {
                        freeBlocks--;
                        header[byteIndex] = maskedHeaderByte;
                    }
                }
                file.seek(address);
                file.write(header);
            }

            public void write(int initialBlockIndex, byte[] data) throws IOException {
                // TODO If we are writing over existing data with shorter data than before, we have to free up the blocks that are no longer needed
                var block = new Block(this, initialBlockIndex);
                var offset = 0;
                var bytesToWrite = data.length;
                while (bytesToWrite > 0) {
                    block.write(data, offset, (short) Math.min(BLOCK_PAYLOAD_SIZE_BYTES, bytesToWrite));
                    bytesToWrite = bytesToWrite - BLOCK_PAYLOAD_SIZE_BYTES;
                    offset = offset + BLOCK_PAYLOAD_SIZE_BYTES;
                    if (bytesToWrite > 0) {
                        int nextBlock = tryAllocateNextFreeBlock().orElse(-1);
                        if (nextBlock == -1) {
                            var nextBlockAddress = allocate(bytesToWrite);
                            block.setNextBlock(nextBlockAddress);
                            block = new Block(getPage(nextBlockAddress.pageIndex()), nextBlockAddress.blockIndex());
                        } else {
                            block.setNextBlock(new DataAddress(index, nextBlock));
                            block = new Block(this, nextBlock);
                        }
                    } else {
                        block.setNextBlock(null);
                    }
                }
            }

            public void free(int initialBlockIndex) throws IOException {
                if (isFree(initialBlockIndex)) {
                    return;
                }
                var block = new Block(this, initialBlockIndex);
                setFree(initialBlockIndex, true);
                var nextBlockAddress = block.nextBlock().orElse(null);
                while (nextBlockAddress != null) {
                    if (nextBlockAddress.pageIndex() == index) {
                        block = new Block(this, nextBlockAddress.blockIndex());
                        setFree(nextBlockAddress.blockIndex(), true);
                        nextBlockAddress = block.nextBlock().orElse(null);
                    } else {
                        getPage(nextBlockAddress.pageIndex()).free(nextBlockAddress.blockIndex());
                        return;
                    }
                }
            }

            public Optional<byte[]> read(int initialBlockIndex) throws IOException {
                if (isFree(initialBlockIndex)) {
                    return Optional.empty();
                }
                var block = new Block(this, initialBlockIndex);
                var nextBlockAddress = block.nextBlock().orElse(null);
                if (nextBlockAddress == null) {
                    return Optional.of(block.read());
                } else {
                    var bos = new ByteArrayOutputStream();
                    bos.writeBytes(block.read());
                    while (nextBlockAddress != null) {
                        if (nextBlockAddress.pageIndex() == index) {
                            block = new Block(this, nextBlockAddress.blockIndex());
                        } else {
                            block = new Block(getPage(nextBlockAddress.pageIndex()), nextBlockAddress.blockIndex());
                        }
                        bos.writeBytes(block.read());
                        nextBlockAddress = block.nextBlock().orElse(null);
                    }
                    return Optional.of(bos.toByteArray());
                }
            }

            public Optional<DataAddress> tryAllocate(int bytes) throws IOException {
                if (bytes > PAGE_PAYLOAD_SIZE_BYTES && freeBlocks == PAGE_SIZE_BLOCKS) {
                    setFree(0, false);
                    return Optional.of(new DataAddress(index, 0));
                } else if (bytes <= freeBlocks * BLOCK_PAYLOAD_SIZE_BYTES) {
                    return tryAllocateNextFreeBlock().map(blockIndex -> new DataAddress(index, blockIndex));
                }
                return Optional.empty();
            }

            private Optional<Integer> tryAllocateNextFreeBlock() throws IOException {
                for (var i = 0; i < PAGE_SIZE_BLOCKS; ++i) {
                    if (isFree(i)) {
                        setFree(i, false);
                        return Optional.of(i);
                    }
                }
                return Optional.empty();
            }
        }

        private class Block {

            private final long address;

            public Block(Page page, int index) {
                address = page.address + PAGE_HEADER_SIZE_BYTES + (long) index * BLOCK_SIZE_BYTES;
            }

            public void write(byte[] data) throws IOException {
                write(data, 0, (short) Math.min(data.length, Short.MAX_VALUE));
            }

            public void write(byte[] data, int offset, short length) throws IOException {
                if (length > BLOCK_PAYLOAD_SIZE_BYTES) {
                    throw new IOException("Data is too large");
                }
                file.seek(address);
                file.writeShort(length);
                file.write(data, offset, length);
            }

            public byte[] read() throws IOException {
                file.seek(address);
                var length = file.readShort();
                var data = new byte[length];
                file.read(data);
                return data;
            }

            public void setNextBlock(DataAddress dataAddress) throws IOException {
                file.seek(address + BLOCK_SIZE_BYTES - Long.BYTES);
                if (dataAddress == null) {
                    file.writeLong(0L);
                } else {
                    file.writeLong(dataAddress.address);
                }
            }

            public Optional<DataAddress> nextBlock() throws IOException {
                file.seek(address + BLOCK_SIZE_BYTES - Long.BYTES);
                var dataAddress = file.readLong();
                return dataAddress == 0 ? Optional.empty() : Optional.of(new DataAddress(dataAddress));
            }
        }
    }
}
