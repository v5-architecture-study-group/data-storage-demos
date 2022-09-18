package foo.v5archstudygroup.demos.distributedlog.store;

import foo.v5archstudygroup.demos.distributedlog.wal.Entry;
import foo.v5archstudygroup.demos.distributedlog.wal.WriteAheadLog;
import foo.v5archstudygroup.demos.distributedlog.wal.WriteAheadLogAppendListener;

import java.io.*;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class KeyValueStore {

    private final WriteAheadLog wal;
    private final KeyValueStoreBackend backend;
    @SuppressWarnings("FieldCanBeLocal")
    private final WriteAheadLogAppendListener appendListener = this::onWalEntryAppended;

    public KeyValueStore(WriteAheadLog wal, KeyValueStoreBackend backend) {
        this.wal = requireNonNull(wal);
        this.backend = requireNonNull(backend);
        wal.addAppendListener(appendListener, backend.readLastSeenWalEntry());
    }

    public <K, V> void put(K key, Serializer<K> keySerializer, V value, Serializer<V> valueSerializer) {
        wal.appendEntry(
                new PutCommand(
                        keySerializer.serialize(key),
                        valueSerializer.serialize(value)
                ).toByteArray(),
                PutCommand.TYPE
        );
    }

    public void put(Serializable key, Serializable value) {
        put(key, this::serialize, value, this::serialize);
    }

    public <K> void remove(K key, Serializer<K> keySerializer) {
        wal.appendEntry(
                new RemoveCommand(
                        keySerializer.serialize(key)
                ).toByteArray(),
                RemoveCommand.TYPE
        );
    }

    public void remove(Serializable key) {
        remove(key, this::serialize);
    }

    public <K, V> Optional<V> get(K key, Serializer<K> keySerializer, Deserializer<V> valueDeserializer) {
        return backend.read(keySerializer.serialize(key)).map(valueDeserializer::deserialize);
    }

    public Optional<Serializable> get(Serializable key) {
        return get(key, this::serialize, this::deserialize);
    }

    private void onWalEntryAppended(Entry entry) {
        if (entry.type().equals(PutCommand.TYPE)) {
            Command.fromByteArray(entry.data(), PutCommand.class).apply(backend);
        } else if (entry.type().equals(RemoveCommand.TYPE)) {
            Command.fromByteArray(entry.data(), RemoveCommand.class).apply(backend);
        }
        backend.writeLastSeenWalEntryId(entry.id());
    }

    private abstract static class Command {
        private byte[] key;

        public Command() {
        }

        public Command(byte[] key) {
            this.key = key;
        }

        protected byte[] key() {
            return key;
        }

        protected void write(DataOutput out) throws IOException {
            out.writeShort(key.length);
            out.write(key);
        }

        protected void read(DataInput in) throws IOException {
            var keyLength = in.readShort();
            key = new byte[keyLength];
            in.readFully(key);
        }

        public byte[] toByteArray() {
            try (var bos = new ByteArrayOutputStream();
                 var dos = new DataOutputStream(bos)) {
                write(dos);
                return bos.toByteArray();
            } catch (IOException ex) {
                throw new IllegalStateException("Could not convert command to byte array", ex);
            }
        }

        public static <C extends Command> C fromByteArray(byte[] data, Class<C> commandClass) {
            try (var bis = new ByteArrayInputStream(data);
                 var dis = new DataInputStream(bis)) {
                var command = commandClass.getConstructor().newInstance();
                command.read(dis);
                return command;
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not create command from byte array");
            }
        }

        protected abstract void apply(KeyValueStoreBackend backend);
    }

    private static class PutCommand extends Command {

        public static final String TYPE = "put";

        private byte[] value;

        public PutCommand() {
        }

        public PutCommand(byte[] key, byte[] value) {
            super(key);
            this.value = value;
        }

        @Override
        protected void write(DataOutput out) throws IOException {
            super.write(out);
            out.writeShort(value.length);
            out.write(value);
        }

        @Override
        protected void read(DataInput in) throws IOException {
            super.read(in);
            var valueLength = in.readShort();
            value = new byte[valueLength];
            in.readFully(value);
        }

        @Override
        protected void apply(KeyValueStoreBackend backend) {
            backend.write(key(), value);
        }
    }

    private static class RemoveCommand extends Command {

        public static final String TYPE = "remove";

        public RemoveCommand() {
        }

        public RemoveCommand(byte[] key) {
            super(key);
        }

        @Override
        protected void apply(KeyValueStoreBackend backend) {
            backend.remove(key());
        }
    }

    private byte[] serialize(Serializable object) {
        try (var bos = new ByteArrayOutputStream();
             var oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot serialize object", ex);
        }
    }

    private Serializable deserialize(byte[] data) {
        try (var bis = new ByteArrayInputStream(data);
             var ois = new ObjectInputStream(bis)) {
            return (Serializable) ois.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalArgumentException("Cannot deserialize object", ex);
        }
    }
}
