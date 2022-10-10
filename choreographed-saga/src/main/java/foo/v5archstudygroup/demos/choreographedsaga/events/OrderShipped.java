package foo.v5archstudygroup.demos.choreographedsaga.events;

public record OrderShipped(
        OrderInfo orderInfo,
        ShipmentInfo shipmentInfo
) {
}
