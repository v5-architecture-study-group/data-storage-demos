package foo.v5archstudygroup.demos.isolationlevels.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Entity
public class Unit extends AbstractPersistable<Long> {

    private static final int CALL_SIGN_MAX_LENGTH = 100;

    @Column(name = "call_sign", nullable = false, unique = true, length = CALL_SIGN_MAX_LENGTH)
    private String callSign;

    @Embedded
    private Location location = Location.NULL;

    @Column(name = "unit_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UnitStatus status = UnitStatus.UNKNOWN;

    @Column(name = "location_last_updated")
    private Instant locationLastUpdated;

    @Column(name = "unit_status_last_updated")
    private Instant statusLastUpdated;

    protected Unit() {
        // Used by JPA only.
    }

    public Unit(String callSign) {
        if (callSign.length() > CALL_SIGN_MAX_LENGTH) {
            throw new IllegalArgumentException("callSign is too long");
        }
        this.callSign = callSign;
    }

    public void updateStatus(Clock clock, UnitStatus status) {
        this.status = requireNonNull(status);
        statusLastUpdated = clock.instant();
    }

    public void updateLocation(Clock clock, Location location) {
        this.location = requireNonNull(location);
        locationLastUpdated = clock.instant();
    }

    public String getCallSign() {
        return callSign;
    }

    public Location getLocation() {
        return location;
    }

    public UnitStatus getStatus() {
        return status;
    }

    public Optional<Instant> getLocationLastUpdated() {
        return Optional.ofNullable(locationLastUpdated);
    }

    public Optional<Instant> getStatusLastUpdated() {
        return Optional.ofNullable(statusLastUpdated);
    }
}
