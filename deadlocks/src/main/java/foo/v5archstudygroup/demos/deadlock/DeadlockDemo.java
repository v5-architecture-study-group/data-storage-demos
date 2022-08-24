package foo.v5archstudygroup.demos.deadlock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DeadlockDemo {

    private static final String JDBC_URL = "jdbc:h2:mem:deadlock-demo";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    public static void main(String[] args) throws Exception {
        try (var connection1 = createConnection();
             var connection2 = createConnection()) {
            createTables(connection1);
            createAuthors(connection1);

            update_deadlock(connection1, connection2);
        }
    }

    private static Connection createConnection() throws SQLException {
        var connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        connection.setAutoCommit(false);
        return connection;
    }

    private static void createTables(Connection connection) throws SQLException {
        execute(connection, "create table authors (id bigint not null primary key, firstname varchar(200) not null, lastname varchar(200) not null)");
        execute(connection, "create table notes (id bigint not null primary key, author_id bigint not null, note varchar(300) not null, foreign key (author_id) references authors (id))");
        connection.commit();
        System.out.println("Tables created");
    }

    private static void createAuthors(Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement("insert into authors (id, firstname, lastname) values (?, ?, ?)")) {
            for (int i = 1; i <= 100; ++i) {
                stmt.setLong(1, i);
                stmt.setString(2, "First" + i);
                stmt.setString(3, "Last" + i);
                stmt.executeUpdate();
            }
            connection.commit();
            System.out.println("Authors inserted");
        }
    }

    private static void update_deadlock(Connection connection1, Connection connection2) throws Exception {
        var t1 = new Thread(() -> {
            execute(connection1, "select id from authors where id = 1 for update");
            execute(connection1, "select id from authors where id = 2 for update");
            commit(connection1);
        });
        var t2 = new Thread(() -> {
            execute(connection2, "select id from authors where id = 2 for update");
            execute(connection2, "select id from authors where id = 1 for update");
            commit(connection2);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    private static void execute(Connection connection, String sql) {
        try (var stmt = connection.createStatement()) {
            System.out.println(Thread.currentThread().getName() + ": BEFORE Executing: " + sql);
            stmt.execute(sql);
            System.out.println(Thread.currentThread().getName() + ": AFTER  Executing: " + sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void commit(Connection connection) {
        try {
            System.out.println(Thread.currentThread().getName() + ": Committing");
            connection.commit();
            System.out.println(Thread.currentThread().getName() + ": Committed");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}