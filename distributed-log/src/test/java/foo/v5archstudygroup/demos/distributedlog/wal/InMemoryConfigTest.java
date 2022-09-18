package foo.v5archstudygroup.demos.distributedlog.wal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryConfigTest {

    // TODO continue here

    @Test
    public void clock_returns_passed_in_clock() {
        var clock = Clock.systemUTC();
        var config = new InMemoryConfig(clock);
        assertThat(config.clock()).isSameAs(clock);
    }

    @Test
    public void empty_config_returns_empty_array() {
        var config = new InMemoryConfig(Clock.systemUTC());
        assertThat(config.toByteArray()).isEmpty();
    }

    @Test
    public void written_bytes_end_up_in_array() throws IOException {
        var config = new InMemoryConfig(Clock.systemUTC());
        config.getOutputStream().write("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(config.toByteArray()).asString(StandardCharsets.UTF_8).isEqualTo("hello");
    }

    @Test
    public void bytes_in_array_are_read_from_input_stream() throws IOException {
        var config = new InMemoryConfig(Clock.systemUTC(), "hello".getBytes(StandardCharsets.UTF_8));
        assertThat(config.getInputStream().readAllBytes()).asString(StandardCharsets.UTF_8).isEqualTo("hello");
    }

    @Test
    public void resetting_the_output_stream_discards_remaining_bytes() throws IOException {
        var config = new InMemoryConfig(Clock.systemUTC(), "hello".getBytes(StandardCharsets.UTF_8));
        config.resetOutputStream(3);
        assertThat(config.toByteArray()).asString(StandardCharsets.UTF_8).isEqualTo("hel");
        config.getOutputStream().write("world".getBytes(StandardCharsets.UTF_8));
        assertThat(config.toByteArray()).asString(StandardCharsets.UTF_8).isEqualTo("helworld");
    }

    // TODO Right now, the position will be messed up by reading and writing. We need a test that discovers this (and a fix of course).
}
