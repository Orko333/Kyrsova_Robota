package UI.Panel;

import DAO.FlightDAO;
import DAO.RouteDAO;
import Models.Flight;
import Models.Enums.FlightStatus;
import UI.Dialog.FlightDialog;
import UI.Model.FlightsTableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
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
 * додавання нових рейсів, редагування та скасування існуючих.
 *
 * @author [Ваше ім'я або назва команди]
 * @version 1.1
 */
public class FlightsPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger("insurance.log");

    private JTable flightsTable;
    private FlightsTableModel flightsTableModel;
    private JButton btnAddFlight, btnEditFlight, btnCancelFlight, btnRefreshFlights;

    private final FlightDAO flightDAO;
    private final RouteDAO routeDAO;

    /**
     * Конструктор панелі управління рейсами.
     * Ініціалізує DAO, компоненти UI та завантажує початкові дані про рейси.
     *
     * @throws RuntimeException якщо не вдалося ініціалізувати {@link FlightDAO} або {@link RouteDAO}.
     */
    public FlightsPanel() {
        logger.info("Ініціалізація FlightsPanel.");
        try {
            this.flightDAO = new FlightDAO();
            logger.debug("FlightDAO успішно створено.");
        } catch (Exception e) {
            logger.fatal("Не вдалося створити FlightDAO в FlightsPanel.", e);
            JOptionPane.showMessageDialog(null, "Критична помилка: не вдалося ініціалізувати сервіс даних рейсів.", "Помилка ініціалізації", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Не вдалося ініціалізувати FlightDAO", e);
        }

        try {
            this.routeDAO = new RouteDAO();
            logger.debug("RouteDAO успішно створено.");
        } catch (Exception e) {
            logger.fatal("Не вдалося створити RouteDAO в FlightsPanel.", e);
            JOptionPane.showMessageDialog(null, "Критична помилка: не вдалося ініціалізувати сервіс даних маршрутів.", "Помилка ініціалізації", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Не вдалося ініціалізувати RouteDAO", e);
        }

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadFlightsData();
        logger.info("FlightsPanel успішно ініціалізовано.");
    }

    /**
     * Ініціалізує та розміщує компоненти користувацького інтерфейсу панелі.
     * Створює таблицю для відображення рейсів та кнопки для виконання операцій:
     * "Додати рейс", "Редагувати рейс", "Скасувати рейс" та "Оновити список".
     */
    private void initComponents() {
        logger.debug("Ініціалізація компонентів UI для FlightsPanel.");
        flightsTableModel = new FlightsTableModel(new ArrayList<>());
        flightsTable = new JTable(flightsTableModel);
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flightsTable.setAutoCreateRowSorter(true);
        flightsTable.setFillsViewportHeight(true);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        if (flightsTable.getColumnModel().getColumnCount() > 6) {
            logger.trace("Налаштування рендерера для стовпців ID, Місць, Ціна.");
            flightsTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
            flightsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
            flightsTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        } else {
            logger.warn("Кількість стовпців ({}) менша за 7, рендерер для деяких стовпців може бути не встановлено.", flightsTable.getColumnModel().getColumnCount());
        }

        if (flightsTable.getColumnModel().getColumnCount() > 7) {
            logger.trace("Налаштування ширини стовпців.");
            flightsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
            flightsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
            flightsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
            flightsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
            flightsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
            flightsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
            flightsTable.getColumnModel().getColumn(6).setPreferredWidth(80);
            flightsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        } else {
            logger.warn("Кількість стовпців ({}) менша за 8, ширина деяких стовпців може бути не встановлена.", flightsTable.getColumnModel().getColumnCount());
        }

        flightsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
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
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnAddFlight = new JButton("Додати рейс");
        btnEditFlight = new JButton("Редагувати рейс");
        btnCancelFlight = new JButton("Скасувати рейс");
        btnRefreshFlights = new JButton("Оновити список");

        btnAddFlight.addActionListener(this::addFlightAction);
        btnEditFlight.addActionListener(this::editFlightAction);
        btnCancelFlight.addActionListener(this::cancelFlightAction);
        btnRefreshFlights.addActionListener(e -> {
            logger.info("Натиснуто кнопку 'Оновити список' рейсів.");
            loadFlightsData();
        });

        buttonPanel.add(btnAddFlight);
        buttonPanel.add(btnEditFlight);
        buttonPanel.add(btnCancelFlight);
        buttonPanel.add(btnRefreshFlights);

        add(buttonPanel, BorderLayout.SOUTH);
        logger.debug("Компоненти UI для FlightsPanel успішно створені та додані.");
    }

    /**
     * Завантажує або оновлює список рейсів у таблиці.
     * Дані отримуються з {@link FlightDAO}. У випадку помилки,
     * виводиться повідомлення користувачу.
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

    /**
     * Відкриває діалогове вікно для редагування обраного рейсу.
     * Якщо рейс не передано ({@code null}), виводить повідомлення про помилку.
     * Після закриття діалогу, якщо дані були збережені, оновлює список рейсів.
     * @param flightToEdit Об'єкт {@link Flight} для редагування.
     */
    private void openEditFlightDialog(Flight flightToEdit) {
        if (flightToEdit == null) {
            logger.error("Спроба відкрити діалог редагування для null рейсу.");
            JOptionPane.showMessageDialog(this, "Не вдалося отримати дані обраного рейсу для редагування.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        logger.debug("Відкриття FlightDialog для редагування рейсу ID: {}", flightToEdit.getId());
        FlightDialog dialog = new FlightDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Редагувати рейс ID: " + flightToEdit.getId(),
                flightDAO,
                routeDAO,
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

    /**
     * Обробляє дію додавання нового рейсу.
     * Відкриває діалогове вікно {@link FlightDialog} для створення нового рейсу.
     * Якщо рейс успішно створено та збережено, оновлює список рейсів у таблиці.
     * @param e Об'єкт події {@link ActionEvent}.
     */
    private void addFlightAction(ActionEvent e) {
        logger.info("Натиснуто кнопку 'Додати рейс'. Відкриття FlightDialog для створення нового рейсу.");
        FlightDialog dialog = new FlightDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Новий рейс", flightDAO, routeDAO, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            logger.info("Новий рейс було створено та збережено. Оновлення списку рейсів.");
            loadFlightsData();
        } else {
            logger.debug("Створення нового рейсу було скасовано або закрито без збереження.");
        }
    }

    /**
     * Обробляє дію редагування обраного рейсу.
     * Отримує обраний рейс з таблиці та відкриває діалогове вікно {@link FlightDialog}
     * для його редагування.
     * @param e Об'єкт події {@link ActionEvent}.
     */
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

    /**
     * Обробляє дію скасування обраного рейсу.
     * Змінює статус рейсу на {@link FlightStatus#CANCELLED}.
     * Забороняє скасування рейсів, які вже скасовані, відправлені або прибули.
     * Попередньо запитує підтвердження у користувача.
     * @param e Об'єкт події {@link ActionEvent}.
     */
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
                    "Підтвердження скасування", JOptionPane.YES_NO_OPTION);

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
     * Обробляє винятки типу {@link SQLException}, логує їх та показує повідомлення користувачу.
     * @param userMessage Повідомлення для користувача, що описує контекст помилки.
     * @param e Об'єкт винятку {@link SQLException}.
     */
    private void handleSqlException(String userMessage, SQLException e) {
        logger.error("{}: {}", userMessage, e.getMessage(), e);
        JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Помилка бази даних", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Обробляє загальні винятки (не {@link SQLException}), логує їх та показує повідомлення користувачу.
     * @param userMessage Повідомлення для користувача, що описує контекст помилки.
     * @param e Об'єкт винятку {@link Exception}.
     */
    private void handleGenericException(String userMessage, Exception e) {
        logger.error("{}: {}", userMessage, e.getMessage(), e);
        JOptionPane.showMessageDialog(this, userMessage + ":\n" + e.getMessage(), "Внутрішня помилка програми", JOptionPane.ERROR_MESSAGE);
    }
}