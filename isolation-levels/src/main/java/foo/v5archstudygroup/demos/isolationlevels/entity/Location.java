package foo.v5archstudygroup.demos.isolationlevels.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Location implements Serializable {

    @Transient
    public static final Location NULL = new Location(0.0, 0.0);

    @Column(name = "latitude", scale = 7, nullable = false)
    private double latitude;
    @Column(name = "longitude", scale = 7, nullable = false)
    private double longitude;

    public Location getNULL() {
        return NULL;
    }

    protected Location() {
        // Used by JPA only
    }

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isNull() {
        return latitude == 0.0 && longitude == 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Double.compare(location.latitude, latitude) == 0 && Double.compare(location.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }
}
