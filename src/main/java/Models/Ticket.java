package Models;

import Models.Enums.TicketStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * @version 1.1 // Версія оновлена для відображення змін
 */
public class Ticket {
    private static final Logger logger = LogManager.getLogger("insurance.log"); // Використання логера "insurance.log"

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
        logger.debug("Спроба створити новий об'єкт Ticket з ID: {}", id);

        if (flight == null) {
            logger.error("Помилка створення Ticket: Рейс (flight) не може бути null для ID: {}", id);
            throw new IllegalArgumentException("Рейс не може бути null.");
        }
        if (passenger == null) {
            logger.error("Помилка створення Ticket: Пасажир (passenger) не може бути null для ID: {}", id);
            throw new IllegalArgumentException("Пасажир не може бути null.");
        }
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            logger.error("Помилка створення Ticket: Номер місця (seatNumber) не може бути порожнім для ID: {}", id);
            throw new IllegalArgumentException("Номер місця не може бути порожнім.");
        }
        if (bookingDateTime == null) {
            logger.error("Помилка створення Ticket: Дата та час бронювання (bookingDateTime) не можуть бути null для ID: {}", id);
            throw new IllegalArgumentException("Дата та час бронювання не можуть бути null.");
        }
        if (pricePaid == null || pricePaid.compareTo(BigDecimal.ZERO) < 0) {
            logger.error("Помилка створення Ticket: Ціна (pricePaid) не може бути null або від'ємною ({}) для ID: {}", pricePaid, id);
            throw new IllegalArgumentException("Ціна не може бути null або від'ємною.");
        }
        if (status == null) {
            logger.error("Помилка створення Ticket: Статус (status) не може бути null для ID: {}", id);
            throw new IllegalArgumentException("Статус квитка не може бути null.");
        }


        this.id = id;
        this.flight = flight;
        this.passenger = passenger;
        this.seatNumber = seatNumber;
        this.bookingDateTime = bookingDateTime;
        this.pricePaid = pricePaid;
        this.status = status;
        // purchaseDateTime та bookingExpiryDateTime можуть бути встановлені пізніше
        logger.info("Об'єкт Ticket успішно створено: {}", this.toString());
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
        logger.trace("Встановлення ID квитка {} на: {}", this.id, id);
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
        if (flight == null) {
            logger.warn("Спроба встановити null рейс для квитка ID: {}", this.id);
            // throw new IllegalArgumentException("Рейс не може бути null."); // Розкоментуйте, якщо це суворе правило
        }
        logger.trace("Зміна рейсу для квитка ID {}: з {} на {}", this.id,
                (this.flight != null ? "ID " + this.flight.getId() : "null"),
                (flight != null ? "ID " + flight.getId() : "null"));
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
        if (passenger == null) {
            logger.warn("Спроба встановити null пасажира для квитка ID: {}", this.id);
            // throw new IllegalArgumentException("Пасажир не може бути null."); // Розкоментуйте, якщо це суворе правило
        }
        logger.trace("Зміна пасажира для квитка ID {}: з {} на {}", this.id,
                (this.passenger != null ? "ID " + this.passenger.getId() : "null"),
                (passenger != null ? "ID " + passenger.getId() : "null"));
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
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            logger.warn("Спроба встановити порожній номер місця для квитка ID: {}", this.id);
            // throw new IllegalArgumentException("Номер місця не може бути порожнім."); // Розкоментуйте, якщо це суворе правило
        }
        logger.trace("Зміна номера місця для квитка ID {}: з '{}' на '{}'", this.id, this.seatNumber, seatNumber);
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
        if (bookingDateTime == null) {
            logger.warn("Спроба встановити null дату бронювання для квитка ID: {}", this.id);
            // throw new IllegalArgumentException("Дата бронювання не може бути null."); // Розкоментуйте, якщо це суворе правило
        }
        logger.trace("Зміна дати бронювання для квитка ID {}: з {} на {}", this.id, this.bookingDateTime, bookingDateTime);
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
        logger.trace("Зміна дати покупки для квитка ID {}: з {} на {}", this.id, this.purchaseDateTime, purchaseDateTime);
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
        logger.trace("Зміна дати закінчення броні для квитка ID {}: з {} на {}", this.id, this.bookingExpiryDateTime, bookingExpiryDateTime);
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
        if (pricePaid == null || pricePaid.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Спроба встановити некоректну ціну ({}) для квитка ID: {}", pricePaid, this.id);
            // throw new IllegalArgumentException("Ціна не може бути null або від'ємною."); // Розкоментуйте, якщо це суворе правило
        }
        logger.trace("Зміна ціни для квитка ID {}: з {} на {}", this.id, this.pricePaid, pricePaid);
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
        TicketStatus oldStatus = this.status;
        if (status == null) {
            logger.error("Спроба встановити null статус для квитка ID: {}. Поточний статус: {}", this.id, oldStatus);
            // Залежно від логіки, можна кинути виняток або залишити старий статус
            // throw new IllegalArgumentException("Статус квитка не може бути null.");
            return; // Або не змінювати статус
        }
        this.status = status;
        logger.info("Зміна статусу для квитка ID {}: з {} на {}", this.id, oldStatus, this.status);
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
        String flightInfo = (flight != null) ? "ID " + flight.getId() : "Рейс не вказано";
        String passengerInfo = (passenger != null && passenger.getFullName() != null) ? passenger.getFullName() : "Пасажир не вказаний";
        String seatInfo = (seatNumber != null && !seatNumber.isEmpty()) ? seatNumber : "Місце не вказано";
        String statusInfo = (status != null && status.getDisplayName() != null) ? status.getDisplayName() : "Статус невідомий";
        String priceInfo = (pricePaid != null) ? pricePaid.toString() : "Ціна не вказана";
        String bookingTimeInfo = (bookingDateTime != null) ? bookingDateTime.toString() : "Час бронювання не вказано";


        return String.format("Квиток ID %d: Рейс [%s], Пасажир [%s], Місце [%s], Бронювання [%s], Ціна [%s], Статус [%s]",
                id, flightInfo, passengerInfo, seatInfo, bookingTimeInfo, priceInfo, statusInfo);
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