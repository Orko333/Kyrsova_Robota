package UI.Panel;

import DAO.TicketDAO;
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;
import Models.Ticket;
import UI.Model.BookingsTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;
import java.util.List;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Панель для управління бронюваннями та продажем квитків.
 */
class BookingsManagementPanel extends JPanel {
    private JTable bookingsTable;
    private BookingsTableModel bookingsTableModel;
    private JComboBox<TicketStatus> cmbStatusFilter;
    private JButton btnSellTicket, btnCancelBookingTicket, btnRefresh;

    private TicketDAO ticketDAO;

    public BookingsManagementPanel() {
        this.ticketDAO = new TicketDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        loadBookingsData(null); // Завантажити всі квитки спочатку
    }

    private void initComponents() {
        // Панель фільтрів
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Фільтр за статусом:"));
        // Додаємо опцію "Всі" до комбобоксу
        TicketStatus[] statusesWithAll = new TicketStatus[TicketStatus.values().length + 1];
        statusesWithAll[0] = null; // Для "Всі"
        System.arraycopy(TicketStatus.values(), 0, statusesWithAll, 1, TicketStatus.values().length);

        cmbStatusFilter = new JComboBox<>(statusesWithAll);
        cmbStatusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TicketStatus) {
                    setText(((TicketStatus) value).getDisplayName());
                } else if (value == null) { // Для опції "Всі"
                    setText("Всі статуси");
                }
                return this;
            }
        });
        cmbStatusFilter.addActionListener(e -> loadBookingsData((TicketStatus) cmbStatusFilter.getSelectedItem()));
        filterPanel.add(cmbStatusFilter);

        btnRefresh = new JButton("Оновити");
        btnRefresh.addActionListener(e -> loadBookingsData((TicketStatus) cmbStatusFilter.getSelectedItem()));
        filterPanel.add(btnRefresh);

        // Таблиця бронювань/квитків
        bookingsTableModel = new BookingsTableModel(new ArrayList<>());
        bookingsTable = new JTable(bookingsTableModel);
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingsTable.setAutoCreateRowSorter(true);
        // Налаштування рендерера для ціни
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        bookingsTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer); // Ціна

        JScrollPane scrollPane = new JScrollPane(bookingsTable);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnSellTicket = new JButton("Продати квиток");
        btnCancelBookingTicket = new JButton("Скасувати бронювання/квиток");

        btnSellTicket.addActionListener(this::sellTicketAction);
        btnCancelBookingTicket.addActionListener(this::cancelTicketAction);

        // Керування активністю кнопок залежно від вибору
        bookingsTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        buttonPanel.add(btnSellTicket);
        buttonPanel.add(btnCancelBookingTicket);

        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        updateButtonStates(); // Початковий стан кнопок
    }

    private void updateButtonStates() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            btnSellTicket.setEnabled(false);
            btnCancelBookingTicket.setEnabled(false);
            return;
        }
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        Ticket selectedTicket = bookingsTableModel.getTicketAt(modelRow);

        if (selectedTicket == null) {
            btnSellTicket.setEnabled(false);
            btnCancelBookingTicket.setEnabled(false);
            return;
        }

        btnSellTicket.setEnabled(selectedTicket.getStatus() == TicketStatus.BOOKED);
        btnCancelBookingTicket.setEnabled(selectedTicket.getStatus() == TicketStatus.BOOKED || selectedTicket.getStatus() == TicketStatus.SOLD);
        // Можна додати перевірку, чи рейс ще не відбувся для скасування
    }

    private void loadBookingsData(TicketStatus filterStatus) {
        try {
            List<Ticket> tickets = ticketDAO.getAllTickets(filterStatus);
            bookingsTableModel.setTickets(tickets);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Помилка завантаження списку квитків: " + e.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        updateButtonStates();
    }

    private void sellTicketAction(ActionEvent e) {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) return; // Кнопка має бути неактивна, але про всяк випадок
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        Ticket ticketToSell = bookingsTableModel.getTicketAt(modelRow);

        if (ticketToSell != null && ticketToSell.getStatus() == TicketStatus.BOOKED) {
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Продати квиток ID " + ticketToSell.getId() + " пасажиру " + ticketToSell.getPassenger().getFullName() + "?",
                    "Підтвердження продажу", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                try {
                    if (ticketDAO.updateTicketStatus(ticketToSell.getId(), TicketStatus.SOLD, LocalDateTime.now())) {
                        JOptionPane.showMessageDialog(this, "Квиток успішно продано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadBookingsData((TicketStatus) cmbStatusFilter.getSelectedItem());
                    } else {
                        JOptionPane.showMessageDialog(this, "Не вдалося продати квиток.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Помилка БД при продажу: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }

    private void cancelTicketAction(ActionEvent e) {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) return;
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        Ticket ticketToCancel = bookingsTableModel.getTicketAt(modelRow);

        if (ticketToCancel != null && (ticketToCancel.getStatus() == TicketStatus.BOOKED || ticketToCancel.getStatus() == TicketStatus.SOLD)) {
            // Перевірка, чи рейс ще не відбувся (приклад)
            if (ticketToCancel.getFlight().getDepartureDateTime().isBefore(LocalDateTime.now()) &&
                    ticketToCancel.getFlight().getStatus() != FlightStatus.PLANNED &&
                    ticketToCancel.getFlight().getStatus() != FlightStatus.DELAYED) {
                JOptionPane.showMessageDialog(this, "Неможливо скасувати квиток на рейс, який вже відбувся або скасований.", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String actionType = ticketToCancel.getStatus() == TicketStatus.BOOKED ? "бронювання" : "квиток";
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Скасувати " + actionType + " ID " + ticketToCancel.getId() + "?",
                    "Підтвердження скасування", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                try {
                    // При скасуванні проданого квитка дата покупки не змінюється, лише статус
                    if (ticketDAO.updateTicketStatus(ticketToCancel.getId(), TicketStatus.CANCELLED, null)) {
                        JOptionPane.showMessageDialog(this, actionType.substring(0, 1).toUpperCase() + actionType.substring(1) + " успішно скасовано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadBookingsData((TicketStatus) cmbStatusFilter.getSelectedItem());
                    } else {
                        JOptionPane.showMessageDialog(this, "Не вдалося скасувати " + actionType + ".", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Помилка БД при скасуванні: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }
}
