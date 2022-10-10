package foo.v5archstudygroup.demos.orchestratedsaga.workhours;

import foo.v5archstudygroup.demos.orchestratedsaga.api.AddWorkHoursEmployeeCommand;
import foo.v5archstudygroup.demos.orchestratedsaga.api.AddWorkHoursEmployeeResponse;
import foo.v5archstudygroup.demos.orchestratedsaga.api.WorkHoursGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicInteger;

public class WorkHoursService extends WorkHoursGrpc.WorkHoursImplBase {

    private final AtomicInteger nextId = new AtomicInteger();

    @Override
    public void addEmployee(AddWorkHoursEmployeeCommand request, StreamObserver<AddWorkHoursEmployeeResponse> responseObserver) {
        // Only simulate writing since there will be no reads
        System.out.println("Adding employee " + request.getFirstName() + " " + request.getLastName());
        responseObserver.onNext(AddWorkHoursEmployeeResponse.newBuilder().setStatus(0).setDescription("Employee added").setEmployeeId(nextId.incrementAndGet()).build());
        responseObserver.onCompleted();
    }
}
