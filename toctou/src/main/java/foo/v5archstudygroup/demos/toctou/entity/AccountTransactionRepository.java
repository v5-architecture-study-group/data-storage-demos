package foo.v5archstudygroup.demos.toctou.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    @Query("select sum(tx.amount) from AccountTransaction tx where tx.account = :account")
    BigDecimal getAccountBalance(Account account);
}
