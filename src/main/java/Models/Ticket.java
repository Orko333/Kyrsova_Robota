package Models;

import Models.Enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Клас, що представляє квиток.
 * Пов'язує рейс, пасажира, місце. Містить інформацію про бронювання, продаж та ціну.
 */
public class Ticket {
    private long id;
    private Flight flight;
    private Passenger passenger;
    private String seatNumber; // Номер місця, наприклад "1A", "25"
    private LocalDateTime bookingDateTime; // Час бронювання
    private LocalDateTime purchaseDateTime; // Час покупки, може бути null якщо тільки заброньовано
    private LocalDateTime bookingExpiryDateTime; // Термін дії броні, може бути null
    private BigDecimal pricePaid; // Фактично сплачена ціна (може відрізнятися через пільги)
    private TicketStatus status;

    public Ticket(long id, Flight flight, Passenger passenger, String seatNumber,
                  LocalDateTime bookingDateTime, BigDecimal pricePaid, TicketStatus status) {
        this.id = id;
        this.flight = flight;
        this.passenger = passenger;
        this.seatNumber = seatNumber;
        this.bookingDateTime = bookingDateTime;
        this.pricePaid = pricePaid;
        this.status = status;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(LocalDateTime bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public LocalDateTime getPurchaseDateTime() {
        return purchaseDateTime;
    }

    public void setPurchaseDateTime(LocalDateTime purchaseDateTime) {
        this.purchaseDateTime = purchaseDateTime;
    }

    public LocalDateTime getBookingExpiryDateTime() {
        return bookingExpiryDateTime;
    }

    public void setBookingExpiryDateTime(LocalDateTime bookingExpiryDateTime) {
        this.bookingExpiryDateTime = bookingExpiryDateTime;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public void setPricePaid(BigDecimal pricePaid) {
        this.pricePaid = pricePaid;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Квиток " + id + " на рейс " + flight.getId() + ", Пасажир: " + passenger.getFullName() +
                ", Місце: " + seatNumber + ", Статус: " + status.getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return id == ticket.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
