package Models;

import java.util.List;
import java.util.Objects;

/**
 * Клас, що представляє маршрут рейсу.
 * Маршрут складається з пункту відправлення, пункту призначення та списку проміжних зупинок.
 */
public class Route {
    private long id;
    private Stop departureStop;
    private Stop destinationStop;
    private List<Stop> intermediateStops; // Може бути порожнім

    public Route(long id, Stop departureStop, Stop destinationStop, List<Stop> intermediateStops) {
        this.id = id;
        this.departureStop = departureStop;
        this.destinationStop = destinationStop;
        this.intermediateStops = intermediateStops;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Stop getDepartureStop() {
        return departureStop;
    }

    public void setDepartureStop(Stop departureStop) {
        this.departureStop = departureStop;
    }

    public Stop getDestinationStop() {
        return destinationStop;
    }

    public void setDestinationStop(Stop destinationStop) {
        this.destinationStop = destinationStop;
    }

    public List<Stop> getIntermediateStops() {
        return intermediateStops;
    }

    public void setIntermediateStops(List<Stop> intermediateStops) {
        this.intermediateStops = intermediateStops;
    }

    /**
     * Повертає повний опис маршруту.
     *
     * @return Рядок, що описує маршрут.
     */
    public String getFullRouteDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(departureStop.getCity()).append(" -> ");
        if (intermediateStops != null && !intermediateStops.isEmpty()) {
            for (Stop stop : intermediateStops) {
                sb.append(stop.getCity()).append(" -> ");
            }
        }
        sb.append(destinationStop.getCity());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Маршрут " + id + ": " + getFullRouteDescription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return id == route.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
