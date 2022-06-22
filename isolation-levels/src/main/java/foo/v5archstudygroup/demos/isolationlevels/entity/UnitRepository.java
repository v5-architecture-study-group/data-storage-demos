package foo.v5archstudygroup.demos.isolationlevels.entity;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByCallSign(String callSign);

    default Unit getByCallSign(String callSign) {
        return findByCallSign(callSign).orElseThrow(() -> new IncorrectResultSizeDataAccessException(1));
    }

    List<Unit> findByStatusOrderByCallSign(UnitStatus status);
}
