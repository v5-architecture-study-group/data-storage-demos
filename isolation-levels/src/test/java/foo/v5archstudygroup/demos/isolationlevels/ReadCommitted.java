package foo.v5archstudygroup.demos.isolationlevels;

import foo.v5archstudygroup.demos.isolationlevels.entity.Unit;
import foo.v5archstudygroup.demos.isolationlevels.entity.UnitRepository;
import foo.v5archstudygroup.demos.isolationlevels.entity.UnitStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.time.Clock;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ReadCommitted {

    private static final Logger LOG = LoggerFactory.getLogger(ReadCommitted.class);

    @Autowired
    UnitRepository unitRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    EntityManager entityManager;

    @Autowired
    Clock clock;

    TransactionTemplate txTemplate;

    @BeforeEach
    public void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        for (int i = 10; i < 99; ++i) {
            unitRepository.saveAndFlush(new Unit("RVS" + i + "1"));
        }
    }

    @AfterEach
    public void tearDown() {
        unitRepository.deleteAll();
    }

    @Test
    public void transactions_cannot_see_each_others_changes_before_committing() throws Exception {
        // These semaphores are just used to make sure the threads do what they are expected to, in the correct
        // order.
        var writeSemaphore = new Semaphore(0);
        var readSemaphore = new Semaphore(0);
        var writeThread = new Thread(() -> {
            LOG.info("Transaction begins");
            txTemplate.executeWithoutResult(tx -> {
                try {
                    writeSemaphore.acquire();
                    {
                        var unit = unitRepository.getByCallSign("RVS911");
                        unit.updateStatus(clock, UnitStatus.EN_ROUTE);
                        unitRepository.saveAndFlush(unit);
                        LOG.info("Changed status to EN_ROUTE");
                    }
                    readSemaphore.release();

                    writeSemaphore.acquire();
                    {
                        var unit = unitRepository.getByCallSign("RVS911");
                        unit.updateStatus(clock, UnitStatus.ON_SCENE);
                        unitRepository.saveAndFlush(unit);
                        LOG.info("Changed status to ON_SCENE");
                    }
                } catch (InterruptedException ignoreIt) {
                }
            });
            LOG.info("Transaction committed");
        });
        var readThread = new Thread(() -> {
            LOG.info("Transaction begins");
            txTemplate.executeWithoutResult(tx -> {
                try {
                    LOG.info("Checking initial status, assuming UNKNOWN");
                    assertThat(unitRepository.getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.UNKNOWN);
                    writeSemaphore.release();

                    readSemaphore.acquire();
                    LOG.info("Checking status, still assuming UNKNOWN");
                    entityManager.clear(); // Without this, we will get a cached version when we run the query below
                    assertThat(unitRepository.getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.UNKNOWN);
                    writeSemaphore.release();
                } catch (InterruptedException ignoreIt) {
                }
            });
            LOG.info("Transaction committed");
        });

        writeThread.start();
        readThread.start();

        writeThread.join();
        readThread.join();

        assertThat(unitRepository.getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.ON_SCENE);
    }

    @Test
    public void the_same_query_inside_the_same_transaction_returns_different_results_if_another_transaction_commits_changes_in_between() throws Exception {
        // These semaphores are just used to make sure the threads do what they are expected to, in the correct
        // order.
        var writeSemaphore = new Semaphore(0);
        var readSemaphore = new Semaphore(0);
        var writeThread = new Thread(() -> {
            LOG.info("Transaction begins");
            txTemplate.executeWithoutResult(tx -> {
                try {
                    writeSemaphore.acquire();
                    {
                        var unit = unitRepository.getByCallSign("RVS911");
                        unit.updateStatus(clock, UnitStatus.EN_ROUTE);
                        unitRepository.saveAndFlush(unit);
                        LOG.info("Changed status to EN_ROUTE");
                    }
                } catch (InterruptedException ignoreIt) {
                }
            });
            LOG.info("Transaction committed");
            readSemaphore.release();
        });
        var readThread = new Thread(() -> {
            LOG.info("Transaction begins");
            txTemplate.executeWithoutResult(tx -> {
                try {
                    LOG.info("Checking initial status, assuming UNKNOWN");
                    assertThat(unitRepository.getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.UNKNOWN);
                    writeSemaphore.release();

                    readSemaphore.acquire();
                    LOG.info("Checking status, assuming EN_ROUTE");
                    entityManager.clear(); // Without this, we will get a cached version when we run the query below
                    assertThat(unitRepository.getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.EN_ROUTE);
                } catch (InterruptedException ignoreIt) {
                }
            });
            LOG.info("Transaction committed");
        });

        writeThread.start();
        readThread.start();

        writeThread.join();
        readThread.join();

        assertThat(unitRepository.getByCallSign("RVS911").getStatus()).isEqualTo(UnitStatus.EN_ROUTE);
    }
}
