package UI.Panel;

import DAO.*;
import Models.*;
import Models.Enums.FlightStatus;
import UI.Dialog.BookingDialog;
import UI.Model.FlightsTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer; // Імпорт для рендерера таблиці
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException; // Важливий імпорт для обробки винятків
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Панель для пошуку рейсів та бронювання квитків.
 */
public class TicketsPanel extends JPanel { // Зроблено public для доступу з MainFrame
    private JComboBox<Stop> cmbDepartureStop, cmbDestinationStop;
    private JTextField txtDepartureDate;
    private JButton btnSearchFlights;
    private JTable flightsResultTable;
    private FlightsTableModel flightsResultTableModel;
    private JList<String> listAvailableSeats;
    private DefaultListModel<String> availableSeatsModel;
    private JButton btnBookTicket;
    private JLabel lblSelectedFlightInfo;

    private final FlightDAO flightDAO;
    // private final RouteDAO routeDAO; // routeDAO може бути не потрібен безпосередньо тут, якщо вся логіка маршрутів інкапсульована у Flight
    private final StopDAO stopDAO;
    private final TicketDAO ticketDAO;
    private final PassengerDAO passengerDAO;

    private Flight selectedFlightForBooking;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DIALOG_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public TicketsPanel() { // Зроблено public
        this.flightDAO = new FlightDAO();
        // this.routeDAO = new RouteDAO();
        this.stopDAO = new StopDAO();
        this.ticketDAO = new TicketDAO();
        this.passengerDAO = new PassengerDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents(); // Ініціалізація компонентів
        // loadStopsIntoComboBoxes() викликається всередині initComponents
    }

