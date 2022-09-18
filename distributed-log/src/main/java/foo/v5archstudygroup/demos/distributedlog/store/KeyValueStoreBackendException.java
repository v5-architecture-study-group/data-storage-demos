package foo.v5archstudygroup.demos.distributedlog.store;

public class KeyValueStoreBackendException extends RuntimeException {

    public KeyValueStoreBackendException() {
    }

    public KeyValueStoreBackendException(String message) {
        super(message);
    }

    public KeyValueStoreBackendException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyValueStoreBackendException(Throwable cause) {
        super(cause);
    }
}
