package UI.Model;

import Models.Passenger;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблиці для пасажирів.
 */
public class PassengersTableModel extends AbstractTableModel {
    private List<Passenger> passengers;
    private final String[] columnNames = {"ID", "ПІБ", "Документ", "Номер документа", "Телефон", "Email", "Пільга"};

    public PassengersTableModel(List<Passenger> passengers) {
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
        fireTableDataChanged();
    }

    public Passenger getPassengerAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < passengers.size()) {
            return passengers.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return passengers.size();
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
        Passenger p = passengers.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return p.getId();
            case 1:
                return p.getFullName();
            case 2:
                return p.getDocumentType();
            case 3:
                return p.getDocumentNumber();
            case 4:
                return p.getPhoneNumber();
            case 5:
                return p.getEmail();
            case 6:
                return p.getBenefitType().getDisplayName();
            default:
                return null;
        }
    }
}
