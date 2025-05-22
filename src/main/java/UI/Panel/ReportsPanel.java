package UI.Panel;

import DAO.FlightDAO;
import DAO.TicketDAO;
import Models.Flight;
import Models.Enums.TicketStatus; // Для звіту по статусах

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel; // Для створення пустої моделі таблиці при помилці
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat; // Для форматування валюти
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Панель для генерації та відображення звітів.
 */
public class ReportsPanel extends JPanel { // Зроблено public
    private static final Logger logger = LogManager.getLogger("insurance.log");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));
    private static final DateTimeFormatter TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private JComboBox<String> cmbReportType;
    private JPanel parametersPanel; // Панель для динамічних параметрів
    private JButton btnGenerateReport;
    private JTextArea reportTextArea; // Для текстових звітів
    private JTable reportTable;       // Для табличних звітів
    private JScrollPane reportScrollPane; // Для прокрутки таблиці або тексту

    private final TicketDAO ticketDAO;
    private final FlightDAO flightDAO;

    // Поля для параметрів звітів (можуть бути перевизначені)
    private JTextField txtStartDate, txtEndDate, txtReportDate;


    public ReportsPanel() { // Зроблено public
        logger.info("Ініціалізація ReportsPanel.");
        try {
            this.ticketDAO = new TicketDAO();
            this.flightDAO = new FlightDAO();
            logger.debug("TicketDAO та FlightDAO успішно створені.");
        } catch (Exception e) {
            logger.fatal("Не вдалося створити DAO в ReportsPanel.", e);
            JOptionPane.showMessageDialog(this, "Критична помилка: не вдалося ініціалізувати сервіси даних.", "Помилка ініціалізації", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Не вдалося ініціалізувати DAO", e);
        }


        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initComponents();
        logger.info("ReportsPanel успішно ініціалізовано.");
    }

    private void initComponents() {
        logger.debug("Ініціалізація компонентів UI для ReportsPanel.");
        // --- Панель вибору звіту та параметрів ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));

        JPanel reportSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reportSelectionPanel.add(new JLabel("Тип звіту:"));
        cmbReportType = new JComboBox<>(new String[]{
                "Оберіть тип звіту...",
                "Продажі за маршрутами (період)",
                "Завантаженість рейсів (дата)",
                "Статистика по статусах квитків"
        });
        cmbReportType.addActionListener(this::onReportTypeChange);
        reportSelectionPanel.add(cmbReportType);

        btnGenerateReport = new JButton("Сформувати звіт");
        btnGenerateReport.setEnabled(false); // Активується при виборі типу звіту
        btnGenerateReport.addActionListener(this::generateReportAction);
        reportSelectionPanel.add(btnGenerateReport);

        topPanel.add(reportSelectionPanel, BorderLayout.NORTH);

        parametersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Динамічна панель параметрів
        topPanel.add(parametersPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // --- Панель відображення звіту ---
        reportTextArea = new JTextArea(15, 70);
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportScrollPane = new JScrollPane(reportTextArea);

        add(reportScrollPane, BorderLayout.CENTER);
        logger.debug("Компоненти UI для ReportsPanel успішно створені та додані.");
    }

    /**
     * Обробник зміни типу звіту. Оновлює панель параметрів.
     */
    private void onReportTypeChange(ActionEvent e) {
        String selectedReport = (String) cmbReportType.getSelectedItem();
        logger.info("Змінено тип звіту на: '{}'", selectedReport);
        parametersPanel.removeAll();
        boolean isReportSelected = !"Оберіть тип звіту...".equals(selectedReport);
        btnGenerateReport.setEnabled(isReportSelected);

        if ("Продажі за маршрутами (період)".equals(selectedReport)) {
            logger.debug("Налаштування параметрів для звіту 'Продажі за маршрутами'.");
            parametersPanel.add(new JLabel("З:"));
            txtStartDate = new JTextField(10);
            txtStartDate.setText(LocalDate.now().minusMonths(1).format(DATE_FORMATTER));
            parametersPanel.add(txtStartDate);
            parametersPanel.add(new JLabel("По:"));
            txtEndDate = new JTextField(10);
            txtEndDate.setText(LocalDate.now().format(DATE_FORMATTER));
            parametersPanel.add(txtEndDate);
        } else if ("Завантаженість рейсів (дата)".equals(selectedReport)) {
            logger.debug("Налаштування параметрів для звіту 'Завантаженість рейсів'.");
            parametersPanel.add(new JLabel("Дата:"));
            txtReportDate = new JTextField(10);
            txtReportDate.setText(LocalDate.now().format(DATE_FORMATTER));
            parametersPanel.add(txtReportDate);
        } else if ("Статистика по статусах квитків".equals(selectedReport)) {
            logger.debug("Для звіту 'Статистика по статусах квитків' параметри не потрібні.");
        }

        parametersPanel.revalidate();
        parametersPanel.repaint();
        logger.trace("Панель параметрів оновлено.");
    }

    /**
     * Обробник натискання кнопки "Сформувати звіт".
     */
    private void generateReportAction(ActionEvent e) {
        String selectedReport = (String) cmbReportType.getSelectedItem();
        logger.info("Натиснуто кнопку 'Сформувати звіт'. Обраний тип: '{}'", selectedReport);

        if ("Оберіть тип звіту...".equals(selectedReport)) {
            logger.warn("Спроба сформувати звіт без вибору типу.");
            JOptionPane.showMessageDialog(this, "Будь ласка, оберіть тип звіту.", "Увага", JOptionPane.WARNING_MESSAGE);
            return;
        }

        reportTextArea.setText("");
        if (reportTable != null) {
            logger.debug("Очищення попереднього табличного звіту.");
            reportScrollPane.setViewportView(reportTextArea);
            reportTable = null;
        } else {
            logger.debug("Очищення попереднього текстового звіту.");
        }


        try {
            switch (selectedReport) {
                case "Продажі за маршрутами (період)":
                    logger.debug("Генерація звіту 'Продажі за маршрутами'.");
                    generateSalesByRouteReport();
                    break;
                case "Завантаженість рейсів (дата)":
                    logger.debug("Генерація звіту 'Завантаженість рейсів'.");
                    generateFlightLoadReport();
                    break;
                case "Статистика по статусах квитків":
                    logger.debug("Генерація звіту 'Статистика по статусах квитків'.");
                    generateTicketStatusReport();
                    break;
                default:
                    logger.warn("Обрано непідтримуваний тип звіту: '{}'", selectedReport);
                    reportTextArea.setText("Тип звіту не підтримується.");
            }
        } catch (DateTimeParseException ex) {
            logger.warn("Помилка формату дати при генерації звіту '{}'. Введені дати: Start='{}', End='{}', ReportDate='{}'.",
                    selectedReport,
                    (txtStartDate != null ? txtStartDate.getText() : "N/A"),
                    (txtEndDate != null ? txtEndDate.getText() : "N/A"),
                    (txtReportDate != null ? txtReportDate.getText() : "N/A"),
                    ex);
            JOptionPane.showMessageDialog(this, "Неправильний формат дати: " + ex.getMessage() + "\nВикористовуйте формат РРРР-ММ-ДД.", "Помилка формату дати", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            handleSqlException("Помилка при генерації звіту '" + selectedReport + "'", ex);
        } catch (Exception ex) {
            handleGenericException("Непередбачена помилка при генерації звіту '" + selectedReport + "'", ex);
        }
    }

    private void generateSalesByRouteReport() throws SQLException, DateTimeParseException {
        if (txtStartDate == null || txtEndDate == null) {
            logger.error("Поля дат для звіту 'Продажі за маршрутами' не ініціалізовані.");
            JOptionPane.showMessageDialog(this, "Помилка: поля для вводу дат не знайдено.", "Внутрішня помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate startDate = LocalDate.parse(txtStartDate.getText().trim(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(txtEndDate.getText().trim(), DATE_FORMATTER);
        logger.info("Генерація звіту продажів за маршрутами за період з {} по {}.", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            logger.warn("Помилка дат: початкова дата ({}) пізніше кінцевої ({}).", startDate, endDate);
            JOptionPane.showMessageDialog(this, "Початкова дата не може бути пізніше кінцевої.", "Помилка дати", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Map<String, Object>> salesData = ticketDAO.getSalesByRouteForPeriod(startDate, endDate);
        logger.debug("Отримано {} записів для звіту продажів.", salesData.size());

        StringBuilder sb = new StringBuilder();
        sb.append("Звіт: Продажі за маршрутами\n");
        sb.append("Період: з ").append(startDate.format(DATE_FORMATTER)).append(" по ").append(endDate.format(DATE_FORMATTER)).append("\n");
        sb.append("-----------------------------------------------------------------\n");
        if (salesData.isEmpty()) {
            sb.append("За вказаний період продажів не знайдено.\n");
            logger.info("Продажів за маршрутами за вказаний період не знайдено.");
        } else {
            sb.append(String.format("%-40s | %15s | %10s\n", "Маршрут", "Сума продажів", "К-ть квитків"));
            sb.append("-----------------------------------------------------------------\n");
            BigDecimal totalSalesOverall = BigDecimal.ZERO;
            int totalTicketsOverall = 0;
            for (Map.Entry<String, Map<String, Object>> entry : salesData.entrySet()) {
                String routeName = entry.getKey();
                BigDecimal totalAmount = (BigDecimal) entry.getValue().get("totalSales");
                int ticketCount = (Integer) entry.getValue().get("ticketCount");
                sb.append(String.format("%-40.40s | %15s | %10d\n", routeName, CURRENCY_FORMATTER.format(totalAmount), ticketCount));
                totalSalesOverall = totalSalesOverall.add(totalAmount);
                totalTicketsOverall += ticketCount;
            }
            sb.append("-----------------------------------------------------------------\n");
            sb.append(String.format("%-40s | %15s | %10d\n", "Всього:", CURRENCY_FORMATTER.format(totalSalesOverall), totalTicketsOverall));
            logger.info("Звіт продажів за маршрутами сформовано. Загальна сума: {}, Загальна к-ть квитків: {}",
                    CURRENCY_FORMATTER.format(totalSalesOverall), totalTicketsOverall);
        }
        sb.append("-----------------------------------------------------------------\n");
        reportTextArea.setText(sb.toString());
    }

    private void generateFlightLoadReport() throws SQLException, DateTimeParseException {
        if (txtReportDate == null) {
            logger.error("Поле дати для звіту 'Завантаженість рейсів' не ініціалізоване.");
            JOptionPane.showMessageDialog(this, "Помилка: поле для вводу дати не знайдено.", "Внутрішня помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate reportDate = LocalDate.parse(txtReportDate.getText().trim(), DATE_FORMATTER);
        logger.info("Генерація звіту завантаженості рейсів на дату: {}", reportDate);

        List<Flight> flights = flightDAO.getFlightsByDate(reportDate);
        logger.debug("Знайдено {} рейсів на дату {}.", flights.size(), reportDate);

        if (flights.isEmpty()) {
            reportTextArea.setText("На дату " + reportDate.format(DATE_FORMATTER) + " рейсів не знайдено.");
            logger.info("На дату {} рейсів не знайдено.", reportDate);
            // Створюємо пусту таблицю з заголовками, щоб користувач бачив структуру
            String[] columnNames = {"ID Рейсу", "Маршрут", "Відправлення", "Місць всього", "Зайнято", "Завантаженість (%)"};
            reportTable = new JTable(new DefaultTableModel(new Object[][]{}, columnNames)); // Пуста модель
            reportTable.setEnabled(false);
            reportTable.setFillsViewportHeight(true);
            reportScrollPane.setViewportView(reportTable);
            return;
        }

        String[] columnNames = {"ID Рейсу", "Маршрут", "Відправлення", "Місць всього", "Зайнято", "Завантаженість (%)"};
        Object[][] data = new Object[flights.size()][columnNames.length];

        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            int occupiedSeats = 0;
            try {
                occupiedSeats = flightDAO.getOccupiedSeatsCount(flight.getId());
            } catch (SQLException sqlEx) {
                logger.error("Помилка отримання кількості зайнятих місць для рейсу ID: {}", flight.getId(), sqlEx);
                // Можна встановити якесь значення за замовчуванням або пропустити
            }
            double loadPercentage = (flight.getTotalSeats() > 0) ? ((double) occupiedSeats / flight.getTotalSeats()) * 100 : 0;

            data[i][0] = flight.getId();
            data[i][1] = (flight.getRoute() != null && flight.getRoute().getFullRouteDescription() != null) ? flight.getRoute().getFullRouteDescription() : "Маршрут не вказано";
            data[i][2] = (flight.getDepartureDateTime() != null) ? flight.getDepartureDateTime().format(TABLE_DATE_TIME_FORMATTER) : "Дата не вказана";
            data[i][3] = flight.getTotalSeats();
            data[i][4] = occupiedSeats;
            data[i][5] = String.format("%.2f %%", loadPercentage);
            logger.trace("Дані для звіту завантаженості: Рейс ID={}, Зайнято={}, Завантаженість={}%", flight.getId(), occupiedSeats, String.format("%.2f", loadPercentage));
        }

        reportTable = new JTable(data, columnNames);
        reportTable.setEnabled(false);
        reportTable.setFillsViewportHeight(true);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        if (reportTable.getColumnModel().getColumnCount() > 5) { // Перевірка кількості стовпців
            reportTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer); // ID Рейсу
            reportTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Місць всього
            reportTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Зайнято
            reportTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Завантаженість
        } else {
            logger.warn("Не вдалося налаштувати рендерер для таблиці завантаженості рейсів - недостатньо стовпців.");
        }


        reportScrollPane.setViewportView(reportTable);
        logger.info("Звіт завантаженості рейсів на дату {} сформовано.", reportDate);
    }

    private void generateTicketStatusReport() throws SQLException {
        logger.info("Генерація звіту статистики по статусах квитків.");
        Map<TicketStatus, Integer> statusCounts = ticketDAO.getTicketCountsByStatus();
        logger.debug("Отримано статистику статусів квитків: {}", statusCounts);

        StringBuilder sb = new StringBuilder();
        sb.append("Звіт: Статистика по статусах квитків\n");
        sb.append("-------------------------------------\n");
        sb.append(String.format("%-20s | %s\n", "Статус", "Кількість"));
        sb.append("-------------------------------------\n");
        int totalTickets = 0;
        for (Map.Entry<TicketStatus, Integer> entry : statusCounts.entrySet()) {
            String displayName = (entry.getKey() != null && entry.getKey().getDisplayName() != null) ? entry.getKey().getDisplayName() : "Невідомий статус";
            sb.append(String.format("%-20s | %d\n", displayName, entry.getValue()));
            totalTickets += entry.getValue();
        }
        sb.append("-------------------------------------\n");
        sb.append(String.format("%-20s | %d\n", "Всього квитків:", totalTickets));
        sb.append("-------------------------------------\n");
        reportTextArea.setText(sb.toString());
        logger.info("Звіт статистики по статусах квитків сформовано. Всього квитків: {}", totalTickets);
    }

    private void handleSqlException(String userMessage, SQLException e) {
        logger.error("{}: {}", userMessage, e.getMessage(), e);
        JOptionPane.showMessageDialog(this,
                userMessage + ":\n" + e.getMessage(),
                "Помилка бази даних",
                JOptionPane.ERROR_MESSAGE);
    }

    private void handleGenericException(String userMessage, Exception e) {
        logger.error("{}: {}", userMessage, e.getMessage(), e);
        JOptionPane.showMessageDialog(this,
                userMessage + ":\n" + e.getMessage(),
                "Внутрішня помилка програми",
                JOptionPane.ERROR_MESSAGE);
    }
}