package foo.v5archstudygroup.demos.choreographedsaga.events;

import java.util.List;

public record InvoiceInfo(
        String invoiceId,
        String orderId,
        Address billingAddress,
        List<InvoiceItem> items
) {
}
