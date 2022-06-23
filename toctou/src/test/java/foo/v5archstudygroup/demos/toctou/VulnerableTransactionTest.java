package foo.v5archstudygroup.demos.toctou;

import foo.v5archstudygroup.demos.toctou.service.AccountService;
import foo.v5archstudygroup.demos.toctou.service.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class VulnerableTransactionTest {

    @Autowired
    @Qualifier("vulnerable")
    AccountService accountService;

    @Test
    public void two_simultaneous_threads_manage_to_overdraft_the_account() throws Exception {
        var account = accountService.createAccount("my vulnerable bank account");
        accountService.deposit(account, new Money(100));
        assertThat(accountService.getBalance(account)).isEqualTo(new Money(100));

        var t1 = new Thread(() -> accountService.withdraw(account, new Money(50)));
        var t2 = new Thread(() -> accountService.withdraw(account, new Money(70)));

        t1.start();
        Thread.sleep(100);
        t2.start();

        t1.join();
        t2.join();

        assertThat(accountService.getBalance(account)).isEqualTo(new Money(-20));
    }
}
