package foo.v5archstudygroup.demos.distributedlog.wal;

import java.time.Instant;

/**
 * TODO Document me
 *
 * @param id
 * @param data
 * @param type
 * @param timestamp
 */
public record Entry(
        long id,
        byte[] data,
        String type,
        Instant timestamp
) {
}
