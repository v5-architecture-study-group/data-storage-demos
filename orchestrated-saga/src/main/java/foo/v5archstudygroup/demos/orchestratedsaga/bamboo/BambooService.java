package foo.v5archstudygroup.demos.orchestratedsaga.bamboo;

import foo.v5archstudygroup.demos.orchestratedsaga.api.AddBambooHREmployeeCommand;
import foo.v5archstudygroup.demos.orchestratedsaga.api.AddBambooHREmployeeResponse;
import foo.v5archstudygroup.demos.orchestratedsaga.api.BambooHRGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BambooService extends BambooHRGrpc.BambooHRImplBase {

    public static final int SUCCESS = 0;
    public static final int DUPLICATE = 1;

    private final ConcurrentMap<String, Employee> employeeMap = new ConcurrentHashMap<>();

    @Override
    public void addEmployee(AddBambooHREmployeeCommand request, StreamObserver<AddBambooHREmployeeResponse> responseObserver) {
        System.out.print("Adding employee " + request.getEmailAddress() + " ...");
        if (employeeMap.putIfAbsent(request.getEmailAddress(), new Employee(request.getFirstName(), request.getFirstName(), request.getEmailAddress(), request.getDepartment())) == null) {
            System.out.println("Added");
            responseObserver.onNext(AddBambooHREmployeeResponse.newBuilder().setStatus(SUCCESS).setDescription("Employee successfully added").build());
        } else {
            System.out.println("Already added");
            responseObserver.onNext(AddBambooHREmployeeResponse.newBuilder().setStatus(DUPLICATE).setDescription("An employee with the given e-mail address already exists").build());
        }
        responseObserver.onCompleted();
    }

    public record Employee(String firstName, String lastName, String email, String department) {
    }
}
