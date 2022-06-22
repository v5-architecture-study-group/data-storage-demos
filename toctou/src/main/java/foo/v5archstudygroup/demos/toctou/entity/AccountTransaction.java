package foo.v5archstudygroup.demos.toctou.entity;

import foo.v5archstudygroup.demos.toctou.service.Money;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Entity
public class AccountTransaction extends AbstractPersistable<Long> {

    @Column(name = "tx_timestamp", nullable = false)
    private Instant timestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tx_account_id", nullable = false)
    private Account account;

    @Column(name = "tx_amount", nullable = false, scale = Money.SCALE, precision = Money.PRECISION)
    private BigDecimal amount;

    protected AccountTransaction() {
        // Used by JPA only
    }

    public AccountTransaction(Clock clock, Account account, BigDecimal amount) {
        this.timestamp = clock.instant();
        this.account = requireNonNull(account);
        this.amount = requireNonNull(amount);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Account getAccount() {
        return account;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
