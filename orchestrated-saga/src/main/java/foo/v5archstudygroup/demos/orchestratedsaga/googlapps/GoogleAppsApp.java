package foo.v5archstudygroup.demos.orchestratedsaga.googlapps;

import io.grpc.ServerBuilder;

import java.util.Scanner;

public class GoogleAppsApp {

    public static void main(String[] args) throws Exception {
        var server = ServerBuilder.forPort(9002).addService(new GoogleAppsService()).build().start();
        var scanner = new Scanner(System.in);
        System.out.println("Press enter to exit");
        scanner.nextLine();
        server.shutdownNow();
        server.awaitTermination();
    }
}
