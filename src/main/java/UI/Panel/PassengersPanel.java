package UI.Panel;

import DAO.PassengerDAO;
import DAO.TicketDAO; // Для історії поїздок
import Models.Passenger;
import Models.Ticket; // Для PassengerHistoryTableModel
import UI.Dialog.PassengerDialog;
import UI.Model.PassengerHistoryTableModel;
import UI.Model.PassengersTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Припускаємо, що PassengersTableModel, PassengerHistoryTableModel та PassengerDialog визначені або імпортовані
// і PassengerDialog.savePassenger() (або еквівалент) обробляє SQLException всередині

public class PassengersPanel extends JPanel {
    private JTable passengersTable;
    private PassengersTableModel passengersTableModel;
    private JTable historyTable;
    private PassengerHistoryTableModel historyTableModel;
    private JButton btnEditPassenger, btnRefreshPassengers;

    private final PassengerDAO passengerDAO;
    private final TicketDAO ticketDAO;

    public PassengersPanel() {
        this.passengerDAO = new PassengerDAO();
        this.ticketDAO = new TicketDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadPassengersData();
    }

    private void initComponents() {
        // Панель зі списком пасажирів
        JPanel passengerListPanel = new JPanel(new BorderLayout(5,5));
        passengerListPanel.setBorder(BorderFactory.createTitledBorder("Список пасажирів"));

        passengersTableModel = new PassengersTableModel(new ArrayList<>());
        passengersTable = new JTable(passengersTableModel);
        passengersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        passengersTable.setAutoCreateRowSorter(true);
        passengersTable.setFillsViewportHeight(true);

        passengersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && passengersTable.getSelectedRow() != -1) {
                int modelRow = passengersTable.convertRowIndexToModel(passengersTable.getSelectedRow());
                Passenger selectedPassenger = passengersTableModel.getPassengerAt(modelRow);
                if (selectedPassenger != null) {
                    loadPassengerHistory(selectedPassenger.getId());
                }
            } else if (passengersTable.getSelectedRow() == -1 && historyTableModel != null) {
                historyTableModel.setTickets(new ArrayList<>());
            }
        });

        passengersTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (row != -1 && mouseEvent.getClickCount() == 2) {
                    int modelRow = table.convertRowIndexToModel(row);
                    Passenger passengerToEdit = passengersTableModel.getPassengerAt(modelRow);
                    if (passengerToEdit != null) {
                        openEditPassengerDialog(passengerToEdit);
                    }
                }
            }
        });

        JScrollPane passengersScrollPane = new JScrollPane(passengersTable);
        passengersScrollPane.setPreferredSize(new Dimension(700, 250));
        passengerListPanel.add(passengersScrollPane, BorderLayout.CENTER);

        JPanel passengerButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnEditPassenger = new JButton("Редагувати пасажира");
        btnRefreshPassengers = new JButton("Оновити список");

        btnEditPassenger.addActionListener(this::editPassengerAction);
        btnRefreshPassengers.addActionListener(e -> loadPassengersData());

        passengerButtonsPanel.add(btnEditPassenger);
        passengerButtonsPanel.add(btnRefreshPassengers);
        passengerListPanel.add(passengerButtonsPanel, BorderLayout.SOUTH);

        // Панель історії поїздок
        JPanel historyPanel = new JPanel(new BorderLayout(5,5));
        historyPanel.setBorder(BorderFactory.createTitledBorder("Історія поїздок обраного пасажира"));
        historyTableModel = new PassengerHistoryTableModel(new ArrayList<>());
        historyTable = new JTable(historyTableModel);
        historyTable.setFillsViewportHeight(true);
        // Можна налаштувати рендерери для historyTable, якщо потрібно
        DefaultTableCellRenderer rightRendererHistory = new DefaultTableCellRenderer();
        rightRendererHistory.setHorizontalAlignment(JLabel.RIGHT);
        if (historyTable.getColumnCount() > 5) { // ID Квитка, Рейс (ID), Ціна
            historyTable.getColumnModel().getColumn(0).setCellRenderer(rightRendererHistory);
            historyTable.getColumnModel().getColumn(1).setCellRenderer(rightRendererHistory);
            historyTable.getColumnModel().getColumn(5).setCellRenderer(rightRendererHistory);
        }


        JScrollPane historyScrollPane = new JScrollPane(historyTable);
        historyScrollPane.setPreferredSize(new Dimension(700, 200));
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, passengerListPanel, historyPanel);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);
    }

    private void openEditPassengerDialog(Passenger passengerToEdit) {
        if (passengerToEdit == null) {
            JOptionPane.showMessageDialog(this, "Не вдалося отримати дані обраного пасажира для редагування.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PassengerDialog dialog = new PassengerDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                passengerToEdit,
                passengerDAO
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            int selectedRowBeforeEdit = passengersTable.getSelectedRow(); // Зберегти вибір
            loadPassengersData();
            // Спробувати відновити вибір, якщо це можливо та доцільно
            if (selectedRowBeforeEdit != -1 && selectedRowBeforeEdit < passengersTable.getRowCount()) {
                // Потрібно знайти пасажира в оновленому списку (за ID) і вибрати його,
                // або просто вибрати той самий індекс, якщо порядок не змінився сильно
                // Для простоти, можна спробувати вибрати той самий індекс, якщо він валідний
                try{
                    int modelRowToReselect = passengersTable.convertRowIndexToModel(selectedRowBeforeEdit);
                    if(modelRowToReselect < passengersTableModel.getRowCount()){
                        // Можливо, краще знайти пасажира за ID, щоб уникнути проблем з сортуванням
                        // Поки що просто перевіряємо валідність індексу
                        passengersTable.setRowSelectionInterval(selectedRowBeforeEdit, selectedRowBeforeEdit);
                    }
                } catch (Exception ignored) { /* Ігнорувати, якщо не вдалося відновити вибір */}
            }
        }
    }

    private void editPassengerAction(ActionEvent e) {
        int selectedRowView = passengersTable.getSelectedRow();
        if (selectedRowView == -1) {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть пасажира для редагування.", "Пасажира не вибрано", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = passengersTable.convertRowIndexToModel(selectedRowView);
        Passenger passengerToEdit = passengersTableModel.getPassengerAt(modelRow);
        openEditPassengerDialog(passengerToEdit);
    }

    private void loadPassengersData() {
        try {
            List<Passenger> passengers = passengerDAO.getAllPassengers();
            passengersTableModel.setPassengers(passengers);
            if (passengersTable.getSelectedRow() == -1 && historyTableModel != null) {
                historyTableModel.setTickets(new ArrayList<>());
            }
        } catch (SQLException e) {
            handleSqlException("Помилка завантаження списку пасажирів", e);
        }
    }

    private void loadPassengerHistory(long passengerId) {
        try {
            List<Ticket> tickets = ticketDAO.getTicketsByPassengerId(passengerId);
            historyTableModel.setTickets(tickets);
        } catch (SQLException e) {
            handleSqlException("Помилка завантаження історії поїздок", e);
        }
    }

    private void handleSqlException(String userMessage, SQLException e) {
        System.err.println(userMessage + ": " + e.getMessage());
        JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Помилка бази даних", JOptionPane.ERROR_MESSAGE);
    }

    // Можна додати handleGenericException, якщо потрібно
    // private void handleGenericException(String userMessage, Exception e) { ... }
}