package Models;

import java.util.Objects;

/**
 * Клас, що представляє зупинку в маршруті.
 * Кожна зупинка має унікальний ідентифікатор, назву та місто.
 */
public class Stop {
    private long id;
    private String name; // Наприклад, "Автовокзал Центральний"
    private String city; // Наприклад, "Київ"

    public Stop(long id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return name + " (" + city + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return id == stop.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
