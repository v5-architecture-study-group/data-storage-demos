package foo.v5archstudygroup.demos.choreographedsaga.orders;

import foo.v5archstudygroup.demos.choreographedsaga.AbstractApp;
import foo.v5archstudygroup.demos.choreographedsaga.events.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class OrdersApp extends AbstractApp {

    public OrdersApp(String host) throws IOException, TimeoutException {
        super(host);
        consume(Constants.SHIPMENTS_STREAM, OrderShipped.class, this::onOrderShipped);
        consume(Constants.INVOICES_STREAM, InvoiceCreated.class, this::onInvoiceCreated);
    }

    public void publish(OrderReceived orderReceived) {
        publish(Constants.ORDERS_STREAM, orderReceived);
    }

    public void onOrderShipped(OrderShipped orderShipped) {
        logger.info("Order shipped {}", orderShipped.orderInfo().orderId());
        // Here you would update the state of the local database
    }

    public void onInvoiceCreated(InvoiceCreated invoiceCreated) {
        logger.info("Order invoiced {}", invoiceCreated.orderInfo().orderId());
        // Here you would update the state of the local database
    }

    public static void main(String[] args) throws Exception {
        var app = new OrdersApp("localhost");
        app.publish(new OrderReceived(new OrderInfo(UUID.randomUUID().toString(), Instant.now(),
                new Address("Petter Holmström / Vaadin Ltd", "Köpmansgatan 18", "21600", "Pargas", "Finland"),
                new Address("Vaadin Ltd", "Ruukinkatu 2-4", "20540", "Turku", "Finland"),
                List.of(
                        new OrderItem(UUID.randomUUID().toString(), "Domain Modeling Made Functional", 1, new BigDecimal("39.99"), new BigDecimal("0.24")),
                        new OrderItem(UUID.randomUUID().toString(), "Shipment costs", 1, new BigDecimal("5.99"), new BigDecimal("0"))
                ))));
    }
}
