package foo.v5archstudygroup.demos.distributedlog.store;

@FunctionalInterface
public interface Serializer<T> {

    byte[] serialize(T object);
}
