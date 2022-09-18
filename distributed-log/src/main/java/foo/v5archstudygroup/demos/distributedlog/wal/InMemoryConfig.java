package foo.v5archstudygroup.demos.distributedlog.wal;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/**
 * TODO Document me
 */
public class InMemoryConfig implements WriteAheadLogImpl.Config {

    private static final int INITIAL_BUFFER_SIZE_BYTES = 400;
    private final Clock clock;
    private final ByteBuffer buffer;
    private int dataLength;

    public InMemoryConfig(Clock clock) {
        this.clock = requireNonNull(clock);
        this.buffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE_BYTES);
        this.dataLength = 0;
    }

    public InMemoryConfig(Clock clock, byte[] data) {
        this.clock = requireNonNull(clock);
        this.buffer = ByteBuffer.allocate(Math.max(INITIAL_BUFFER_SIZE_BYTES, data.length));
        this.buffer.put(data);
        this.buffer.position(0);
        this.dataLength = data.length;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer.array(), dataLength);
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() {
                if (buffer.position() >= dataLength) {
                    return -1;
                }
                return buffer.get();
            }

            @Override
            public int available() {
                return dataLength - buffer.position();
            }
        };
    }

    @Override
    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) {
                // TODO Increase buffer if needed
                buffer.put((byte) (b & 0xff));
                dataLength++;
            }
        };
    }

    @Override
    public void resetOutputStream(int offsetInBytes) {
        if (offsetInBytes > dataLength) {
            throw new IndexOutOfBoundsException("Offset cannot be bigger than data length");
        }
        if (offsetInBytes < 0) {
            throw new IndexOutOfBoundsException("Offset cannot be negative");
        }
        buffer.position(offsetInBytes);
        dataLength = offsetInBytes;
    }
}
