package foo.v5archstudygroup.demos.distributedlog.wal;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WriteAheadLogImplTest {

    @Test
    public void empty_log_has_empty_iterator() {
        var config = new InMemoryConfig(Clock.systemUTC());
        var log = new WriteAheadLogImpl(config);
        var it = log.iterator();
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void written_entries_can_be_read_back() {
        var now = Instant.now();
        var config = new InMemoryConfig(Clock.fixed(now, ZoneId.systemDefault()));
        var log = new WriteAheadLogImpl(config);
        log.writeEntry("hello world".getBytes(StandardCharsets.UTF_8), "greeting");
        var it = log.iterator();
        assertThat(it.hasNext()).isTrue();
        var entry = it.next();
        assertThat(entry.id()).isEqualTo(1L);
        assertThat(entry.data()).asString(StandardCharsets.UTF_8).isEqualTo("hello world");
        assertThat(entry.type()).isEqualTo("greeting");
        assertThat(entry.timestamp()).isEqualTo(now);
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    public void log_is_populated_from_non_empty_config() {
        var config = new InMemoryConfig(Clock.systemUTC());
        var log = new WriteAheadLogImpl(config);
        log.writeEntry("hello".getBytes(StandardCharsets.UTF_8), "greeting");
        log.writeEntry("world".getBytes(StandardCharsets.UTF_8), "greeting");
        log.writeEntry("foo".getBytes(StandardCharsets.UTF_8), "test");
        log.writeEntry("bar".getBytes(StandardCharsets.UTF_8), "test");

        var otherConfig = new InMemoryConfig(Clock.systemUTC(), config.toByteArray());
        var otherLog = new WriteAheadLogImpl(otherConfig);
        var entries = StreamSupport.stream(otherLog.spliterator(), false).toList();
        assertThat(entries).hasSize(4);
        assertThat(entries.get(0).data()).asString(StandardCharsets.UTF_8).isEqualTo("hello");
        assertThat(entries.get(1).data()).asString(StandardCharsets.UTF_8).isEqualTo("world");
        assertThat(entries.get(2).data()).asString(StandardCharsets.UTF_8).isEqualTo("foo");
        assertThat(entries.get(3).data()).asString(StandardCharsets.UTF_8).isEqualTo("bar");
    }

    @Test
    public void corrupt_log_is_not_loadable() {
        var config = new InMemoryConfig(Clock.systemUTC());
        var log = new WriteAheadLogImpl(config);
        log.writeEntry("hello".getBytes(StandardCharsets.UTF_8), "greeting");
        log.writeEntry("world".getBytes(StandardCharsets.UTF_8), "greeting");

        var rawLog = config.toByteArray();
        rawLog[0] = 1;

        assertThatThrownBy(() -> new WriteAheadLogImpl(new InMemoryConfig(Clock.systemUTC(), rawLog))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void corrupt_last_entry_is_ignored() {
        var config = new InMemoryConfig(Clock.systemUTC());
        var log = new WriteAheadLogImpl(config);
        log.writeEntry("hello".getBytes(StandardCharsets.UTF_8), "greeting");
        log.writeEntry("world".getBytes(StandardCharsets.UTF_8), "greeting");

        var rawLog = config.toByteArray();
        var truncatedLog = Arrays.copyOf(rawLog, rawLog.length - 8);

        var otherLog = new WriteAheadLogImpl(new InMemoryConfig(Clock.systemUTC(), truncatedLog));
        var entries = StreamSupport.stream(otherLog.spliterator(), false).toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).data()).asString(StandardCharsets.UTF_8).isEqualTo("hello");
    }
}
