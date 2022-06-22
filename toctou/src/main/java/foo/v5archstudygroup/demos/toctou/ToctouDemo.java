package foo.v5archstudygroup.demos.toctou;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class ToctouDemo {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    // We're not running this application, we only load it up inside test cases.
}