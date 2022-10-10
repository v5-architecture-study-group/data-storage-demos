package foo.v5archstudygroup.demos.orchestratedsaga.orchestrator;

import foo.v5archstudygroup.demos.orchestratedsaga.api.*;

public class AddEmployeeSagaProcessor {

    private final GoogleAppsGrpc.GoogleAppsBlockingStub googleApps;
    private final WorkHoursGrpc.WorkHoursBlockingStub workHours;
    private final BambooHRGrpc.BambooHRBlockingStub bambooHR;

    public AddEmployeeSagaProcessor(GoogleAppsGrpc.GoogleAppsBlockingStub googleApps,
                                    WorkHoursGrpc.WorkHoursBlockingStub workHours,
                                    BambooHRGrpc.BambooHRBlockingStub bambooHR) {
        this.googleApps = googleApps;
        this.workHours = workHours;
        this.bambooHR = bambooHR;
    }

    public AddEmployeeSaga.SagaState process(AddEmployeeSaga saga) {
        System.out.println("Processing saga " + saga);
        addToGoogle(saga);
        addToBambooHR(saga);
        addToWorkHours(saga);
        return saga.getState();
    }

    private void addToGoogle(AddEmployeeSaga saga) {
        if (saga.getGoogleState() == AddEmployeeSaga.SyncState.PENDING) {
            System.out.println("Adding " + saga.getEmailAddress() + " to Google");
            saga.setGoogleState(AddEmployeeSaga.SyncState.ADDING);
            try {
                var response = googleApps.addUser(AddGoogleAppsUserCommand.newBuilder()
                        .setEmailAddress(saga.getEmailAddress())
                        .setFullName(saga.getFullName())
                        .build());
                if (response.getStatus() == 0) {
                    System.out.println("Employee " + saga.getEmailAddress() + " has been added to Google");
                    saga.setGoogleState(AddEmployeeSaga.SyncState.ADDED);
                } else {
                    System.err.println("Error adding " + saga.getEmailAddress() + " to Google Apps: " + response.getDescription());
                    saga.setGoogleState(AddEmployeeSaga.SyncState.ERROR);
                }
            } catch (Exception ex) {
                // There was an error contacting Google Apps. We have to try again later.
                System.err.println("Could not contact Google, trying again later");
                saga.setGoogleState(AddEmployeeSaga.SyncState.PENDING);
            }
        }
    }

    private void addToBambooHR(AddEmployeeSaga saga) {
        // BambooHR requires a valid e-mail address, so we have to be synced with Google Apps before we can proceed.
        if (saga.getGoogleState() != AddEmployeeSaga.SyncState.ADDED) {
            System.out.println("Cannot proceed because employee " + saga.getEmailAddress() + " has not been added to Google yet");
            return;
        }

        if (saga.getBambooState() == AddEmployeeSaga.SyncState.PENDING) {
            System.out.println("Adding " + saga.getEmailAddress() + " to BambooHR");
            saga.setBambooState(AddEmployeeSaga.SyncState.ADDING);
            try {
                var response = bambooHR.addEmployee(AddBambooHREmployeeCommand.newBuilder()
                        .setFirstName(saga.getFirstName())
                        .setLastName(saga.getLastName())
                        .setEmailAddress(saga.getEmailAddress())
                        .setDepartment(saga.getDepartment())
                        .build());
                if (response.getStatus() == 0) {
                    System.out.println("Employee " + saga.getEmailAddress() + " has been added to BambooHR");
                    saga.setBambooState(AddEmployeeSaga.SyncState.ADDED);
                } else if (response.getStatus() == 1) {
                    System.out.println("Looks like the employee " + saga.getEmailAddress() + " has already been added to BambooHR");
                    saga.setBambooState(AddEmployeeSaga.SyncState.ADDED);
                } else {
                    System.out.println("Error adding " + saga.getEmailAddress() + " to BambooHR: " + response.getDescription());
                    saga.setBambooState(AddEmployeeSaga.SyncState.ERROR);
                }
            } catch (Exception ex) {
                System.err.println("Could not contact BambooHR, trying again later");
                saga.setBambooState(AddEmployeeSaga.SyncState.PENDING);
            }
        }
    }

    private void addToWorkHours(AddEmployeeSaga saga) {
        if (saga.getWorkHoursState() == AddEmployeeSaga.SyncState.PENDING) {
            System.out.println("Adding " + saga.getEmailAddress() + " to WorkHours");
            saga.setWorkHoursState(AddEmployeeSaga.SyncState.ADDING);
            try {
                var response = workHours.addEmployee(AddWorkHoursEmployeeCommand.newBuilder()
                        .setFirstName(saga.getFirstName())
                        .setLastName(saga.getLastName())
                        .build());
                if (response.getStatus() == 0) {
                    System.out.println("Employee " + saga.getEmailAddress() + " has been added to WorkHours");
                    saga.setWorkHoursState(AddEmployeeSaga.SyncState.ADDED);
                } else {
                    System.out.println("Error adding " + saga.getEmailAddress() + " to WorkHours: " + response.getDescription());
                    saga.setWorkHoursState(AddEmployeeSaga.SyncState.ERROR);
                }
            } catch (Exception ex) {
                System.err.println("Could not contact WorkHours, trying again later");
                saga.setWorkHoursState(AddEmployeeSaga.SyncState.PENDING);
            }
        }
    }


}
