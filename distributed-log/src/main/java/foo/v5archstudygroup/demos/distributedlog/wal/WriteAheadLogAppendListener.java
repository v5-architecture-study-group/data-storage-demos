package foo.v5archstudygroup.demos.distributedlog.wal;

@FunctionalInterface
public interface WriteAheadLogAppendListener {

    void onEntryAppended(Entry entry);
}
