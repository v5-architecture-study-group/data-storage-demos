package foo.v5archstudygroup.demos.choreographedsaga.invoicing;

import foo.v5archstudygroup.demos.choreographedsaga.AbstractApp;
import foo.v5archstudygroup.demos.choreographedsaga.events.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class InvoicingApp extends AbstractApp {

    public InvoicingApp(String host) throws IOException, TimeoutException {
        super(host);
        consume(Constants.SHIPMENTS_STREAM, OrderShipped.class, this::onOrderShipped);
    }

    public void onOrderShipped(OrderShipped orderShipped) {
        logger.info("Processing {}", orderShipped);
        // Here, you would do something with the event. Eventually, the invoice is created, and we generate our own event.
        var event = new InvoiceCreated(orderShipped.orderInfo(),
                new InvoiceInfo(UUID.randomUUID().toString(),
                        orderShipped.orderInfo().orderId(),
                        orderShipped.orderInfo().billToAddress(),
                        orderShipped.orderInfo().items().stream().map(orderItem -> new InvoiceItem(orderItem.productId(), orderItem.description(), orderItem.quantity(), orderItem.itemPrice(), orderItem.vatPercentage())).toList()));
        publish(event);
    }

    public void publish(InvoiceCreated invoiceCreated) {
        publish(Constants.INVOICES_STREAM, invoiceCreated);
    }

    public static void main(String[] args) throws Exception {
        new InvoicingApp("localhost");
    }
}
