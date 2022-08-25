package foo.v5archstudygroup.demos.optimisticlocking.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
