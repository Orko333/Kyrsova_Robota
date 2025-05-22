// Всі класи будуть в одному файлі для цього прикладу.
// У реальному проекті вони були б у окремих файлах в пакеті Models.

package Models;

import Models.Enums.FlightStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Клас, що представляє рейс.
 * Містить інформацію про маршрут, розклад, кількість місць, статус та ціну.
 * "Періодичність" реалізується на рівні сервісів, що генерують конкретні екземпляри рейсів.
 * Ця модель представляє один конкретний рейс на певну дату/час.
 */
public class Flight {
    private long id;
    private Route route;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private int totalSeats; // Загальна кількість місць в автобусі
    // Кількість доступних місць буде розраховуватися динамічно або управлятися сервісом
    private FlightStatus status;
    private String busModel; // Модель автобуса, опціонально
    private BigDecimal pricePerSeat; // Базова ціна за місце

    public Flight(long id, Route route, LocalDateTime departureDateTime, LocalDateTime arrivalDateTime,
                  int totalSeats, FlightStatus status, String busModel, BigDecimal pricePerSeat) {
        this.id = id;
        this.route = route;
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
        this.totalSeats = totalSeats;
        this.status = status;
        this.busModel = busModel;
        this.pricePerSeat = pricePerSeat;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Route getRoute() {
        return this.route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public LocalDateTime getDepartureDateTime() {
        return departureDateTime;
    }

    public void setDepartureDateTime(LocalDateTime departureDateTime) {
        this.departureDateTime = departureDateTime;
    }

    public LocalDateTime getArrivalDateTime() {
        return arrivalDateTime;
    }

    public void setArrivalDateTime(LocalDateTime arrivalDateTime) {
        this.arrivalDateTime = arrivalDateTime;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    public String getBusModel() {
        return busModel;
    }

    public void setBusModel(String busModel) {
        this.busModel = busModel;
    }

    public BigDecimal getPricePerSeat() {
        return pricePerSeat;
    }

    public void setPricePerSeat(BigDecimal pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }

    @Override
    public String toString() {
        return "Рейс " + id + ": " + route.getFullRouteDescription() +
                ", Відправлення: " + departureDateTime + ", Статус: " + status.getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flight flight = (Flight) o;
        return id == flight.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

