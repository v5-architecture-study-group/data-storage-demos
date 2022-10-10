package foo.v5archstudygroup.demos.choreographedsaga.shipment;

import foo.v5archstudygroup.demos.choreographedsaga.AbstractApp;
import foo.v5archstudygroup.demos.choreographedsaga.events.*;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class ShipmentApp extends AbstractApp {

    public ShipmentApp(String host) throws IOException, TimeoutException {
        super(host);
        consume(Constants.ORDERS_STREAM, OrderReceived.class, this::onOrderReceived);
    }

    public void onOrderReceived(OrderReceived orderReceived) {
        logger.info("Processing {}", orderReceived);
        // Here, you would do something with the event. Eventually, the order is shipped, and we generate our own event.
        var event = new OrderShipped(orderReceived.orderInfo(),
                new ShipmentInfo(UUID.randomUUID().toString(),
                        orderReceived.orderInfo().orderId(),
                        Instant.now(),
                        orderReceived.orderInfo().shipToAddress(),
                        orderReceived.orderInfo().items().stream().map(orderItem -> new ShipmentItem(orderItem.productId(), orderItem.description(), orderItem.quantity())).toList()));
        publish(event);
    }

    public void publish(OrderShipped orderShipped) {
        publish(Constants.SHIPMENTS_STREAM, orderShipped);
    }

    public static void main(String[] args) throws Exception {
        new ShipmentApp("localhost");
    }
}
