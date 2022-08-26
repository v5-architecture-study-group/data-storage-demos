package foo.v5archstudygroup.demos.idempotence.event;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderShippedEvent(
        UUID orderId,
        Instant orderReceivedOn,
        Instant orderShippedOn,
        Recipient shipTo,
        Recipient billTo,
        BigDecimal shipmentCosts,
        List<OrderItem> items
) {

    public record Recipient(
            String name,
            String streetAddress,
            String postalCode,
            String postOffice,
            String country
    ) {
    }

    public record OrderItem(
            UUID productId,
            String name,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice
    ) {
    }
}
