package foo.v5archstudygroup.demos.idempotence.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Invoice extends AbstractPersistable<Long> {

    @ManyToOne(optional = false)
    private ReceivedOrder order;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String name;
    private String streetAddress;
    private String postalCode;
    private String postOffice;
    private String country;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InvoiceItem> items;

    protected Invoice() {
    }

    public Invoice(ReceivedOrder order, LocalDate invoiceDate, LocalDate dueDate, String name, String streetAddress, String postalCode, String postOffice, String country, Set<InvoiceItem> items) {
        this.order = order;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.name = name;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.postOffice = postOffice;
        this.country = country;
        this.items = new HashSet<>(items);
        items.forEach(item -> item.setInvoice(this));
    }

    public ReceivedOrder getOrderId() {
        return order;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getName() {
        return name;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getPostOffice() {
        return postOffice;
    }

    public String getCountry() {
        return country;
    }

    public Collection<InvoiceItem> getItems() {
        return Collections.unmodifiableSet(items);
    }
}
