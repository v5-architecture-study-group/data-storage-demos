package foo.v5archstudygroup.demos.idempotence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivedOrderRepository extends JpaRepository<ReceivedOrder, Long> {

    boolean existsByOrderId(String orderId);
}
