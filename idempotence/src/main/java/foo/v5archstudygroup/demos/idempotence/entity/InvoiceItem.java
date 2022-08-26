package foo.v5archstudygroup.demos.idempotence.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class InvoiceItem extends AbstractPersistable<Long> {

    @ManyToOne(optional = false)
    private Invoice invoice;

    private String name;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;

    protected InvoiceItem() {
    }

    public InvoiceItem(String name, BigDecimal quantity, String unit, BigDecimal unitPrice) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
    }

    void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
