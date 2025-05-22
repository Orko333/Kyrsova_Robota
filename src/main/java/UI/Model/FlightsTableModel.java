package UI.Model;

import Models.Flight;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель даних для таблиці рейсів (`JTable`).
 * Відповідає за надання даних таблиці та інформації про її структуру.
 */
public class FlightsTableModel extends AbstractTableModel {
    private List<Flight> flights;
    private final String[] columnNames = {"ID", "Маршрут", "Відправлення", "Прибуття", "Місць", "Автобус", "Ціна", "Статус"};
    private static final DateTimeFormatter TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public FlightsTableModel(List<Flight> flights) {
        this.flights = flights != null ? new ArrayList<>(flights) : new ArrayList<>();
    }

    /**
     * Оновлює дані в моделі таблиці.
     *
     * @param flights Новий список рейсів.
     */
    public void setFlights(List<Flight> flights) {
        this.flights = flights != null ? new ArrayList<>(flights) : new ArrayList<>();
        fireTableDataChanged(); // Сповіщає JTable про зміну даних
    }

    /**
     * Повертає об'єкт рейсу за індексом рядка.
     *
     * @param rowIndex Індекс рядка.
     * @return Об'єкт {@link Flight} або {@code null}, якщо індекс недійсний.
     */
    public Flight getFlightAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < flights.size()) {
            return flights.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return flights.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // Це допомагає JTable правильно сортувати числові та датові стовпці
        switch (columnIndex) {
            case 0:
                return Long.class; // ID
            case 4:
                return Integer.class; // Місць
            case 6:
                return BigDecimal.class; // Ціна
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Flight flight = flights.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return flight.getId();
            case 1:
                return flight.getRoute().getFullRouteDescription();
            case 2:
                return flight.getDepartureDateTime().format(TABLE_DATE_TIME_FORMATTER);
            case 3:
                return flight.getArrivalDateTime().format(TABLE_DATE_TIME_FORMATTER);
            case 4:
                return flight.getTotalSeats();
            case 5:
                return flight.getBusModel();
            case 6:
                return flight.getPricePerSeat();
            case 7:
                return flight.getStatus().getDisplayName();
            default:
                return null;
        }
    }
}
