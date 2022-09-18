package foo.v5archstudygroup.demos.distributedlog.wal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

import static java.util.Objects.requireNonNull;

public class OutputStreamWriter implements Writer {

    private final OutputStream outputStream;
    private final Supplier<Checksum> checksumFactory;

    public OutputStreamWriter(OutputStream outputStream, Supplier<Checksum> checksumFactory) {
        this.outputStream = requireNonNull(outputStream);
        this.checksumFactory = requireNonNull(checksumFactory);
    }

    public OutputStreamWriter(OutputStream outputStream) {
        this(outputStream, CRC32::new);
    }

    @Override
    public boolean writeEntry(Entry entry) {
        var checksum = checksumFactory.get();
        try {
            var dos = new DataOutputStream(new CheckedOutputStream(outputStream, checksum));
            var entryTypeBytes = entry.type().getBytes(StandardCharsets.UTF_8);
            dos.writeLong(entry.id());
            dos.writeInt(entry.data().length);
            dos.write(entry.data());
            dos.writeShort(entryTypeBytes.length);
            dos.write(entryTypeBytes);
            dos.writeLong(entry.timestamp().toEpochMilli());
            dos.writeLong(checksum.getValue());
            outputStream.flush();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
