package foo.v5archstudygroup.demos.choreographedsaga.events;

public record InvoiceCreated(
        OrderInfo orderInfo,
        InvoiceInfo invoiceInfo
) {
}
