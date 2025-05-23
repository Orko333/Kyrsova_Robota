package UI.Panel;

import DAO.FlightDAO;
import DAO.RouteDAO;
import DAO.StopDAO;
import Models.Flight;
import Models.Enums.FlightStatus;
import Models.Route; // Потрібно для нового маршруту
import UI.Dialog.FlightDialog;
import UI.Dialog.RouteCreationDialog; // Потрібно для діалогу створення маршруту
import UI.Model.FlightsTableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder; // Імпорт для EmptyBorder
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Панель для управління рейсами.
 * Надає користувацький інтерфейс для перегляду списку рейсів,
 * додавання нових рейсів, редагування, скасування існуючих,
 * а також ініціює створення нових маршрутів через відповідний діалог.
 */
public class FlightsPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger("insurance.log");

    private JTable flightsTable;
    private FlightsTableModel flightsTableModel;
    private JButton btnAddFlight, btnEditFlight, btnCancelFlight, btnRefreshFlights, btnAddNewRoute;

    private final FlightDAO flightDAO;
    private final RouteDAO routeDAO;
    private final StopDAO stopDAO;

    /**
     * Конструктор панелі управління рейсами для використання в програмі.
     * Ініціалізує DAO через new, компоненти UI та завантажує початкові дані про рейси.
     *
     * @throws RuntimeException якщо не вдалося ініціалізувати один з DAO.
     */
    public FlightsPanel() {
        // Цей конструктор викликає інший, який ініціалізує DAO.
        this(createFlightDAOInternal(), createRouteDAOInternal(), createStopDAOInternal());
        logger.info("FlightsPanel створено з DAO за замовчуванням.");
    }

    // Допоміжні методи для створення DAO з обробкою помилок
    private static FlightDAO createFlightDAOInternal() {
        try {
            return new FlightDAO();
        } catch (Exception e) {
            logger.fatal("Критична помилка при створенні FlightDAO в конструкторі за замовчуванням.", e);
            throw new RuntimeException("Не вдалося ініціалізувати FlightDAO", e);
        }
    }

    private static RouteDAO createRouteDAOInternal() {
        try {
            return new RouteDAO();
        } catch (Exception e) {
            logger.fatal("Критична помилка при створенні RouteDAO в конструкторі за замовчуванням.", e);
            throw new RuntimeException("Не вдалося ініціалізувати RouteDAO", e);
        }
    }

    private static StopDAO createStopDAOInternal() {
        try {
            return new StopDAO();
        } catch (Exception e) {
            logger.fatal("Критична помилка при створенні StopDAO в конструкторі за замовчуванням.", e);
            throw new RuntimeException("Не вдалося ініціалізувати StopDAO", e);
        }
    }


    /**
     * Конструктор панелі управління рейсами для тестування та ін'єкції залежностей.
     * Ініціалізує компоненти UI та завантажує початкові дані про рейси, використовуючи надані DAO.
     *
     * @param flightDAO DAO для роботи з рейсами.
     * @param routeDAO DAO для роботи з маршрутами.
     * @param stopDAO DAO для роботи із зупинками.
     * @throws IllegalArgumentException якщо будь-який з наданих DAO є null.
     */
    public FlightsPanel(FlightDAO flightDAO, RouteDAO routeDAO, StopDAO stopDAO) {
        logger.info("Ініціалізація FlightsPanel з наданими DAO.");
        if (flightDAO == null || routeDAO == null || stopDAO == null) {
            logger.fatal("Надані DAO не можуть бути null при створенні FlightsPanel.");
            throw new IllegalArgumentException("FlightDAO, RouteDAO та StopDAO не можуть бути null.");
        }
        this.flightDAO = flightDAO;
        this.routeDAO = routeDAO;
        this.stopDAO = stopDAO;
        logger.debug("DAO успішно присвоєні.");

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10)); // Використовуємо імпортований EmptyBorder

        initComponents();
        loadFlightsData(); // Завантажуємо дані після ініціалізації компонентів
        logger.info("FlightsPanel успішно ініціалізовано.");
    }


    private void initComponents() {
        logger.debug("Ініціалізація компонентів UI для FlightsPanel.");
        flightsTableModel = new FlightsTableModel(new ArrayList<>());
        flightsTable = new JTable(flightsTableModel);
        flightsTable.setName("flightsTable"); // ВАЖЛИВО: Ім'я для тестів
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flightsTable.setAutoCreateRowSorter(true);
        flightsTable.setFillsViewportHeight(true);

        // Налаштування рендерера та ширини стовпців
        // Краще робити це після того, як таблиця гарантовано має модель стовпців
        SwingUtilities.invokeLater(() -> {
            if (flightsTable.getColumnModel().getColumnCount() > 0) { // Перевірка, чи є стовпці
                DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
                rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
                if (flightsTable.getColumnModel().getColumnCount() > 6) {
                    logger.trace("Налаштування рендерера для стовпців ID, Місць, Ціна.");
                    try {
                        flightsTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer); // ID
                        flightsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Total Seats
                        flightsTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Price
                    } catch (ArrayIndexOutOfBoundsException e) {
                        logger.warn("Помилка при налаштуванні рендерера: індекс стовпця поза межами.", e);
                    }
                } else {
                    logger.warn("Кількість стовпців ({}) менша за 7, рендерер для деяких стовпців може бути не встановлено.", flightsTable.getColumnModel().getColumnCount());
                }

                if (flightsTable.getColumnModel().getColumnCount() > 7) {
                    logger.trace("Налаштування ширини стовпців.");
                    try {
                        flightsTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // ID
                        flightsTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Маршрут
                        flightsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Відправлення
                        flightsTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Прибуття
                        flightsTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // Місць
                        flightsTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Статус
                        flightsTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Ціна
                        flightsTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Автобус
                    } catch (ArrayIndexOutOfBoundsException e) {
                        logger.warn("Помилка при налаштуванні ширини стовпців: індекс стовпця поза межами.", e);
                    }
                } else {
                    logger.warn("Кількість стовпців ({}) менша за 8, ширина деяких стовпців може бути не встановлена.", flightsTable.getColumnModel().getColumnCount());
                }
            } else {
                logger.warn("Модель стовпців для flightsTable ще не ініціалізована або порожня, налаштування рендерера та ширини відкладено.");
            }
        });


        flightsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (row != -1 && mouseEvent.getClickCount() == 2) {
                    logger.debug("Подвійний клік на рядку {} таблиці рейсів.", row);
                    int modelRow = table.convertRowIndexToModel(row);
                    Flight flightToEdit = flightsTableModel.getFlightAt(modelRow);
                    if (flightToEdit != null) {
                        logger.info("Відкриття діалогу редагування для рейсу ID: {}", flightToEdit.getId());
                        openEditFlightDialog(flightToEdit);
                    } else {
                        logger.warn("Подвійний клік на рядку {}, але не вдалося отримати рейс для редагування (модельний індекс {}).", row, modelRow);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(flightsTable);
        scrollPane.setName("flightsScrollPane"); // Опціонально
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // buttonPanel.setName("flightsButtonPanel"); // Опціонально

        btnAddFlight = new JButton("Додати рейс");
        btnAddFlight.setName("btnAddFlight"); // ВАЖЛИВО

        btnEditFlight = new JButton("Редагувати рейс");
        btnEditFlight.setName("btnEditFlight"); // ВАЖЛИВО

        btnCancelFlight = new JButton("Скасувати рейс");
        btnCancelFlight.setName("btnCancelFlight"); // ВАЖЛИВО

        btnRefreshFlights = new JButton("Оновити список");
        btnRefreshFlights.setName("btnRefreshFlights"); // ВАЖЛИВО

        btnAddNewRoute = new JButton("Створити маршрут");
        btnAddNewRoute.setName("btnAddNewRoute"); // ВАЖЛИВО

        btnAddFlight.addActionListener(this::addFlightAction);
        btnEditFlight.addActionListener(this::editFlightAction);
        btnCancelFlight.addActionListener(this::cancelFlightAction);
        btnRefreshFlights.addActionListener(e -> {
            logger.info("Натиснуто кнопку 'Оновити список' рейсів.");
            loadFlightsData();
        });
        btnAddNewRoute.addActionListener(this::addNewRouteAction);

        buttonPanel.add(btnAddFlight);
        buttonPanel.add(btnEditFlight);
        buttonPanel.add(btnCancelFlight);
        buttonPanel.add(btnRefreshFlights);
        buttonPanel.add(btnAddNewRoute);

        add(buttonPanel, BorderLayout.SOUTH);
        logger.debug("Компоненти UI для FlightsPanel успішно створені та додані.");
    }

    /**
     * Завантажує або оновлює список рейсів у таблиці.
     * Дані отримуються з {@link FlightDAO}. У випадку помилки SQL або іншої непередбаченої помилки,
     * виводиться відповідне повідомлення користувачу.
     */
    public void loadFlightsData() {
        logger.info("Завантаження даних про рейси.");
        try {
            List<Flight> flights = flightDAO.getAllFlights();
            flightsTableModel.setFlights(flights);
            logger.info("Успішно завантажено {} рейсів.", flights.size());
        } catch (SQLException e) {
            handleSqlException("Не вдалося завантажити список рейсів", e);
        } catch (Exception e) {
            handleGenericException("Непередбачена помилка при завантаженні списку рейсів", e);
        }
    }

    private void openEditFlightDialog(Flight flightToEdit) {
        if (flightToEdit == null) {
            logger.error("Спроба відкрити діалог редагування для null рейсу.");
            JOptionPane.showMessageDialog(this, "Не вдалося отримати дані обраного рейсу для редагування.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        logger.debug("Відкриття FlightDialog для редагування рейсу ID: {}", flightToEdit.getId());

        Window topLevelAncestor = SwingUtilities.getWindowAncestor(this);
        Frame ownerFrame = (topLevelAncestor instanceof Frame) ? (Frame) topLevelAncestor : null;
        if (ownerFrame == null) {
            logger.warn("Батьківське вікно не є JFrame, FlightDialog може не мати коректного власника.");
        }

        FlightDialog dialog = new FlightDialog(
                ownerFrame,
                "Редагувати рейс ID: " + flightToEdit.getId(),
                flightDAO,
                routeDAO, // Передаємо routeDAO
                flightToEdit
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            logger.info("Рейс ID: {} було відредаговано та збережено. Оновлення списку рейсів.", flightToEdit.getId());
            loadFlightsData();
        } else {
            logger.debug("Редагування рейсу ID: {} було скасовано або закрито без збереження.", flightToEdit.getId());
        }
    }

    private void addFlightAction(ActionEvent e) {
        logger.info("Натиснуто кнопку 'Додати рейс'. Відкриття FlightDialog для створення нового рейсу.");
        Window topLevelAncestor = SwingUtilities.getWindowAncestor(this);
        Frame ownerFrame = (topLevelAncestor instanceof Frame) ? (Frame) topLevelAncestor : null;
        if (ownerFrame == null) {
            logger.warn("Батьківське вікно не є JFrame, FlightDialog може не мати коректного власника.");
        }

        FlightDialog dialog = new FlightDialog(ownerFrame,
                "Новий рейс",
                flightDAO,
                routeDAO, // Передаємо routeDAO
                null); // null для нового рейсу
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            logger.info("Новий рейс було створено та збережено. Оновлення списку рейсів.");
            loadFlightsData();
        } else {
            logger.debug("Створення нового рейсу було скасовано або закрито без збереження.");
        }
    }

    private void editFlightAction(ActionEvent e) {
        logger.debug("Натиснуто кнопку 'Редагувати рейс'.");
        int selectedRowView = flightsTable.getSelectedRow();
        if (selectedRowView == -1) {
            logger.warn("Спроба редагувати рейс, але жоден рядок не вибрано.");
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть рейс для редагування.", "Рейс не вибрано", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = flightsTable.convertRowIndexToModel(selectedRowView);
        Flight flightToEdit = flightsTableModel.getFlightAt(modelRow);
        if (flightToEdit != null) {
            logger.info("Відкриття діалогу редагування для рейсу ID: {} (обраний рядок: {}, модельний індекс: {}).",
                    flightToEdit.getId(), selectedRowView, modelRow);
            openEditFlightDialog(flightToEdit);
        } else {
            logger.error("Не вдалося отримати рейс для редагування. Обраний рядок: {}, модельний індекс: {}.", selectedRowView, modelRow);
            JOptionPane.showMessageDialog(this, "Помилка: не вдалося отримати дані вибраного рейсу.", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelFlightAction(ActionEvent e) {
        logger.debug("Натиснуто кнопку 'Скасувати рейс'.");
        int selectedRowView = flightsTable.getSelectedRow();
        if (selectedRowView == -1) {
            logger.warn("Спроба скасувати рейс, але жоден рядок не вибрано.");
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть рейс для скасування.", "Рейс не вибрано", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = flightsTable.convertRowIndexToModel(selectedRowView);
        Flight flightToCancel = flightsTableModel.getFlightAt(modelRow);

        if (flightToCancel != null) {
            logger.info("Спроба скасувати рейс ID: {}. Поточний статус: {}", flightToCancel.getId(), flightToCancel.getStatus());
            if (flightToCancel.getStatus() == FlightStatus.CANCELLED) {
                logger.info("Рейс ID: {} вже скасовано.", flightToCancel.getId());
                JOptionPane.showMessageDialog(this, "Цей рейс вже скасовано.", "Інформація", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (flightToCancel.getStatus() == FlightStatus.DEPARTED || flightToCancel.getStatus() == FlightStatus.ARRIVED) {
                logger.warn("Спроба скасувати рейс ID: {}, який вже відправлений/прибув. Статус: {}", flightToCancel.getId(), flightToCancel.getStatus());
                JOptionPane.showMessageDialog(this, "Неможливо скасувати рейс, який вже відправлений або прибув.", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String routeDescription = (flightToCancel.getRoute() != null && flightToCancel.getRoute().getFullRouteDescription() != null) ? flightToCancel.getRoute().getFullRouteDescription() : "N/A";
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Ви впевнені, що хочете скасувати рейс ID " + flightToCancel.getId() + " (" + routeDescription + ")?",
                    "Підтвердження скасування", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); // Додано тип повідомлення

            if (confirmation == JOptionPane.YES_OPTION) {
                logger.debug("Користувач підтвердив скасування рейсу ID: {}", flightToCancel.getId());
                try {
                    if (flightDAO.updateFlightStatus(flightToCancel.getId(), FlightStatus.CANCELLED)) {
                        logger.info("Рейс ID: {} успішно скасовано.", flightToCancel.getId());
                        JOptionPane.showMessageDialog(this, "Рейс успішно скасовано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadFlightsData();
                    } else {
                        logger.warn("Не вдалося скасувати рейс ID: {} (DAO повернув false).", flightToCancel.getId());
                        JOptionPane.showMessageDialog(this, "Не вдалося скасувати рейс (операція DAO не вдалася).", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    handleSqlException("Помилка бази даних при скасуванні рейсу ID: " + flightToCancel.getId(), ex);
                } catch (Exception exGeneral) {
                    handleGenericException("Непередбачена помилка при скасуванні рейсу ID: " + flightToCancel.getId(), exGeneral);
                }
            } else {
                logger.debug("Користувач скасував операцію скасування рейсу ID: {}", flightToCancel.getId());
            }
        } else {
            logger.error("Не вдалося отримати рейс для скасування. Обраний рядок: {}, модельний індекс: {}.", selectedRowView, modelRow);
            JOptionPane.showMessageDialog(this, "Не вдалося отримати дані обраного рейсу для скасування.", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Обробляє дію кнопки "Створити маршрут".
     * Відкриває діалогове вікно {@link RouteCreationDialog} для створення нового маршруту.
     * Якщо маршрут успішно створений та збережений, виводиться повідомлення.
     * @param e Об'єкт події {@link ActionEvent}.
     */
    private void addNewRouteAction(ActionEvent e) {
        logger.info("Натиснуто кнопку 'Створити маршрут'. Відкриття RouteCreationDialog.");
        Window topLevelAncestor = SwingUtilities.getWindowAncestor(this);
        Frame ownerFrame = (topLevelAncestor instanceof Frame) ? (Frame) topLevelAncestor : null;
        if (ownerFrame == null) {
            logger.warn("Батьківське вікно не є JFrame, RouteCreationDialog може не мати коректного власника.");
        }

        RouteCreationDialog routeDialog = new RouteCreationDialog(
                ownerFrame,
                stopDAO // Передаємо stopDAO
        );
        routeDialog.setVisible(true);

        if (routeDialog.isSaved()) {
            Route newRoute = routeDialog.getCreatedRoute();
            if (newRoute != null) {
                logger.debug("RouteCreationDialog повернув новий маршрут: {}", newRoute.getFullRouteDescription());
                try {
                    if (routeDAO.addRoute(newRoute)) {
                        logger.info("Новий маршрут успішно додано до БД. ID: {}", newRoute.getId());
                        JOptionPane.showMessageDialog(this, "Новий маршрут '" + newRoute.getFullRouteDescription() + "' успішно створено та додано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        logger.warn("Не вдалося додати новий маршрут до БД (DAO повернув false).");
                        JOptionPane.showMessageDialog(this, "Не вдалося зберегти новий маршрут в базі даних.", "Помилка збереження", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    logger.error("Помилка SQL при збереженні нового маршруту.", ex);
                    JOptionPane.showMessageDialog(this, "Помилка бази даних при збереженні нового маршруту: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
                } catch (Exception exGeneric) {
                    logger.error("Непередбачена помилка при збереженні нового маршруту.", exGeneric);
                    JOptionPane.showMessageDialog(this, "Непередбачена помилка: " + exGeneric.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.warn("RouteCreationDialog був збережений, але повернув null маршрут.");
            }
        } else {
            logger.debug("Створення нового маршруту було скасовано або закрито без збереження.");
        }
    }

    private void handleSqlException(String userMessage, SQLException e) {
        logger.error("{}: {}", userMessage, e.getMessage(), e);
        // Перевірка, чи компонент видимий, перед показом діалогу
        if (this.isShowing()) {
            JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Помилка бази даних", JOptionPane.ERROR_MESSAGE);
        } else {
            logger.warn("FlightsPanel не видима, JOptionPane для SQLException не буде показано: {}", userMessage);
        }
    }

    private void handleGenericException(String userMessage, Exception e) {
        logger.error("{}: {}", userMessage, e.getMessage(), e);
        if (this.isShowing()) {
            JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Внутрішня помилка програми", JOptionPane.ERROR_MESSAGE);
        } else {
            logger.warn("FlightsPanel не видима, JOptionPane для GenericException не буде показано: {}", userMessage);
        }
    }
}