package foo.v5archstudygroup.demos.orchestratedsaga.workhours;

import io.grpc.ServerBuilder;

import java.util.Scanner;

public class WorkHoursApp {

    public static void main(String[] args) throws Exception {
        var server = ServerBuilder.forPort(9003).addService(new WorkHoursService()).build().start();
        var scanner = new Scanner(System.in);
        System.out.println("Press enter to exit");
        scanner.nextLine();
        server.shutdownNow();
        server.awaitTermination();
    }
}
