package foo.v5archstudygroup.demos.idempotence.orchestrator;

import foo.v5archstudygroup.demos.idempotence.entity.InvoiceRepository;
import foo.v5archstudygroup.demos.idempotence.entity.ReceivedOrder;
import foo.v5archstudygroup.demos.idempotence.entity.ReceivedOrderRepository;
import foo.v5archstudygroup.demos.idempotence.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;


@Service
public class InvoiceCreator {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceCreator.class);

    private final ReceivedOrderRepository receivedOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceFactory invoiceFactory;
    private final TransactionTemplate transactionTemplate;

    public InvoiceCreator(ReceivedOrderRepository receivedOrderRepository, InvoiceRepository invoiceRepository,
                          InvoiceFactory invoiceFactory, PlatformTransactionManager transactionManager) {
        this.receivedOrderRepository = receivedOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceFactory = invoiceFactory;
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener
    public void onOrderShippedEvent(OrderShippedEvent event) {
        try {
            // Please note that this is one way of achieving idempotence in this particular use case. It is not
            // the only way.
            LOG.info("Attempting to create invoice for shipped order {}", event.orderId());
            transactionTemplate.executeWithoutResult(tx -> {
                // We use the order ID to determine whether we have already created an invoice for this order.
                var orderId = event.orderId().toString();
                if (!receivedOrderRepository.existsByOrderId(orderId)) {
                    var order = receivedOrderRepository.saveAndFlush(new ReceivedOrder(orderId));
                    var invoice = invoiceFactory.createInvoice(order, event);
                    invoiceRepository.saveAndFlush(invoice);
                    // If the same event arrives at the same time, one of the transactions will fail because the orderId
                    // has a unique key, making sure we can only process a received event exactly once.
                }
            });
        } catch (DataIntegrityViolationException ex) {
            // Ignore it, this just means we have seen the event before.
            LOG.info("Shipped order {} has already been processed, ignoring", event.orderId());
        }
    }
}
