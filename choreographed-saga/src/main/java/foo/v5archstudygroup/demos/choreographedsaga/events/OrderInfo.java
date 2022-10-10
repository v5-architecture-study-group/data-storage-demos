package foo.v5archstudygroup.demos.choreographedsaga.events;

import java.time.Instant;
import java.util.List;

public record OrderInfo(
        String orderId,
        Instant orderTimestamp,
        Address shipToAddress,
        Address billToAddress,
        List<OrderItem> items
) {
}
