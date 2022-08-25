package foo.v5archstudygroup.demos.isolationlevels;

import foo.v5archstudygroup.demos.isolationlevels.entity.Unit;
import foo.v5archstudygroup.demos.isolationlevels.entity.UnitRepository;
import foo.v5archstudygroup.demos.isolationlevels.entity.UnitStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class BaseTest {

    private final Semaphore readSemaphore = new Semaphore(0);
    private final Semaphore writeSemaphore = new Semaphore(0);

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final UnitRepository unitRepository;

    BaseTest(PlatformTransactionManager transactionManager, EntityManager entityManager,
             UnitRepository unitRepository) {
        this.entityManager = entityManager;
        this.unitRepository = unitRepository;

        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setIsolationLevel(getIsolationLevel());
    }

    protected abstract int getIsolationLevel();

    @BeforeEach
    public void setUp() {
        for (int i = 10; i <= 99; ++i) {
            unitRepository.saveAndFlush(new Unit("RVS" + i + "1"));
        }
    }

    @AfterEach
    public void tearDown() {
        unitRepository.deleteAllInBatch();
    }

    protected void runInTransaction(Runnable job) {
        log.info("Transaction begins");
        try {
            transactionTemplate.executeWithoutResult(tx -> job.run());
            log.info("Transaction committed");
        } catch (RuntimeException ex) {
            log.error("Error in transaction", ex);
            throw ex;
        }
    }

    protected void allowReading() {
        log.debug("Allowing reading");
        readSemaphore.release();
    }

    protected void allowWriting() {
        log.debug("Allowing writing");
        writeSemaphore.release();
    }

    protected void awaitReadPermission() {
        try {
            log.debug("Awaiting read permission");
            if (!readSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                Assertions.fail("Could not acquire read permission");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void awaitWritePermission() {
        try {
            log.debug("Awaiting write permission");
            if (!writeSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                Assertions.fail("Could not acquire write permission");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected Unit getByCallSign(String callSign) {
        var unit = unitRepository.getByCallSign(callSign);
        // To always force the next query to the database instead of using the entity manager cache.
        entityManager.detach(unit);
        return unit;
    }

    protected void saveAndFlush(Unit unit) {
        unitRepository.saveAndFlush(unit);
        // To always force the next query to the database instead of using the entity manager cache.
        entityManager.detach(unit);
    }

    protected void deleteAndFlush(Unit unit) {
        unitRepository.delete(unit);
        unitRepository.flush();
    }

    protected List<Unit> findByStatus(UnitStatus status) {
        var result = unitRepository.findByStatusOrderByCallSign(status);
        // To always force the next query to the database instead of using the entity manager cache.
        //entityManager.clear();
        return result;
    }
}
