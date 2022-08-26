package foo.v5archstudygroup.demos.idempotence.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class ReceivedOrder extends AbstractPersistable<Long> {

    @Column(nullable = false, unique = true)
    private String orderId;

    protected ReceivedOrder() {
    }

    public ReceivedOrder(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
