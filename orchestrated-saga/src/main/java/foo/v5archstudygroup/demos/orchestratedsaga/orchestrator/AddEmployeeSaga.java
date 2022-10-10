package foo.v5archstudygroup.demos.orchestratedsaga.orchestrator;

public class AddEmployeeSaga {
    private final String firstName;
    private final String lastName;
    private final String emailAddress;
    private final String department;

    private SyncState bambooState = SyncState.PENDING;
    private SyncState googleState = SyncState.PENDING;
    private SyncState workHoursState = SyncState.PENDING;

    public enum SyncState {
        PENDING, ADDING, ADDED, ERROR
    }

    public AddEmployeeSaga(String firstName, String lastName, String emailAddress, String department) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.department = department;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getDepartment() {
        return department;
    }

    public SyncState getBambooState() {
        return bambooState;
    }

    public void setBambooState(SyncState bambooState) {
        this.bambooState = bambooState;
    }

    public SyncState getGoogleState() {
        return googleState;
    }

    public void setGoogleState(SyncState googleState) {
        this.googleState = googleState;
    }

    public SyncState getWorkHoursState() {
        return workHoursState;
    }

    public void setWorkHoursState(SyncState workHoursState) {
        this.workHoursState = workHoursState;
    }

    public SagaState getState() {
        if (bambooState == SyncState.ERROR || googleState == SyncState.ERROR || workHoursState == SyncState.ERROR) {
            return SagaState.ERROR;
        }
        if (bambooState == SyncState.ADDED && googleState == SyncState.ADDED && workHoursState == SyncState.ADDED) {
            return SagaState.DONE;
        }
        return SagaState.PENDING;
    }

    public enum SagaState {
        PENDING, DONE, ERROR;
    }

    @Override
    public String toString() {
        return "AddEmployeeSaga{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", department='" + department + '\'' +
                ", bambooState=" + bambooState +
                ", googleState=" + googleState +
                ", workHoursState=" + workHoursState +
                '}';
    }
}
