package foo.v5archstudygroup.demos.optimisticlocking.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.Optional;

@Entity
@Table
public class Author extends AbstractPersistable<Long> {

    private static final int NAME_MAX_LENGTH = 200;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
    private String firstName;

    @Column(length = NAME_MAX_LENGTH, nullable = false)
    private String lastName;

    protected Author() {
        // Used by JPA only.
    }

    public Author(String firstName, String lastName) {
        setFirstName(firstName);
        setLastName(lastName);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("First name cannot be longer than " + NAME_MAX_LENGTH + " characters");
        }
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Last name cannot be longer than " + NAME_MAX_LENGTH + " characters");
        }
        this.lastName = lastName;
    }

    public Optional<Long> getVersion() {
        return Optional.ofNullable(version);
    }
}
