package UI.Model;

import Models.Flight;
import Models.Passenger;
import Models.Route;
import Models.Ticket;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Модель таблиці для відображення інформації про квитки (бронювання).
 * Цей клас розширює {@link AbstractTableModel} і надає дані для {@link javax.swing.JTable},
 * відображаючи деталі кожного квитка, такі як ID, рейс, маршрут, пасажир, місце,
 * дати бронювання та продажу, ціну та статус.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class BookingsTableModel extends AbstractTableModel {
    /**
     * Список об'єктів {@link Ticket}, що відображаються в таблиці.
     */
    private List<Ticket> tickets;
    /**
     * Масив назв стовпців таблиці.
     */
    private final String[] columnNames = {"ID Квитка", "Рейс (ID)", "Маршрут", "Пасажир", "Місце", "Дата бронюв.", "Дата продажу", "Ціна", "Статус"};
    /**
     * Форматер для відображення дати та часу в таблиці у зручному форматі ("dd.MM.yy HH:mm").
     */
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    /**
     * Конструктор для створення моделі таблиці бронювань.
     * Ініціалізує модель наданим списком квитків. Якщо список {@code null},
     * створюється порожній список.
     *
     * @param tickets список об'єктів {@link Ticket} для відображення.
     */
    public BookingsTableModel(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
    }

    /**
     * Встановлює новий список квитків для моделі.
     * Оновлює внутрішній список квитків, сортує їх за датою бронювання
     * (новіші квитки відображаються першими) та сповіщає таблицю про зміну даних.
     *
     * @param tickets новий список об'єктів {@link Ticket}.
     */
    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
        // Сортування за датою бронювання (новіші вгорі)
        this.tickets.sort(Comparator.comparing(Ticket::getBookingDateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        fireTableDataChanged(); // Сповіщення таблиці про оновлення даних
    }

    /**
     * Повертає об'єкт {@link Ticket} за вказаним індексом рядка.
     *
     * @param rowIndex індекс рядка в таблиці.
     * @return об'єкт {@link Ticket} з відповідного рядка, або {@code null}, якщо індекс виходить за межі.
     */
    public Ticket getTicketAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < tickets.size()) {
            return tickets.get(rowIndex);
        }
        return null;
    }

    /**
     * Повертає кількість рядків у моделі таблиці.
     * Кількість рядків відповідає кількості квитків у списку.
     *
     * @return кількість рядків.
     */
    @Override
    public int getRowCount() {
        return tickets.size();
    }

    /**
     * Повертає кількість стовпців у моделі таблиці.
     * Кількість стовпців визначається масивом {@code columnNames}.
     *
     * @return кількість стовпців.
     */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Повертає назву стовпця за його індексом.
     *
     * @param column індекс стовпця.
     * @return назва стовпця.
     */
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    /**
     * Повертає значення для комірки таблиці за вказаними індексами рядка та стовпця.
     * Визначає, які дані з об'єкта {@link Ticket} (а також пов'язаних об'єктів
     * {@link Flight}, {@link Passenger}, {@link Route}) відображати в кожному стовпці.
     * Дати форматуються за допомогою {@link #TABLE_DATE_FORMATTER}.
     *
     * @param rowIndex індекс рядка.
     * @param columnIndex індекс стовпця.
     * @return об'єкт, що представляє значення комірки.
     *         Повертає {@code null}, якщо індекс стовпця невідомий.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Ticket ticket = tickets.get(rowIndex);
        Flight flight = ticket.getFlight(); // Може бути null, якщо дані неповні
        Passenger passenger = ticket.getPassenger(); // Може бути null

        // Перевірка на null для flight та passenger, щоб уникнути NullPointerException
        if (flight == null || passenger == null) {
            // Можна повернути спеціальне значення або порожній рядок для таких випадків
            // Або обробити це більш витончено залежно від вимог
            switch (columnIndex) {
                case 0: return ticket.getId();
                // ... обробка інших стовпців, якщо ticket є, але flight/passenger відсутні
                default: return "N/A"; // "Not Available" або інший плейсхолдер
            }
        }

        Route route = flight.getRoute(); // Може бути null, якщо дані flight неповні

        switch (columnIndex) {
            case 0: // ID Квитка
                return ticket.getId();
            case 1: // Рейс (ID)
                return flight.getId();
            case 2: // Маршрут
                return route != null ? route.getFullRouteDescription() : "Маршрут не вказано";
            case 3: // Пасажир
                return passenger.getFullName();
            case 4: // Місце
                return ticket.getSeatNumber();
            case 5: // Дата бронюв.
                return ticket.getBookingDateTime() != null ? ticket.getBookingDateTime().format(TABLE_DATE_FORMATTER) : "-";
            case 6: // Дата продажу
                return ticket.getPurchaseDateTime() != null ? ticket.getPurchaseDateTime().format(TABLE_DATE_FORMATTER) : "-";
            case 7: // Ціна
                return ticket.getPricePaid();
            case 8: // Статус
                return ticket.getStatus() != null ? ticket.getStatus().getDisplayName() : "Статус невідомий";
            default:
                return null; // Для невідомих індексів стовпців
        }
    }
}