    private void initComponents() {
        // --- Панель пошуку рейсів ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Пошук рейсів"));

        searchPanel.add(new JLabel("Пункт відправлення:"));
        cmbDepartureStop = new JComboBox<>();
        cmbDepartureStop.setPrototypeDisplayValue(new Stop(0, "Довга назва міста для прототипу", "Місто X"));
        searchPanel.add(cmbDepartureStop);

        searchPanel.add(new JLabel("Пункт призначення:"));
        cmbDestinationStop = new JComboBox<>();
        cmbDestinationStop.setPrototypeDisplayValue(new Stop(0, "Довга назва міста для прототипу", "Місто Y"));
        searchPanel.add(cmbDestinationStop);

        searchPanel.add(new JLabel("Дата (РРРР-ММ-ДД):"));
        txtDepartureDate = new JTextField(10);
        txtDepartureDate.setText(LocalDate.now().format(DATE_FORMATTER));
        searchPanel.add(txtDepartureDate);

        btnSearchFlights = new JButton("Знайти рейси");
        btnSearchFlights.addActionListener(this::searchFlightsAction);
        searchPanel.add(btnSearchFlights);

        add(searchPanel, BorderLayout.NORTH);

        // --- Панель результатів пошуку та деталей рейсу ---
        JPanel resultsAndDetailsPanel = new JPanel(new BorderLayout(10,10));

        flightsResultTableModel = new FlightsTableModel(new ArrayList<>());
        flightsResultTable = new JTable(flightsResultTableModel);
        flightsResultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flightsResultTable.setAutoCreateRowSorter(true);
        flightsResultTable.setFillsViewportHeight(true);
        flightsResultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && flightsResultTable.getSelectedRow() != -1) {
                int modelRow = flightsResultTable.convertRowIndexToModel(flightsResultTable.getSelectedRow());
                selectedFlightForBooking = flightsResultTableModel.getFlightAt(modelRow);
                if (selectedFlightForBooking != null) {
                    updateFlightDetailsAndSeats(selectedFlightForBooking);
                }
            } else if (flightsResultTable.getSelectedRow() == -1) {
                clearFlightDetailsAndSeats();
            }
        });
        JScrollPane flightsTableScrollPane = new JScrollPane(flightsResultTable);
        flightsTableScrollPane.setPreferredSize(new Dimension(700, 200));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        flightsResultTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        flightsResultTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        flightsResultTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        flightsResultTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        flightsResultTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        flightsResultTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        flightsResultTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        flightsResultTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        flightsResultTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        flightsResultTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        flightsResultTable.getColumnModel().getColumn(7).setPreferredWidth(100);


        JPanel flightDetailsPanel = new JPanel(new BorderLayout(5,5));
        flightDetailsPanel.setBorder(BorderFactory.createTitledBorder("Деталі рейсу та доступні місця"));

        lblSelectedFlightInfo = new JLabel("Оберіть рейс зі списку вище для перегляду деталей.");
        lblSelectedFlightInfo.setHorizontalAlignment(SwingConstants.CENTER);
        flightDetailsPanel.add(lblSelectedFlightInfo, BorderLayout.NORTH);

        availableSeatsModel = new DefaultListModel<>();
        listAvailableSeats = new JList<>(availableSeatsModel);
        listAvailableSeats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listAvailableSeats.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listAvailableSeats.setVisibleRowCount(-1);
        JScrollPane seatsScrollPane = new JScrollPane(listAvailableSeats);
        seatsScrollPane.setPreferredSize(new Dimension(300, 150));
        flightDetailsPanel.add(seatsScrollPane, BorderLayout.CENTER);

        btnBookTicket = new JButton("Забронювати обране місце");
        btnBookTicket.setEnabled(false);
        btnBookTicket.addActionListener(this::bookTicketAction);
        listAvailableSeats.addListSelectionListener(e -> {
            btnBookTicket.setEnabled(!listAvailableSeats.isSelectionEmpty() && selectedFlightForBooking != null &&
                    (selectedFlightForBooking.getStatus() == FlightStatus.PLANNED || selectedFlightForBooking.getStatus() == FlightStatus.DELAYED) );
        });
        flightDetailsPanel.add(btnBookTicket, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, flightsTableScrollPane, flightDetailsPanel);
        splitPane.setResizeWeight(0.6);
        resultsAndDetailsPanel.add(splitPane, BorderLayout.CENTER);

        add(resultsAndDetailsPanel, BorderLayout.CENTER);

        // Завантаження даних для комбобоксів
        loadStopsIntoComboBoxes();
    }

    private void loadStopsIntoComboBoxes() {
        try {
            List<Stop> stops = stopDAO.getAllStops(); // Може кинути SQLException

            Stop emptyStop = new Stop(0, "Будь-який", "");
            cmbDepartureStop.addItem(emptyStop);
            cmbDestinationStop.addItem(emptyStop);

            DefaultListCellRenderer stopRenderer = new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Stop) {
                        Stop s = (Stop) value;
                        if (s.getId() == 0) setText(s.getName());
                        else setText(s.getName() + " (" + s.getCity() + ")");
                    }
                    return this;
                }
            };
            cmbDepartureStop.setRenderer(stopRenderer);
            cmbDestinationStop.setRenderer(stopRenderer);

            for (Stop stop : stops) {
                cmbDepartureStop.addItem(stop);
                cmbDestinationStop.addItem(stop);
            }
        } catch (SQLException e) {
            System.err.println("Помилка завантаження списку зупинок: " + e.getMessage());
            // e.printStackTrace(); // Розкоментуйте для детального стеку помилок
            JOptionPane.showMessageDialog(this,
                    "Не вдалося завантажити список зупинок: " + e.getMessage(),
                    "Помилка бази даних",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchFlightsAction(ActionEvent e) {
        Stop departureFilter = (Stop) cmbDepartureStop.getSelectedItem();
        Stop destinationFilter = (Stop) cmbDestinationStop.getSelectedItem();
        LocalDate dateFilter = null;
        try {
            if (!txtDepartureDate.getText().trim().isEmpty()) {
                dateFilter = LocalDate.parse(txtDepartureDate.getText().trim(), DATE_FORMATTER);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Неправильний формат дати. Використовуйте РРРР-ММ-ДД.", "Помилка дати", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final LocalDate finalDateFilter = dateFilter;

        try { // Обробка SQLException від flightDAO.getAllFlights()
            List<Flight> allFlights = flightDAO.getAllFlights();
            List<Flight> filteredFlights = allFlights.stream()
                    .filter(flight -> flight.getStatus() == FlightStatus.PLANNED || flight.getStatus() == FlightStatus.DELAYED)
                    .filter(flight -> departureFilter == null || departureFilter.getId() == 0 || (flight.getRoute() != null && flight.getRoute().getDepartureStop().getId() == departureFilter.getId()))
                    .filter(flight -> destinationFilter == null || destinationFilter.getId() == 0 || (flight.getRoute() != null && flight.getRoute().getDestinationStop().getId() == destinationFilter.getId()))
                    .filter(flight -> finalDateFilter == null || flight.getDepartureDateTime().toLocalDate().isEqual(finalDateFilter))
                    .sorted(Comparator.comparing(Flight::getDepartureDateTime))
                    .collect(Collectors.toList());

            flightsResultTableModel.setFlights(filteredFlights);
            clearFlightDetailsAndSeats();
            if (filteredFlights.isEmpty()){
                JOptionPane.showMessageDialog(this, "Рейсів за вашим запитом не знайдено.", "Результати пошуку", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            System.err.println("Помилка отримання списку рейсів: " + ex.getMessage());
            // ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Не вдалося завантажити список рейсів: " + ex.getMessage(),
                    "Помилка бази даних",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFlightDetailsAndSeats() {
        lblSelectedFlightInfo.setText("Оберіть рейс зі списку вище для перегляду деталей.");
        availableSeatsModel.clear();
        btnBookTicket.setEnabled(false);
        selectedFlightForBooking = null;
    }

    private void updateFlightDetailsAndSeats(Flight flight) {
        if (flight == null) {
            clearFlightDetailsAndSeats();
            return;
        }

        // Перевірка, чи маршрут не null перед тим, як його використовувати
        String departureCity = flight.getRoute() != null && flight.getRoute().getDepartureStop() != null ? flight.getRoute().getDepartureStop().getCity() : "N/A";
        String destinationCity = flight.getRoute() != null && flight.getRoute().getDestinationStop() != null ? flight.getRoute().getDestinationStop().getCity() : "N/A";


        lblSelectedFlightInfo.setText(String.format("Обрано: %s -> %s, Відпр: %s, Приб: %s, Ціна: %.2f грн, Статус: %s",
                departureCity,
                destinationCity,
                flight.getDepartureDateTime().format(DIALOG_DATE_TIME_FORMATTER), // Змінено на DIALOG_DATE_TIME_FORMATTER
                flight.getArrivalDateTime().format(DIALOG_DATE_TIME_FORMATTER),   // Змінено на DIALOG_DATE_TIME_FORMATTER
                flight.getPricePerSeat(),
                flight.getStatus().getDisplayName()));

        availableSeatsModel.clear();
        btnBookTicket.setEnabled(false);

        if (flight.getStatus() != FlightStatus.PLANNED && flight.getStatus() != FlightStatus.DELAYED) {
            lblSelectedFlightInfo.setText(lblSelectedFlightInfo.getText() + " | Бронювання неможливе (рейс не запланований).");
            return;
        }

        try { // Обробка SQLException від ticketDAO.getOccupiedSeatsForFlight
            List<String> occupiedSeats = ticketDAO.getOccupiedSeatsForFlight(flight.getId());
            int totalSeats = flight.getTotalSeats();

            for (int i = 1; i <= totalSeats; i++) {
                String seatNumber = String.valueOf(i);
                if (!occupiedSeats.contains(seatNumber)) {
                    availableSeatsModel.addElement(seatNumber);
                }
            }
            if (availableSeatsModel.isEmpty()){
                lblSelectedFlightInfo.setText(lblSelectedFlightInfo.getText() + " | Вільних місць немає.");
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання зайнятих місць: " + e.getMessage());
            // e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Не вдалося завантажити інформацію про місця: " + e.getMessage(),
                    "Помилка бази даних",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bookTicketAction(ActionEvent e) {
        if (selectedFlightForBooking == null || listAvailableSeats.isSelectionEmpty()) {
            JOptionPane.showMessageDialog(this, "Будь ласка, оберіть рейс та вільне місце для бронювання.", "Помилка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedFlightForBooking.getStatus() != FlightStatus.PLANNED && selectedFlightForBooking.getStatus() != FlightStatus.DELAYED) {
            JOptionPane.showMessageDialog(this, "Неможливо забронювати квиток на цей рейс. Статус рейсу: " + selectedFlightForBooking.getStatus().getDisplayName(), "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedSeat = listAvailableSeats.getSelectedValue();
        // Переконайтеся, що конструктор BookingDialog є public
        BookingDialog bookingDialog = new BookingDialog((Frame) SwingUtilities.getWindowAncestor(this),
                selectedFlightForBooking, selectedSeat, passengerDAO, ticketDAO);
        bookingDialog.setVisible(true);

        if (bookingDialog.isBookingConfirmed()) {
            updateFlightDetailsAndSeats(selectedFlightForBooking);
        }
    }
}