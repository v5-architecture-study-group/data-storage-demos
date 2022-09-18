package foo.v5archstudygroup.demos.distributedlog.store;

@FunctionalInterface
public interface Deserializer<T> {

    T deserialize(byte[] data);
}
