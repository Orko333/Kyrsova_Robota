package UI.Panel;

import DAO.FlightDAO;
import DAO.TicketDAO;
import Models.Flight;
import Models.Enums.TicketStatus; // Для звіту по статусах

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));
    private static final DateTimeFormatter TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public ReportsPanel() { // Зроблено public
        this.ticketDAO = new TicketDAO();
        this.flightDAO = new FlightDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initComponents();
    }

    private void initComponents() {
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
        // Спершу створимо текстову область, потім можемо замінити її на таблицю
        reportTextArea = new JTextArea(15, 70);
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Моноширинний шрифт для кращого форматування
        reportScrollPane = new JScrollPane(reportTextArea); // За замовчуванням текстова область

        add(reportScrollPane, BorderLayout.CENTER);
    }

    /**
     * Обробник зміни типу звіту. Оновлює панель параметрів.
     */
    private void onReportTypeChange(ActionEvent e) {
        parametersPanel.removeAll(); // Очистити попередні параметри
        String selectedReport = (String) cmbReportType.getSelectedItem();
        btnGenerateReport.setEnabled(!"Оберіть тип звіту...".equals(selectedReport));

        if ("Продажі за маршрутами (період)".equals(selectedReport)) {
            parametersPanel.add(new JLabel("З:"));
            txtStartDate = new JTextField(10);
            txtStartDate.setText(LocalDate.now().minusMonths(1).format(DATE_FORMATTER));
            parametersPanel.add(txtStartDate);
            parametersPanel.add(new JLabel("По:"));
            txtEndDate = new JTextField(10);
            txtEndDate.setText(LocalDate.now().format(DATE_FORMATTER));
            parametersPanel.add(txtEndDate);
        } else if ("Завантаженість рейсів (дата)".equals(selectedReport)) {
            parametersPanel.add(new JLabel("Дата:"));
            txtReportDate = new JTextField(10);
            txtReportDate.setText(LocalDate.now().format(DATE_FORMATTER));
            parametersPanel.add(txtReportDate);
        } else if ("Статистика по статусах квитків".equals(selectedReport)) {
            // Для цього звіту параметри не потрібні
        }

        parametersPanel.revalidate();
        parametersPanel.repaint();
    }

    /**
     * Обробник натискання кнопки "Сформувати звіт".
     */
    private void generateReportAction(ActionEvent e) {
        String selectedReport = (String) cmbReportType.getSelectedItem();
        if ("Оберіть тип звіту...".equals(selectedReport)) {
            JOptionPane.showMessageDialog(this, "Будь ласка, оберіть тип звіту.", "Увага", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Очистити попередній результат
        reportTextArea.setText("");
        // Якщо reportScrollPane містив таблицю, її треба видалити і додати TextArea, або навпаки
        if (reportTable != null) {
            reportScrollPane.setViewportView(reportTextArea); // Повертаємо TextArea
            reportTable = null; // Скидаємо посилання на таблицю
        }


        try {
            switch (selectedReport) {
                case "Продажі за маршрутами (період)":
                    generateSalesByRouteReport();
                    break;
                case "Завантаженість рейсів (дата)":
                    generateFlightLoadReport();
                    break;
                case "Статистика по статусах квитків":
                    generateTicketStatusReport();
                    break;
                default:
                    reportTextArea.setText("Тип звіту не підтримується.");
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Неправильний формат дати: " + ex.getMessage() + "\nВикористовуйте формат РРРР-ММ-ДД.", "Помилка формату дати", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            handleSqlException("Помилка при генерації звіту", ex);
        } catch (Exception ex) {
            handleGenericException("Непередбачена помилка при генерації звіту", ex);
        }
    }

    private void generateSalesByRouteReport() throws SQLException, DateTimeParseException {
        LocalDate startDate = LocalDate.parse(txtStartDate.getText().trim(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(txtEndDate.getText().trim(), DATE_FORMATTER);
        if (startDate.isAfter(endDate)) {
            JOptionPane.showMessageDialog(this, "Початкова дата не може бути пізніше кінцевої.", "Помилка дати", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Map<String, Object>> salesData = ticketDAO.getSalesByRouteForPeriod(startDate, endDate);

        StringBuilder sb = new StringBuilder();
        sb.append("Звіт: Продажі за маршрутами\n");
        sb.append("Період: з ").append(startDate.format(DATE_FORMATTER)).append(" по ").append(endDate.format(DATE_FORMATTER)).append("\n");
        sb.append("-----------------------------------------------------------------\n");
        if (salesData.isEmpty()) {
            sb.append("За вказаний період продажів не знайдено.\n");
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
        }
        sb.append("-----------------------------------------------------------------\n");
        reportTextArea.setText(sb.toString());
    }

    private void generateFlightLoadReport() throws SQLException, DateTimeParseException {
        LocalDate reportDate = LocalDate.parse(txtReportDate.getText().trim(), DATE_FORMATTER);
        List<Flight> flights = flightDAO.getFlightsByDate(reportDate);

        if (flights.isEmpty()) {
            reportTextArea.setText("На дату " + reportDate.format(DATE_FORMATTER) + " рейсів не знайдено.");
            return;
        }

        // Створюємо модель для таблиці
        String[] columnNames = {"ID Рейсу", "Маршрут", "Відправлення", "Місць всього", "Зайнято", "Завантаженість (%)"};
        Object[][] data = new Object[flights.size()][columnNames.length];

        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            int occupiedSeats = flightDAO.getOccupiedSeatsCount(flight.getId());
            double loadPercentage = (flight.getTotalSeats() > 0) ? ((double) occupiedSeats / flight.getTotalSeats()) * 100 : 0;

            data[i][0] = flight.getId();
            data[i][1] = (flight.getRoute() != null) ? flight.getRoute().getFullRouteDescription() : "N/A";
            data[i][2] = flight.getDepartureDateTime().format(TABLE_DATE_TIME_FORMATTER);
            data[i][3] = flight.getTotalSeats();
            data[i][4] = occupiedSeats;
            data[i][5] = String.format("%.2f %%", loadPercentage);
        }

        reportTable = new JTable(data, columnNames);
        reportTable.setEnabled(false); // Таблиця тільки для читання
        reportTable.setFillsViewportHeight(true);
        // Налаштування рендерера для вирівнювання чисел
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        reportTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        reportTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        reportTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        reportTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        reportScrollPane.setViewportView(reportTable); // Замінюємо TextArea на Table
    }

    private void generateTicketStatusReport() throws SQLException {
        Map<TicketStatus, Integer> statusCounts = ticketDAO.getTicketCountsByStatus();
        StringBuilder sb = new StringBuilder();
        sb.append("Звіт: Статистика по статусах квитків\n");
        sb.append("-------------------------------------\n");
        sb.append(String.format("%-20s | %s\n", "Статус", "Кількість"));
        sb.append("-------------------------------------\n");
        int totalTickets = 0;
        for (Map.Entry<TicketStatus, Integer> entry : statusCounts.entrySet()) {
            sb.append(String.format("%-20s | %d\n", entry.getKey().getDisplayName(), entry.getValue()));
            totalTickets += entry.getValue();
        }
        sb.append("-------------------------------------\n");
        sb.append(String.format("%-20s | %d\n", "Всього квитків:", totalTickets));
        sb.append("-------------------------------------\n");
        reportTextArea.setText(sb.toString());
    }

    private void handleSqlException(String userMessage, SQLException e) {
        System.err.println(userMessage + ": " + e.getMessage());
        // e.printStackTrace();
        JOptionPane.showMessageDialog(this,
                userMessage + ":\n" + e.getMessage(),
                "Помилка бази даних",
                JOptionPane.ERROR_MESSAGE);
    }

    private void handleGenericException(String userMessage, Exception e) {
        System.err.println(userMessage + ": " + e.getMessage());
        // e.printStackTrace();
        JOptionPane.showMessageDialog(this,
                userMessage + ":\n" + e.getMessage(),
                "Внутрішня помилка програми",
                JOptionPane.ERROR_MESSAGE);
    }
}