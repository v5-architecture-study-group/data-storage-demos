package foo.v5archstudygroup.demos.idempotence.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("select i from Invoice i where i.order.orderId = :orderId")
    List<Invoice> findByOrderId(String orderId);
}
