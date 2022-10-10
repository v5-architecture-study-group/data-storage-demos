package foo.v5archstudygroup.demos.orchestratedsaga.googlapps;

import foo.v5archstudygroup.demos.orchestratedsaga.api.AddGoogleAppsUserCommand;
import foo.v5archstudygroup.demos.orchestratedsaga.api.AddGoogleAppsUserResponse;
import foo.v5archstudygroup.demos.orchestratedsaga.api.GoogleAppsGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GoogleAppsService extends GoogleAppsGrpc.GoogleAppsImplBase {

    public static final int SUCCESS = 0;
    public static final int DUPLICATE = 1;

    private final ConcurrentMap<String, User> userMap = new ConcurrentHashMap<>();

    @Override
    public void addUser(AddGoogleAppsUserCommand request, StreamObserver<AddGoogleAppsUserResponse> responseObserver) {
        System.out.print("Adding user " + request.getEmailAddress() + "...");
        if (userMap.putIfAbsent(request.getEmailAddress(), new User(request.getEmailAddress(), request.getFullName())) == null) {
            System.out.println("Added");
            responseObserver.onNext(AddGoogleAppsUserResponse.newBuilder().setStatus(SUCCESS).setDescription("User successfully added").build());
        } else {
            System.out.println("Already added");
            responseObserver.onNext(AddGoogleAppsUserResponse.newBuilder().setStatus(DUPLICATE).setDescription("E-mail address already taken").build());
        }
        responseObserver.onCompleted();
    }

    public record User(String emailAddress, String fullName) {
    }
}
