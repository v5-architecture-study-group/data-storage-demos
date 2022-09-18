package foo.v5archstudygroup.demos.distributedlog.wal;

import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static java.util.Objects.requireNonNull;

public class InputStreamReader implements Reader {

    private final Supplier<InputStream> inputStreamSource;
    private final Supplier<Checksum> checksumFactory;

    public InputStreamReader(Supplier<InputStream> inputStreamSource, Supplier<Checksum> checksumFactory) {
        this.inputStreamSource = requireNonNull(inputStreamSource);
        this.checksumFactory = requireNonNull(checksumFactory);
    }

    public InputStreamReader(Supplier<InputStream> inputStreamSource) {
        this(inputStreamSource, CRC32::new);
    }

    @Override
    public Iterator<Entry> readEntries() {

        return null;
    }
}
