package foo.v5archstudygroup.demos.orchestratedsaga.orchestrator;

import foo.v5archstudygroup.demos.orchestratedsaga.api.BambooHRGrpc;
import foo.v5archstudygroup.demos.orchestratedsaga.api.GoogleAppsGrpc;
import foo.v5archstudygroup.demos.orchestratedsaga.api.WorkHoursGrpc;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Orchestrator {

    private final List<AddEmployeeSaga> activeSagas = new ArrayList<>();
    private final AddEmployeeSagaProcessor processor;

    public Orchestrator(AddEmployeeSagaProcessor processor) {
        this.processor = processor;
    }

    public synchronized void addSaga(AddEmployeeSaga saga) {
        activeSagas.add(saga);
    }

    public synchronized void processSagas() {
        var it = activeSagas.iterator();
        while (it.hasNext()) {
            var saga = it.next();
            if (processor.process(saga) == AddEmployeeSaga.SagaState.DONE) {
                System.out.println("Removing saga " + saga + " after successful processing");
                it.remove();
            } else {
                System.out.println("Leaving saga " + saga + " in the queue");
            }
        }
    }

    private static BambooHRGrpc.BambooHRBlockingStub createBambooHRClient() {
        var channel = NettyChannelBuilder.forAddress("localhost", 9001).usePlaintext().build();
        return BambooHRGrpc.newBlockingStub(channel);
    }

    private static GoogleAppsGrpc.GoogleAppsBlockingStub createGoogleAppsClient() {
        var channel = NettyChannelBuilder.forAddress("localhost", 9002).usePlaintext().build();
        return GoogleAppsGrpc.newBlockingStub(channel);
    }

    private static WorkHoursGrpc.WorkHoursBlockingStub createWorkHoursClient() {
        var channel = NettyChannelBuilder.forAddress("localhost", 9003).usePlaintext().build();
        return WorkHoursGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) throws Exception {
        var processor = new AddEmployeeSagaProcessor(createGoogleAppsClient(), createWorkHoursClient(), createBambooHRClient());
        var orchestrator = new Orchestrator(processor);
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(orchestrator::processSagas, 0, 1000, TimeUnit.MILLISECONDS);

        orchestrator.addSaga(new AddEmployeeSaga("Joe", "Cool", "joecool@foo.bar", "Services"));
        orchestrator.addSaga(new AddEmployeeSaga("Maxwell", "Smart", "maxwellsmart@foo.bar", "Sales"));

        var scanner = new Scanner(System.in);
        System.out.println("Press enter to exit");
        scanner.nextLine();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}