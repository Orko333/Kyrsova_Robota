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


public class BookingsTableModel extends AbstractTableModel {
    private List<Ticket> tickets;
    private final String[] columnNames = {"ID Квитка", "Рейс (ID)", "Маршрут", "Пасажир", "Місце", "Дата бронюв.", "Дата продажу", "Ціна", "Статус"};
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    public BookingsTableModel(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets != null ? new ArrayList<>(tickets) : new ArrayList<>();
        // Сортування за датою бронювання (новіші вгорі)
        this.tickets.sort(Comparator.comparing(Ticket::getBookingDateTime).reversed());
        fireTableDataChanged();
    }

    public Ticket getTicketAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < tickets.size()) {
            return tickets.get(rowIndex);
        }
        return null;
    }

    @Override public int getRowCount() { return tickets.size(); }
    @Override public int getColumnCount() { return columnNames.length; }
    @Override public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Ticket ticket = tickets.get(rowIndex);
        Flight flight = ticket.getFlight();
        Passenger passenger = ticket.getPassenger();

        switch (columnIndex) {
            case 0: return ticket.getId();
            case 1: return flight.getId();
            case 2:
                Route route = flight.getRoute();
                return route.getFullRouteDescription();
            case 3: return passenger.getFullName();
            case 4: return ticket.getSeatNumber();
            case 5: return ticket.getBookingDateTime().format(TABLE_DATE_FORMATTER);
            case 6: return ticket.getPurchaseDateTime() != null ? ticket.getPurchaseDateTime().format(TABLE_DATE_FORMATTER) : "-";
            case 7: return ticket.getPricePaid();
            case 8: return ticket.getStatus().getDisplayName();
            default: return null;
        }
    }

}

