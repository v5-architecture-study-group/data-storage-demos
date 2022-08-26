package foo.v5archstudygroup.demos.idempotence.orchestrator;

import foo.v5archstudygroup.demos.idempotence.entity.Invoice;
import foo.v5archstudygroup.demos.idempotence.entity.InvoiceItem;
import foo.v5archstudygroup.demos.idempotence.entity.ReceivedOrder;
import foo.v5archstudygroup.demos.idempotence.event.OrderShippedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;

@Service
public class InvoiceFactory {

    private final Clock clock;
    private final Period defaultTerms;

    public InvoiceFactory(Clock clock, @Value("${application.parameters.default-terms:P14D}") Period defaultTerms) {
        this.clock = clock;
        this.defaultTerms = defaultTerms;
    }

    public Invoice createInvoice(ReceivedOrder receivedOrder, OrderShippedEvent orderShippedEvent) {
        if (!receivedOrder.getOrderId().equals(orderShippedEvent.orderId().toString())) {
            throw new IllegalArgumentException("OrderIds do not match");
        }
        var invoiceDate = LocalDate.now(clock);
        var items = new HashSet<InvoiceItem>();
        orderShippedEvent.items().forEach(item -> items.add(new InvoiceItem(item.name(), item.quantity(), item.unit(), item.unitPrice())));
        items.add(new InvoiceItem("Shipment costs", new BigDecimal(1), "N/A", orderShippedEvent.shipmentCosts()));
        return new Invoice(
                receivedOrder,
                invoiceDate,
                calculateDueDate(invoiceDate),
                orderShippedEvent.billTo().name(),
                orderShippedEvent.billTo().streetAddress(),
                orderShippedEvent.billTo().postalCode(),
                orderShippedEvent.billTo().postOffice(),
                orderShippedEvent.billTo().country(),
                items
        );
    }

    private LocalDate calculateDueDate(LocalDate invoiceDate) {
        return invoiceDate.plus(defaultTerms);
    }
}
