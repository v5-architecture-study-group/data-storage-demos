package foo.v5archstudygroup.demos.distributedlog.wal;

import java.util.Iterator;

public interface Reader {

    Iterator<Entry> readEntries();
}
