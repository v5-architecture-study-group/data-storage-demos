package foo.v5archstudygroup.demos.toctou.service;

import foo.v5archstudygroup.demos.toctou.entity.Account;

public interface AccountService {

    Account createAccount(String accountName);

    Money getBalance(Account account);

    void deposit(Account account, Money amount);

    void withdraw(Account account, Money amount) throws InsufficientFundsException;
}
