package UI.Panel;

import DAO.FlightDAO;
import DAO.RouteDAO; // Потрібен для передачі в FlightDialog
import Models.Flight;
import Models.Enums.FlightStatus; // Для перевірки статусу при скасуванні
import UI.Dialog.FlightDialog;
import UI.Model.FlightsTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FlightsPanel extends JPanel {
    private JTable flightsTable;
    private FlightsTableModel flightsTableModel;
    private JButton btnAddFlight, btnEditFlight, btnCancelFlight, btnRefreshFlights;

    private final FlightDAO flightDAO;
    private final RouteDAO routeDAO;

    public FlightsPanel() {
        this.flightDAO = new FlightDAO();
        this.routeDAO = new RouteDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadFlightsData();
    }

    private void initComponents() {
        flightsTableModel = new FlightsTableModel(new ArrayList<>());
        flightsTable = new JTable(flightsTableModel);
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flightsTable.setAutoCreateRowSorter(true);
        flightsTable.setFillsViewportHeight(true);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        if (flightsTable.getColumnCount() > 6) { // Захист від IndexOutOfBounds
            flightsTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer); // ID
            flightsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Місць
            flightsTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Ціна
        }

        if (flightsTable.getColumnCount() > 7) { // Захист від IndexOutOfBounds
            flightsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
            flightsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
            flightsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
            flightsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
            flightsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
            flightsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
            flightsTable.getColumnModel().getColumn(6).setPreferredWidth(80);
            flightsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        }

        flightsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                // Перевіряємо, чи клік був на валідному рядку і чи це подвійний клік
                if (row != -1 && mouseEvent.getClickCount() == 2) {
                    int modelRow = table.convertRowIndexToModel(row);
                    Flight flightToEdit = flightsTableModel.getFlightAt(modelRow);
                    if (flightToEdit != null) {
                        openEditFlightDialog(flightToEdit);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(flightsTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnAddFlight = new JButton("Додати рейс");
        btnEditFlight = new JButton("Редагувати рейс");
        btnCancelFlight = new JButton("Скасувати рейс");
        btnRefreshFlights = new JButton("Оновити список");

        btnAddFlight.addActionListener(this::addFlightAction);
        btnEditFlight.addActionListener(this::editFlightAction);
        btnCancelFlight.addActionListener(this::cancelFlightAction);
        btnRefreshFlights.addActionListener(e -> loadFlightsData());

        buttonPanel.add(btnAddFlight);
        buttonPanel.add(btnEditFlight);
        buttonPanel.add(btnCancelFlight);
        buttonPanel.add(btnRefreshFlights);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void loadFlightsData() {
        try {
            List<Flight> flights = flightDAO.getAllFlights();
            flightsTableModel.setFlights(flights);
        } catch (SQLException e) {
            handleSqlException("Не вдалося завантажити список рейсів", e);
        }
    }

    private void openEditFlightDialog(Flight flightToEdit) {
        if (flightToEdit == null) {
            JOptionPane.showMessageDialog(this, "Не вдалося отримати дані обраного рейсу для редагування.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FlightDialog dialog = new FlightDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Редагувати рейс ID: " + flightToEdit.getId(),
                flightDAO,
                routeDAO,
                flightToEdit
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadFlightsData();
        }
    }

    private void addFlightAction(ActionEvent e) {
        FlightDialog dialog = new FlightDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Новий рейс", flightDAO, routeDAO, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadFlightsData();
        }
    }

    private void editFlightAction(ActionEvent e) {
        int selectedRowView = flightsTable.getSelectedRow();
        if (selectedRowView == -1) {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть рейс для редагування.", "Рейс не вибрано", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = flightsTable.convertRowIndexToModel(selectedRowView);
        Flight flightToEdit = flightsTableModel.getFlightAt(modelRow);
        openEditFlightDialog(flightToEdit);
    }

    private void cancelFlightAction(ActionEvent e) {
        int selectedRowView = flightsTable.getSelectedRow();
        if (selectedRowView == -1) {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть рейс для скасування.", "Рейс не вибрано", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = flightsTable.convertRowIndexToModel(selectedRowView);
        Flight flightToCancel = flightsTableModel.getFlightAt(modelRow);

        if (flightToCancel != null) {
            if (flightToCancel.getStatus() == FlightStatus.CANCELLED) {
                JOptionPane.showMessageDialog(this, "Цей рейс вже скасовано.", "Інформація", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (flightToCancel.getStatus() == FlightStatus.DEPARTED || flightToCancel.getStatus() == FlightStatus.ARRIVED) {
                JOptionPane.showMessageDialog(this, "Неможливо скасувати рейс, який вже відправлений або прибув.", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String routeDescription = (flightToCancel.getRoute() != null) ? flightToCancel.getRoute().getFullRouteDescription() : "N/A";
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Ви впевнені, що хочете скасувати рейс ID " + flightToCancel.getId() + " (" + routeDescription + ")?",
                    "Підтвердження скасування", JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                try {
                    if (flightDAO.updateFlightStatus(flightToCancel.getId(), FlightStatus.CANCELLED)) {
                        JOptionPane.showMessageDialog(this, "Рейс успішно скасовано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadFlightsData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Не вдалося скасувати рейс (операція DAO не вдалася).", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    handleSqlException("Помилка бази даних при скасуванні рейсу", ex);
                } catch (Exception exGeneral) {
                    handleGenericException("Непередбачена помилка при скасуванні рейсу", exGeneral);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Не вдалося отримати дані обраного рейсу для скасування.", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSqlException(String userMessage, SQLException e) {
        System.err.println(userMessage + ": " + e.getMessage());
        JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Помилка бази даних", JOptionPane.ERROR_MESSAGE);
    }

    private void handleGenericException(String userMessage, Exception e) {
        System.err.println(userMessage + ": " + e.getMessage());
        JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Внутрішня помилка програми", JOptionPane.ERROR_MESSAGE);
    }
}