package Models;

import Models.Enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Клас, що представляє квиток на рейс.
 * Квиток пов'язує конкретний {@link Flight рейс} з {@link Passenger пасажиром} та номером місця.
 * Він містить інформацію про час бронювання, час покупки (якщо квиток куплений),
 * термін дії броні (якщо застосовно), фактично сплачену ціну та поточний статус квитка.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class Ticket {
    /**
     * Унікальний ідентифікатор квитка.
     */
    private long id;
    /**
     * Рейс, на який видано квиток.
     * @see Flight
     */
    private Flight flight;
    /**
     * Пасажир, для якого призначений квиток.
     * @see Passenger
     */
    private Passenger passenger;
    /**
     * Номер місця, закріпленого за цим квитком (наприклад, "1A", "25").
     */
    private String seatNumber;
    /**
     * Дата та час бронювання квитка.
     */
    private LocalDateTime bookingDateTime;
    /**
     * Дата та час покупки квитка. Може бути {@code null}, якщо квиток лише заброньовано, але ще не оплачено.
     */
    private LocalDateTime purchaseDateTime;
    /**
     * Дата та час, до якого дійсна бронь квитка. Може бути {@code null}, якщо бронь не має терміну дії
     * або квиток вже куплений.
     */
    private LocalDateTime bookingExpiryDateTime;
    /**
     * Фактично сплачена ціна за квиток. Може відрізнятися від базової ціни рейсу
     * через застосування пільг або знижок.
     */
    private BigDecimal pricePaid;
    /**
     * Поточний статус квитка (наприклад, заброньований, куплений, скасований).
     * @see TicketStatus
     */
    private TicketStatus status;

    /**
     * Конструктор для створення об'єкта Квиток.
     * Цей конструктор ініціалізує основні дані квитка.
     * Поля {@code purchaseDateTime} та {@code bookingExpiryDateTime} за замовчуванням не встановлюються
     * і можуть бути встановлені пізніше за допомогою відповідних сеттерів.
     *
     * @param id унікальний ідентифікатор квитка.
     * @param flight рейс, на який видано квиток.
     * @param passenger пасажир, для якого призначений квиток.
     * @param seatNumber номер місця.
     * @param bookingDateTime дата та час бронювання.
     * @param pricePaid фактично сплачена ціна.
     * @param status поточний статус квитка.
     */
    public Ticket(long id, Flight flight, Passenger passenger, String seatNumber,
                  LocalDateTime bookingDateTime, BigDecimal pricePaid, TicketStatus status) {
        this.id = id;
        this.flight = flight;
        this.passenger = passenger;
        this.seatNumber = seatNumber;
        this.bookingDateTime = bookingDateTime;
        this.pricePaid = pricePaid;
        this.status = status;
        // purchaseDateTime та bookingExpiryDateTime можуть бути встановлені пізніше
    }

    // Getters and Setters

    /**
     * Повертає унікальний ідентифікатор квитка.
     * @return {@code long} значення ідентифікатора.
     */
    public long getId() {
        return id;
    }

    /**
     * Встановлює унікальний ідентифікатор квитка.
     * @param id новий ідентифікатор квитка.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Повертає рейс, на який видано квиток.
     * @return {@link Flight} об'єкт рейсу.
     */
    public Flight getFlight() {
        return flight;
    }

    /**
     * Встановлює рейс для квитка.
     * @param flight новий об'єкт рейсу.
     */
    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    /**
     * Повертає пасажира, для якого призначений квиток.
     * @return {@link Passenger} об'єкт пасажира.
     */
    public Passenger getPassenger() {
        return passenger;
    }

    /**
     * Встановлює пасажира для квитка.
     * @param passenger новий об'єкт пасажира.
     */
    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    /**
     * Повертає номер місця, закріпленого за квитком.
     * @return {@code String} номер місця.
     */
    public String getSeatNumber() {
        return seatNumber;
    }

    /**
     * Встановлює номер місця для квитка.
     * @param seatNumber новий номер місця.
     */
    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    /**
     * Повертає дату та час бронювання квитка.
     * @return {@link LocalDateTime} дата та час бронювання.
     */
    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    /**
     * Встановлює дату та час бронювання квитка.
     * @param bookingDateTime нова дата та час бронювання.
     */
    public void setBookingDateTime(LocalDateTime bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    /**
     * Повертає дату та час покупки квитка.
     * @return {@link LocalDateTime} дата та час покупки, або {@code null}, якщо квиток не куплений.
     */
    public LocalDateTime getPurchaseDateTime() {
        return purchaseDateTime;
    }

    /**
     * Встановлює дату та час покупки квитка.
     * @param purchaseDateTime нова дата та час покупки (може бути {@code null}).
     */
    public void setPurchaseDateTime(LocalDateTime purchaseDateTime) {
        this.purchaseDateTime = purchaseDateTime;
    }

    /**
     * Повертає дату та час, до якого дійсна бронь.
     * @return {@link LocalDateTime} дата та час закінчення терміну дії броні, або {@code null}.
     */
    public LocalDateTime getBookingExpiryDateTime() {
        return bookingExpiryDateTime;
    }

    /**
     * Встановлює дату та час, до якого дійсна бронь.
     * @param bookingExpiryDateTime нова дата та час закінчення терміну дії броні (може бути {@code null}).
     */
    public void setBookingExpiryDateTime(LocalDateTime bookingExpiryDateTime) {
        this.bookingExpiryDateTime = bookingExpiryDateTime;
    }

    /**
     * Повертає фактично сплачену ціну за квиток.
     * @return {@link BigDecimal} сплачена ціна.
     */
    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    /**
     * Встановлює фактично сплачену ціну за квиток.
     * @param pricePaid нова сплачена ціна.
     */
    public void setPricePaid(BigDecimal pricePaid) {
        this.pricePaid = pricePaid;
    }

    /**
     * Повертає поточний статус квитка.
     * @return {@link TicketStatus} статус квитка.
     */
    public TicketStatus getStatus() {
        return status;
    }

    /**
     * Встановлює поточний статус квитка.
     * @param status новий статус квитка.
     */
    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    /**
     * Повертає рядкове представлення об'єкта {@code Ticket}.
     * Формат: "Квиток [id] на рейс [id_рейсу], Пасажир: [ПІБ_пасажира], Місце: [номер_місця], Статус: [назва_статусу]".
     * Передбачається, що клас {@link Flight} має метод {@code getId()},
     * клас {@link Passenger} має метод {@code getFullName()},
     * а перерахування {@link TicketStatus} має метод {@code getDisplayName()}.
     *
     * @return {@code String} рядкове представлення квитка.
     */
    @Override
    public String toString() {
        return "Квиток " + id + " на рейс " + (flight != null ? flight.getId() : "N/A") +
                ", Пасажир: " + (passenger != null ? passenger.getFullName() : "N/A") +
                ", Місце: " + seatNumber +
                ", Статус: " + (status != null ? status.getDisplayName() : "N/A");
    }

    /**
     * Порівнює поточний об'єкт {@code Ticket} з іншим об'єктом.
     * Два квитки вважаються рівними, якщо їхні ідентифікатори ({@code id}) однакові.
     *
     * @param o об'єкт для порівняння.
     * @return {@code true}, якщо об'єкти рівні (мають однаковий {@code id}),
     *         {@code false} в іншому випадку.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return id == ticket.id;
    }

    /**
     * Повертає хеш-код для об'єкта {@code Ticket}.
     * Хеш-код базується на ідентифікаторі ({@code id}) квитка.
     *
     * @return {@code int} хеш-код об'єкта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}