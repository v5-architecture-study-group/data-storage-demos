package foo.v5archstudygroup.demos.toctou.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Account extends AbstractPersistable<Long> {

    private static final int ACCOUNT_NAME_MAX_LENGTH = 255;

    @Column(name = "account_name", length = ACCOUNT_NAME_MAX_LENGTH, nullable = false, unique = true)
    private String accountName;

    protected Account() {
        // Used by JPA only
    }

    public Account(String accountName) {
        if (accountName.length() > ACCOUNT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("The account name is too long");
        }
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String toString() {
        return "Account{" + accountName + "}";
    }
}
