package foo.v5archstudygroup.demos.distributedlog.wal;

import java.time.Clock;

public record Config(Clock clock,
                     Writer writer,
                     Reader reader,
                     int maxDataSizeBytes,
                     int maxTypeStringLength,
                     int maxLogSize
) {
}
