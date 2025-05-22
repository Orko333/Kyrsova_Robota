package UI.Dialog;

import DAO.FlightDAO;
import DAO.RouteDAO;
import Models.Flight;
import Models.Enums.FlightStatus;
import Models.Route; // Переконайтесь, що Models.Route імпортовано

import javax.swing.*;
import javax.swing.border.EmptyBorder; // Для відступів
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.SQLException; // Важливий імпорт
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Діалогове вікно для створення нового або редагування існуючого рейсу.
 */
public class FlightDialog extends JDialog { // Зроблено public
    private JTextField txtDepartureDateTime, txtArrivalDateTime, txtTotalSeats, txtBusModel, txtPricePerSeat;
    private JComboBox<Route> cmbRoute;
    private JComboBox<FlightStatus> cmbStatus;
    // private JButton btnSave, btnCancel; // Ці кнопки вже є в initComponents

    private Flight currentFlight; // Рейс для редагування; null для нового рейсу
    private final FlightDAO flightDAO;
    private final RouteDAO routeDAO;
    private boolean saved = false; // Прапорець, що вказує, чи було збережено дані

    private static final DateTimeFormatter INPUT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Конструктор діалогового вікна.
     * @param owner Батьківське вікно.
     * @param title Заголовок вікна.
     * @param flightDAO DAO для роботи з рейсами.
     * @param routeDAO DAO для роботи з маршрутами (для завантаження списку).
     * @param flightToEdit Рейс для редагування, або {@code null} для створення нового.
     */
    public FlightDialog(Frame owner, String title, FlightDAO flightDAO, RouteDAO routeDAO, Flight flightToEdit) {
        super(owner, title, true);
        this.flightDAO = flightDAO;
        this.routeDAO = routeDAO;
        this.currentFlight = flightToEdit;

        initComponents();
        loadRoutesIntoComboBox(); // Завантаження маршрутів (обробляє SQLException всередині)

        if (currentFlight != null) {
            populateFields(currentFlight);
        } else {
            cmbStatus.setSelectedItem(FlightStatus.PLANNED); // Статус за замовчуванням для нового рейсу
        }

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10)); // Відступи між BorderLayout областями

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Відступи всередині панелі
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Маршрут
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Маршрут:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2;
        cmbRoute = new JComboBox<>();
        cmbRoute.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Route) {
                    setText(((Route) value).getFullRouteDescription());
                } else if (value == null && index == -1) {
                    setText("Оберіть маршрут...");
                }
                return this;
            }
        });
        formPanel.add(cmbRoute, gbc);
        gbc.gridwidth = 1; // Скинути gridwidth

        // Відправлення
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Відправлення (РРРР-ММ-ДД ГГ:ХХ):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        txtDepartureDateTime = new JTextField(16);
        formPanel.add(txtDepartureDateTime, gbc);
        gbc.gridwidth = 1;

        // Прибуття
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Прибуття (РРРР-ММ-ДД ГГ:ХХ):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2;
        txtArrivalDateTime = new JTextField(16);
        formPanel.add(txtArrivalDateTime, gbc);
        gbc.gridwidth = 1;

        // Кількість місць
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Кількість місць:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2;
        txtTotalSeats = new JTextField(5);
        formPanel.add(txtTotalSeats, gbc);
        gbc.gridwidth = 1;

        // Модель автобуса
        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Модель автобуса:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2;
        txtBusModel = new JTextField(15);
        formPanel.add(txtBusModel, gbc);
        gbc.gridwidth = 1;

        // Ціна за місце
        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Ціна за місце (грн):"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2;
        txtPricePerSeat = new JTextField(8);
        formPanel.add(txtPricePerSeat, gbc);
        gbc.gridwidth = 1;

        // Статус
        gbc.gridx = 0; gbc.gridy = 6; formPanel.add(new JLabel("Статус:"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.gridwidth = 2;
        cmbStatus = new JComboBox<>(FlightStatus.values());
        cmbStatus.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FlightStatus) {
                    setText(((FlightStatus) value).getDisplayName());
                }
                return this;
            }
        });
        formPanel.add(cmbStatus, gbc);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Зберегти"); // Локальна змінна, а не поле класу, якщо не потрібен доступ ззовні
        JButton btnCancel = new JButton("Скасувати");

        btnSave.addActionListener(this::saveFlightAction); // Посилання на метод обробки збереження
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadRoutesIntoComboBox() {
        try {
            List<Route> routes = routeDAO.getAllRoutes(); // Може кинути SQLException
            cmbRoute.removeAllItems(); // Очистити старі елементи, якщо є
            if (routes.isEmpty()) {
                cmbRoute.addItem(null); // Додати null або спеціальний об'єкт, якщо маршрутів немає
                cmbRoute.setEnabled(false); // Деактивувати, якщо немає вибору
                JOptionPane.showMessageDialog(this, "Список маршрутів порожній. Додайте маршрути перед створенням рейсів.", "Увага", JOptionPane.WARNING_MESSAGE);
            } else {
                cmbRoute.setEnabled(true);
                for (Route route : routes) {
                    cmbRoute.addItem(route);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка завантаження маршрутів: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Не вдалося завантажити список маршрутів: " + e.getMessage(),
                    "Помилка бази даних",
                    JOptionPane.ERROR_MESSAGE);
            cmbRoute.setEnabled(false); // Деактивувати у випадку помилки
        }
    }

    private void populateFields(Flight flight) {
        // Вибір маршруту в JComboBox
        for (int i = 0; i < cmbRoute.getItemCount(); i++) {
            Route item = cmbRoute.getItemAt(i);
            if (item != null && flight.getRoute() != null && item.getId() == flight.getRoute().getId()) {
                cmbRoute.setSelectedIndex(i);
                break;
            }
        }
        txtDepartureDateTime.setText(flight.getDepartureDateTime().format(INPUT_DATE_TIME_FORMATTER));
        txtArrivalDateTime.setText(flight.getArrivalDateTime().format(INPUT_DATE_TIME_FORMATTER));
        txtTotalSeats.setText(String.valueOf(flight.getTotalSeats()));
        txtBusModel.setText(flight.getBusModel() != null ? flight.getBusModel() : "");
        txtPricePerSeat.setText(flight.getPricePerSeat().toString());
        cmbStatus.setSelectedItem(flight.getStatus());
    }

    /**
     * Обробляє подію натискання кнопки "Зберегти".
     * Валідує дані та зберігає/оновлює рейс у базі даних.
     * Обробляє SQLException всередині.
     * @param event Подія натискання кнопки (не використовується безпосередньо).
     */
    private void saveFlightAction(ActionEvent event) {
        Route selectedRoute = (Route) cmbRoute.getSelectedItem();
        if (selectedRoute == null && cmbRoute.isEnabled()) { // Якщо комбобокс активний, але нічого не вибрано
            JOptionPane.showMessageDialog(this, "Будь ласка, оберіть маршрут.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!cmbRoute.isEnabled() && currentFlight == null) { // Не можна створити новий рейс без маршрутів
            JOptionPane.showMessageDialog(this, "Неможливо створити рейс: список маршрутів порожній або не завантажений.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }


        LocalDateTime departureDateTime, arrivalDateTime;
        try {
            departureDateTime = LocalDateTime.parse(txtDepartureDateTime.getText().trim(), INPUT_DATE_TIME_FORMATTER);
            arrivalDateTime = LocalDateTime.parse(txtArrivalDateTime.getText().trim(), INPUT_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Неправильний формат дати/часу. Використовуйте РРРР-ММ-ДД ГГ:ХХ.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (arrivalDateTime.isBefore(departureDateTime) || arrivalDateTime.isEqual(departureDateTime)) {
            JOptionPane.showMessageDialog(this, "Час прибуття має бути пізніше часу відправлення.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int totalSeats;
        try {
            totalSeats = Integer.parseInt(txtTotalSeats.getText().trim());
            if (totalSeats <= 0) {
                JOptionPane.showMessageDialog(this, "Кількість місць має бути позитивним числом.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Неправильний формат кількості місць.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal pricePerSeat;
        try {
            pricePerSeat = new BigDecimal(txtPricePerSeat.getText().trim().replace(",", "."));
            if (pricePerSeat.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "Ціна не може бути від'ємною.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Неправильний формат ціни.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String busModel = txtBusModel.getText().trim();
        FlightStatus status = (FlightStatus) cmbStatus.getSelectedItem();

        try { // Обгортаємо виклики DAO в try-catch
            if (currentFlight == null) { // Створення нового рейсу
                if (selectedRoute == null) { // Ще одна перевірка, хоча малоймовірно сюди дійти
                    JOptionPane.showMessageDialog(this, "Маршрут не обрано для нового рейсу.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Flight newFlight = new Flight(0, selectedRoute, departureDateTime, arrivalDateTime, totalSeats, status, busModel, pricePerSeat);
                if (flightDAO.addFlight(newFlight)) { // Може кинути SQLException
                    saved = true;
                    dispose();
                } else {
                    // Цей блок може бути досягнутий, якщо addFlight() повертає false, але не кидає SQLException
                    // (хоча наша реалізація DAO має кидати SQLException у випадку помилки)
                    JOptionPane.showMessageDialog(this, "Не вдалося додати рейс (DAO повернув false).", "Помилка збереження", JOptionPane.ERROR_MESSAGE);
                }
            } else { // Оновлення існуючого рейсу
                // Якщо selectedRoute є null при редагуванні, це означає, що список маршрутів порожній,
                // але ми редагуємо існуючий рейс, тому маршрут має залишатися тим самим, якщо не змінюється
                Route routeToSet = (selectedRoute != null) ? selectedRoute : currentFlight.getRoute();
                if (routeToSet == null) { // Це не повинно трапитися, якщо дані узгоджені
                    JOptionPane.showMessageDialog(this, "Помилка: не вдалося визначити маршрут для оновлення.", "Критична помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                currentFlight.setRoute(routeToSet);
                currentFlight.setDepartureDateTime(departureDateTime);
                currentFlight.setArrivalDateTime(arrivalDateTime);
                currentFlight.setTotalSeats(totalSeats);
                currentFlight.setBusModel(busModel);
                currentFlight.setPricePerSeat(pricePerSeat);
                currentFlight.setStatus(status);
                if (flightDAO.updateFlight(currentFlight)) { // Може кинути SQLException
                    saved = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Не вдалося оновити рейс (DAO повернув false).", "Помилка збереження", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Помилка бази даних при збереженні рейсу: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Помилка під час взаємодії з базою даних: " + ex.getMessage(),
                    "Помилка бази даних",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception exGeneral) { // Для інших непередбачених помилок
            System.err.println("Непередбачена помилка при збереженні рейсу: " + exGeneral.getMessage());
            exGeneral.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Сталася непередбачена помилка: " + exGeneral.getMessage(),
                    "Внутрішня помилка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Повертає {@code true}, якщо дані були успішно збережені в діалозі.
     * @return {@code true} якщо збережено, інакше {@code false}.
     */
    public boolean isSaved() {
        return saved;
    }
}