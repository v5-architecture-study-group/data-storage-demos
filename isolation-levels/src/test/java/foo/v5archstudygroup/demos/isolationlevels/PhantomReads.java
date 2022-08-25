package foo.v5archstudygroup.demos.isolationlevels;

import foo.v5archstudygroup.demos.isolationlevels.entity.Unit;
import foo.v5archstudygroup.demos.isolationlevels.entity.UnitRepository;
import foo.v5archstudygroup.demos.isolationlevels.entity.UnitStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import javax.persistence.EntityManager;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:derby:memory:isolation_level_demo_rr;create=true" // Derby
)
public class PhantomReads extends BaseTest {

    @Autowired
    PhantomReads(PlatformTransactionManager transactionManager, EntityManager entityManager, UnitRepository unitRepository) {
        super(transactionManager, entityManager, unitRepository);
    }

    // Separate example for this case as we have to use Derby for this. H2 prevents phantom reads even at this isolation level.

    @Test
    public void rows_inserted_by_other_transactions_still_show_up() {
        var writeThread = CompletableFuture.runAsync(() -> {
            runInTransaction(() -> {
                awaitWritePermission();
                log.info("Inserting new unit");
                var unit = new Unit("RVS917");
                unit.updateStatus(Clock.systemDefaultZone(), UnitStatus.AVAILABLE_ON_RADIO);
                saveAndFlush(unit);
            });
            allowReading();
        });
        var readThread = CompletableFuture.runAsync(() -> {
            runInTransaction(() -> {
                var resultsBeforeInsert = findByStatus(UnitStatus.AVAILABLE_ON_RADIO);
                log.info("Getting units before insert: " + resultsBeforeInsert);
                allowWriting();

                awaitReadPermission();
                var resultsAfterInsert = findByStatus(UnitStatus.AVAILABLE_ON_RADIO);
                log.info("Getting units after insert: " + resultsAfterInsert);
                assertThat(resultsAfterInsert.size()).isEqualTo(resultsBeforeInsert.size() + 1);
            });
        });

        writeThread.join();
        readThread.join();

        log.info("Units after both transactions: " + findByStatus(UnitStatus.AVAILABLE_ON_RADIO).size());
    }

    @Override
    protected int getIsolationLevel() {
        return TransactionDefinition.ISOLATION_REPEATABLE_READ;
    }
}
