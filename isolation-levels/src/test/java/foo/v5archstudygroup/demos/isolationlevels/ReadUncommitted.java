package foo.v5archstudygroup.demos.isolationlevels;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ReadUncommitted extends BaseTest {

    @Autowired
    ReadUncommitted(PlatformTransactionManager transactionManager, EntityManager entityManager, UnitRepository unitRepository) {
        super(transactionManager, entityManager, unitRepository);
    }

    @Test
    public void transactions_can_see_each_others_changes_before_committing() {
        var writeThread = CompletableFuture.runAsync(() -> {
            runInTransaction(() -> {
                awaitWritePermission();
                {
                    var unit = getByCallSign("RVS911");
                    unit.updateStatus(Clock.systemDefaultZone(), UnitStatus.EN_ROUTE);
                    saveAndFlush(unit);
                    log.info("Changed status to EN_ROUTE");
                }
                allowReading();

                awaitWritePermission();
                {
                    var unit = getByCallSign("RVS911");
                    unit.updateStatus(Clock.systemDefaultZone(), UnitStatus.ON_SCENE);
                    saveAndFlush(unit);
                    log.info("Changed status to ON_SCENE");
                }
            });
        });
        var readThread = CompletableFuture.runAsync(() -> {
            runInTransaction(() -> {
                log.info("Checking initial status, assuming UNKNOWN");
                assertThat(getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.UNKNOWN);
                allowWriting();

                awaitReadPermission();
                log.info("Checking status, assuming EN_ROUTE");
                assertThat(getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.EN_ROUTE);
                allowWriting();
            });
        });

        readThread.join();
        writeThread.join();

        assertThat(getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.ON_SCENE);
    }

    @Override
    protected int getIsolationLevel() {
        return TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
    }
}
