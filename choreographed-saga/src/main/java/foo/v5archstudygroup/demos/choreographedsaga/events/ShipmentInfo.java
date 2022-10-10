package foo.v5archstudygroup.demos.choreographedsaga.events;

import java.time.Instant;
import java.util.List;

public record ShipmentInfo(
        String shipmentId,
        String orderId,
        Instant shipmentTimestamp,
        Address shipmentAddress,
        List<ShipmentItem> items
) {
}
