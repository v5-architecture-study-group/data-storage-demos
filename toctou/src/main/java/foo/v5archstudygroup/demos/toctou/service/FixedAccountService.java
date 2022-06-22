package foo.v5archstudygroup.demos.toctou.service;

import foo.v5archstudygroup.demos.toctou.entity.Account;
import foo.v5archstudygroup.demos.toctou.entity.AccountRepository;
import foo.v5archstudygroup.demos.toctou.entity.AccountTransaction;
import foo.v5archstudygroup.demos.toctou.entity.AccountTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Random;

@Service
@Primary
class FixedAccountService implements AccountService {
    private static final Logger LOG = LoggerFactory.getLogger(VulnerableAccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final Clock clock;

    FixedAccountService(AccountRepository accountRepository,
                        AccountTransactionRepository accountTransactionRepository,
                        Clock clock) {
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account createAccount(String accountName) {
        return accountRepository.saveAndFlush(new Account(accountName));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Money getBalance(Account account) {
        return new Money(accountTransactionRepository.getAccountBalance(account));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deposit(Account account, Money amount) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Cannot deposit a negative amount");
        }
        accountTransactionRepository.saveAndFlush(new AccountTransaction(clock, account, amount.toBigDecimal()));
        LOG.info("{} deposited to {}", amount, account);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void withdraw(Account account, Money amount) throws InsufficientFundsException {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Cannot withdraw a negative amount");
        }
        LOG.info("Preparing to withdraw {} from {}", amount, account);
        // We're using the account as a lock even though we don't actually update it.
        // The lock will be released when the transaction completes.
        accountRepository.lockAccountForWriting(account.getId());

        var balance = getBalance(account);
        LOG.info("Balance of {} is {}", account, balance);
        if (balance.compareTo(amount) < 0) {
            LOG.info("{} has insufficient funds", account);
            throw new InsufficientFundsException();
        }
        try {
            // Included here for demonstration purposes only to, to make it more likely for the two threads to cause
            // the intended issue.
            Thread.sleep(1000 + new Random().nextInt(1000));
        } catch (InterruptedException ignoreIt) {
        }
        accountTransactionRepository.saveAndFlush(new AccountTransaction(clock, account, amount.toBigDecimal().negate()));
        LOG.info("{} withdrawn from {}", amount, account);
    }
}
