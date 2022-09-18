package foo.v5archstudygroup.demos.distributedlog.store;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class FileKeyValueStoreBackendTest {

    private static final int CAPACITY = 1000;

    private File createTempFile() throws IOException {
        var tempFile = File.createTempFile("foo", "bar");
        tempFile.deleteOnExit();
        return tempFile;
    }

    private FileKeyValueStoreBackend.IndexFile createIndexFile() throws IOException {
        return new FileKeyValueStoreBackend.IndexFile(createTempFile(), CAPACITY);
    }

    private FileKeyValueStoreBackend.DataFile createDataFile() throws IOException {
        return new FileKeyValueStoreBackend.DataFile(createTempFile());
    }

    @Test
    void empty_index_throws_no_errors_when_accessed() throws Exception {
        try (var indexFile = createIndexFile()) {
            assertThat(indexFile.capacity()).isEqualTo(CAPACITY);
            assertThat(indexFile.size()).isEqualTo(0);

            var key = "hello".getBytes(StandardCharsets.UTF_8);

            assertThat(indexFile.contains(key)).isFalse();
            assertThat(indexFile.get(key)).isEmpty();
            indexFile.remove(key); // Should not do anything
        }
    }

    @Test
    void single_index_entry_can_be_written_updated_and_removed() throws Exception {
        try (var indexFile = createIndexFile()) {
            var key = "hello".getBytes(StandardCharsets.UTF_8);
            // Insert
            indexFile.put(key, new FileKeyValueStoreBackend.DataAddress(123L));
            assertThat(indexFile.contains(key)).isTrue();
            assertThat(indexFile.get(key)).contains(new FileKeyValueStoreBackend.DataAddress(123L));
            assertThat(indexFile.size()).isEqualTo(1);

            // Update
            indexFile.put(key, new FileKeyValueStoreBackend.DataAddress(456L));
            assertThat(indexFile.get(key)).contains(new FileKeyValueStoreBackend.DataAddress(456L));
            assertThat(indexFile.size()).isEqualTo(1);

            // Delete
            indexFile.remove(key);
            assertThat(indexFile.contains(key)).isFalse();
            assertThat(indexFile.size()).isEqualTo(0);
        }
    }

    @Test
    void index_can_be_filled_to_capacity() throws Exception {
        var file = createTempFile();
        try (var indexFile = new FileKeyValueStoreBackend.IndexFile(file, CAPACITY)) {
            for (int i = 0; i < CAPACITY; ++i) {
                var key = ("key" + i).getBytes(StandardCharsets.UTF_8);
                indexFile.put(key, new FileKeyValueStoreBackend.DataAddress(i));
            }
        }
        try (var indexFile = new FileKeyValueStoreBackend.IndexFile(file, CAPACITY)) {
            assertThat(indexFile.size()).isEqualTo(CAPACITY);
            for (int i = 0; i < CAPACITY; ++i) {
                var key = ("key" + i).getBytes(StandardCharsets.UTF_8);
                assertThat(indexFile.get(key)).contains(new FileKeyValueStoreBackend.DataAddress(i));
            }
        }
    }

    @Test
    void data_smaller_than_one_block_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "Hello World".getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS - 1);
        }
    }

    @Test
    void data_of_exactly_one_block_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS - 1);
        }
    }

    @Test
    void data_larger_than_one_block_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS - 2);
        }
    }

    @Test
    void data_larger_than_multiple_blocks_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * 2 + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS - 3);
        }
    }

    @Test
    void data_of_exactly_one_page_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(0);
        }
    }

    @Test
    void data_larger_than_one_page_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(2);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(0);
            assertThat(dataFile.freeBlocksInPage(1)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS - 1);
        }
    }

    @Test
    void data_larger_than_multiple_pages_can_be_written_to_and_read_from_data_file() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS * 2 + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            var readData = dataFile.read(address);
            assertThat(readData).contains(data);
            assertThat(dataFile.pageCount()).isEqualTo(3);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(0);
            assertThat(dataFile.freeBlocksInPage(1)).isEqualTo(0);
            assertThat(dataFile.freeBlocksInPage(2)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS - 1);
        }
    }

    @Test
    void data_smaller_than_one_block_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "Hello World".getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }

    @Test
    void data_of_exactly_one_block_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }

    @Test
    void data_larger_than_one_block_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }

    @Test
    void data_larger_than_multiple_blocks_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * 2 + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }

    @Test
    void data_of_exactly_one_page_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(1);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }

    @Test
    void data_larger_than_one_page_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(2);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
            assertThat(dataFile.freeBlocksInPage(1)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }

    @Test
    void data_larger_than_multiple_pages_can_be_freed() throws Exception {
        try (var dataFile = createDataFile()) {
            var data = "x".repeat(FileKeyValueStoreBackend.DataFile.BLOCK_PAYLOAD_SIZE_BYTES * FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS * 2 + 1).getBytes(StandardCharsets.UTF_8);
            var address = dataFile.allocate(data.length);
            dataFile.write(address, data);
            dataFile.free(address);
            assertThat(dataFile.read(address)).isEmpty();
            assertThat(dataFile.pageCount()).isEqualTo(3);
            assertThat(dataFile.freeBlocksInPage(0)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
            assertThat(dataFile.freeBlocksInPage(1)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
            assertThat(dataFile.freeBlocksInPage(2)).isEqualTo(FileKeyValueStoreBackend.DataFile.PAGE_SIZE_BLOCKS);
        }
    }
}
