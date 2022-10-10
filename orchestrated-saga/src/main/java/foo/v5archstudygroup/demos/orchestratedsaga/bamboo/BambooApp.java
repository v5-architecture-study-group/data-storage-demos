package foo.v5archstudygroup.demos.orchestratedsaga.bamboo;

import io.grpc.ServerBuilder;

import java.util.Scanner;

public class BambooApp {

    public static void main(String[] args) throws Exception {
        var server = ServerBuilder.forPort(9001).addService(new BambooService()).build().start();
        var scanner = new Scanner(System.in);
        System.out.println("Press enter to exit");
        scanner.nextLine();
        server.shutdownNow();
        server.awaitTermination();
    }
}
