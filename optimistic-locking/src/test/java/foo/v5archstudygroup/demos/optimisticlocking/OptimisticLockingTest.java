package foo.v5archstudygroup.demos.optimisticlocking;

import foo.v5archstudygroup.demos.optimisticlocking.entity.Author;
import foo.v5archstudygroup.demos.optimisticlocking.entity.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class OptimisticLockingTest {

    private final AuthorRepository authorRepository;

    @Autowired
    public OptimisticLockingTest(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Test
    public void entity_gets_a_version_number_when_first_saved() {
        var author = authorRepository.saveAndFlush(new Author("John", "Smith"));
        assertThat(author.getVersion()).contains(0L);
    }

    @Test
    public void entity_version_number_is_incremented_after_each_save() {
        var author = authorRepository.save(new Author("John", "Smith"));
        author.setLastName("Cool");
        author = authorRepository.save(author);
        assertThat(author.getVersion()).contains(1L);
        author.setFirstName("Joe");
        author = authorRepository.save(author);
        assertThat(author.getVersion()).contains(2L);
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    @Test
    public void locking_exception_when_trying_to_save_entity_with_wrong_version() {
        var author = authorRepository.save(new Author("John", "Smith"));
        var author2 = authorRepository.findById(author.getId()).get();

        author.setFirstName("Joe");
        author2.setLastName("Cool");

        author = authorRepository.save(author);

        assertThat(author.getVersion()).contains(1L);
        assertThat(author2.getVersion()).contains(0L);

        assertThatThrownBy(() -> authorRepository.save(author2)).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
