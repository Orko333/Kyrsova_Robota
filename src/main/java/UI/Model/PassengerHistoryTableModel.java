package UI.Model;

import Models.Flight;
import Models.Ticket;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблиці для історії поїздок пасажира.
 */
public class PassengerHistoryTableModel extends AbstractTableModel {
    private List<Ticket> tickets;
    private final String[] columnNames = {"ID Квитка", "Рейс (ID)", "Маршрут", "Дата відпр.", "Місце", "Ціна", "Статус квитка"};
    private static final DateTimeFormatter HISTORY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public PassengerHistoryTableModel(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return tickets.size();
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        Ticket ticket = tickets.get(rowIndex);
        Flight flight = ticket.getFlight();
        switch (columnIndex) {
            case 0:
                return ticket.getId();
            case 1:
                return flight.getId();
            case 2:
                return flight.getRoute().getFullRouteDescription();
            case 3:
                return flight.getDepartureDateTime().format(HISTORY_DATE_FORMATTER);
            case 4:
                return ticket.getSeatNumber();
            case 5:
                return ticket.getPricePaid();
            case 6:
                return ticket.getStatus().getDisplayName();
            default:
                return null;
        }
    }
}
