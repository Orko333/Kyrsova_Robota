package UI.Model;

import Models.Flight;
import Models.Route; // Потрібно імпортувати, якщо getRoute() повертає Models.Route
import Models.Ticket;
import Models.Enums.TicketStatus; // Потрібно імпортувати, якщо getStatus() повертає Models.Enums.TicketStatus

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime; // Потрібно імпортувати, якщо getDepartureDateTime() повертає LocalDateTime
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблиці для відображення історії поїздок конкретного пасажира.
 * Цей клас розширює {@link AbstractTableModel} і надає дані для {@link javax.swing.JTable},
 * відображаючи деталі кожного квитка пасажира, такі як ID квитка, інформація про рейс,
 * маршрут, дата відправлення, місце, сплачена ціна та статус квитка.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class PassengerHistoryTableModel extends AbstractTableModel {
    /**
     * Список об'єктів {@link Ticket}, що представляють історію поїздок пасажира.
     */
    private List<Ticket> tickets;
    /**
     * Масив назв стовпців таблиці історії поїздок.
     */
    private final String[] columnNames = {"ID Квитка", "Рейс (ID)", "Маршрут", "Дата відпр.", "Місце", "Ціна", "Статус квитка"};
    /**
     * Форматер для відображення дати та часу відправлення в таблиці у форматі "dd.MM.yyyy HH:mm".
     */
    private static final DateTimeFormatter HISTORY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Конструктор для створення моделі таблиці історії поїздок.
     * Ініціалізує модель наданим списком квитків. Якщо список {@code null},
     * створюється порожній список.
     *
     * @param tickets список об'єктів {@link Ticket}, що становлять історію поїздок.
     */
    public PassengerHistoryTableModel(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
    }

    /**
     * Встановлює новий список квитків для моделі історії поїздок.
     * Оновлює внутрішній список квитків та сповіщає таблицю про зміну даних,
     * що призводить до перемальовування таблиці.
     *
     * @param tickets новий список об'єктів {@link Ticket}.
     */
    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
        fireTableDataChanged(); // Сповіщення таблиці про оновлення даних
    }

    /**
     * Повертає кількість рядків у моделі таблиці.
     * Кількість рядків відповідає кількості квитків у списку історії.
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
     * Визначає, які дані з об'єкта {@link Ticket} та пов'язаного з ним {@link Flight}
     * відображати в кожному стовпці. Дата відправлення форматується за допомогою
     * {@link #HISTORY_DATE_FORMATTER}.
     *
     * @param rowIndex індекс рядка.
     * @param columnIndex індекс стовпця.
     * @return об'єкт, що представляє значення комірки.
     *         Повертає {@code null}, якщо індекс стовпця невідомий або дані відсутні.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Ticket ticket = tickets.get(rowIndex);
        Flight flight = ticket.getFlight(); // Може бути null, якщо дані неповні

        // Перевірка на null для flight, щоб уникнути NullPointerException
        // у випадках, коли доступ до flight потрібен.
        if (flight == null && (columnIndex == 1 || columnIndex == 2 || columnIndex == 3)) {
            // Для стовпців, що залежать від flight, повертаємо плейсхолдер, якщо flight відсутній
            return "N/A";
        }

        switch (columnIndex) {
            case 0: // ID Квитка
                return ticket.getId();
            case 1: // Рейс (ID)
                return flight.getId();
            case 2: // Маршрут
                Route route = flight.getRoute();
                return route != null ? route.getFullRouteDescription() : "Маршрут не вказано";
            case 3: // Дата відпр.
                LocalDateTime departureDateTime = flight.getDepartureDateTime();
                return departureDateTime != null ? departureDateTime.format(HISTORY_DATE_FORMATTER) : "-";
            case 4: // Місце
                return ticket.getSeatNumber();
            case 5: // Ціна
                return ticket.getPricePaid();
            case 6: // Статус квитка
                TicketStatus status = ticket.getStatus();
                return status != null ? status.getDisplayName() : "Статус невідомий";
            default:
                return null; // Для невідомих індексів стовпців
        }
    }
}