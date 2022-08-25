package foo.v5archstudygroup.demos.optimisticlocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OptimisticLockingDemo {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(OptimisticLockingDemo.class, args);
    }

}