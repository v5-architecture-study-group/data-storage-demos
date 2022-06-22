package foo.v5archstudygroup.demos.toctou.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money implements Serializable, Comparable<Money> {
    public static final int SCALE = 4;
    public static final int PRECISION = 19;
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public Money(int amount) {
        this(new BigDecimal(amount));
    }

    public BigDecimal toBigDecimal() {
        return amount;
    }

    public boolean isNegative() {
        return amount.signum() == -1;
    }

    public boolean isPositive() {
        return amount.signum() == 1;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.equals(money.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }

    @Override
    public int compareTo(Money o) {
        return amount.compareTo(o.amount);
    }
}
