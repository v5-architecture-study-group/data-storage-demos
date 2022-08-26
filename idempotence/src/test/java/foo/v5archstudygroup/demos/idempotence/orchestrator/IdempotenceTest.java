package foo.v5archstudygroup.demos.idempotence.orchestrator;

import foo.v5archstudygroup.demos.idempotence.entity.InvoiceRepository;
import foo.v5archstudygroup.demos.idempotence.event.OrderShippedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class IdempotenceTest {

    private final ApplicationContext applicationContext;
    private final InvoiceRepository invoiceRepository;

    @Autowired
    public IdempotenceTest(ApplicationContext applicationContext, InvoiceRepository invoiceRepository) {
        this.applicationContext = applicationContext;
        this.invoiceRepository = invoiceRepository;
    }

    @Test
    public void only_one_invoice_is_created_even_though_multiple_events_arrive() throws Exception {
        var event = new OrderShippedEvent(UUID.randomUUID(), Instant.now().minusSeconds(86400), Instant.now(),
                new OrderShippedEvent.Recipient("Joe Cool", "ACME Street 123", "12345", "Acme City", "Finland"),
                new OrderShippedEvent.Recipient("Maxwell Smart", "Spy Road 86", "98765", "Agentswill", "Finland"),
                new BigDecimal("19.99"),
                List.of(new OrderShippedEvent.OrderItem(UUID.randomUUID(), "Shoe with answering machine", new BigDecimal(2), "pair", new BigDecimal("599.99"))));
        var executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; ++i) {
            executor.execute(() -> applicationContext.publishEvent(event));
        }
        executor.shutdown();
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

        var createdInvoices = invoiceRepository.findByOrderId(event.orderId().toString());
        assertThat(createdInvoices).hasSize(1);
    }
}
