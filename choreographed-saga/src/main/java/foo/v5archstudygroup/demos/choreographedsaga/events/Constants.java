package foo.v5archstudygroup.demos.choreographedsaga.events;

public final class Constants {

    private Constants() {
    }

    public final static String SHIPMENTS_STREAM = "shipments";
    public final static String INVOICES_STREAM = "invoices";
    public final static String ORDERS_STREAM = "orders";

    @Deprecated(forRemoval = true)
    public static final String ORDER_SHIPPED_ROUTING_KEY = "OrderShipped";
    @Deprecated(forRemoval = true)
    public static final String INVOICE_CREATED_ROUTING_KEY = "InvoiceCreated";
    @Deprecated(forRemoval = true)
    public static final String ORDER_RECEIVED_ROUTING_KEY = "OrderReceived";
}
