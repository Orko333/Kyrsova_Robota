package Panel; // Або ваш дійсний пакет

import DAO.FlightDAO;
import DAO.TicketDAO;
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;
import Models.Flight;
import Models.Route;
import Models.Stop;
// Переконайтеся, що ці імпорти є
import UI.Panel.ReportsPanel;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.fixture.JScrollPaneFixture;
import org.assertj.swing.fixture.JPanelFixture;


import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static UI.Panel.ReportsPanel.TABLE_DATE_TIME_FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportsPanelTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;

    private TicketDAO mockTicketDAO;
    private FlightDAO mockFlightDAO;

    private static final DateTimeFormatter DATE_FORMATTER_UI = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMATTER_TEST = NumberFormat.getCurrencyInstance(new Locale("uk", "UA"));

    private static final String REPORT_TYPE_SALES = "Продажі за маршрутами (період)";
    private static final String REPORT_TYPE_LOAD = "Завантаженість рейсів (дата)";
    private static final String REPORT_TYPE_STATUS = "Статистика по статусах квитків";
    private static final String REPORT_TYPE_DEFAULT = "Оберіть тип звіту...";

    // Індекси для таблиці звіту Завантаженість рейсів
    private static final int LOAD_COL_ID = 0;
    private static final int LOAD_COL_ROUTE = 1;
    private static final int LOAD_COL_DEPARTURE = 2;
    private static final int LOAD_COL_TOTAL_SEATS = 3;
    private static final int LOAD_COL_OCCUPIED = 4;
    private static final int LOAD_COL_PERCENTAGE = 5;
    private static final DateTimeFormatter LOAD_TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    @Override
    protected void onSetUp() {
        if (mockTicketDAO != null) Mockito.reset(mockTicketDAO);
        if (mockFlightDAO != null) Mockito.reset(mockFlightDAO);

        mockTicketDAO = mock(TicketDAO.class);
        mockFlightDAO = mock(FlightDAO.class);

        ReportsPanel panel = GuiActionRunner.execute(() -> new ReportsPanel(mockTicketDAO, mockFlightDAO));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame testFrame = new JFrame("Reports Test");
            testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            testFrame.setContentPane(panel);
            testFrame.pack();
            return testFrame;
        });
        window = new FrameFixture(robot(), frame);
        window.show();
    }


    @Test
    public void testSelectSalesReportType_ShowsDateParameters() {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_SALES);
        window.button("btnGenerateReport").requireEnabled();

        // Переконуємося, що текстові поля створені (їх імена встановлюються в onReportTypeChange)
        window.textBox("txtStartDate").requireVisible().requireEnabled();
        window.textBox("txtEndDate").requireVisible().requireEnabled();
    }

    @Test
    public void testSelectLoadReportType_ShowsDateParameter() {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_LOAD);
        window.button("btnGenerateReport").requireEnabled();
        // Припускаємо, що txtReportDate створюється і має ім'я
        window.textBox("txtReportDate").requireVisible().requireEnabled();
    }

    @Test
    public void testGenerateSalesReport_Successful() throws SQLException {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_SALES);

        String startDateStr = LocalDate.now().minusDays(1).format(DATE_FORMATTER_UI);
        String endDateStr = LocalDate.now().format(DATE_FORMATTER_UI);
        window.textBox("txtStartDate").setText(startDateStr);
        window.textBox("txtEndDate").setText(endDateStr);

        Map<String, Map<String, Object>> salesData = new HashMap<>();
        Map<String, Object> route1Data = new HashMap<>();
        route1Data.put("totalSales", BigDecimal.valueOf(1500.50));
        route1Data.put("ticketCount", 10);
        salesData.put("Київ - Львів", route1Data);

        when(mockTicketDAO.getSalesByRouteForPeriod(LocalDate.parse(startDateStr), LocalDate.parse(endDateStr)))
                .thenReturn(salesData);

        window.button("btnGenerateReport").click();
        Pause.pause(200);

        String expectedCurrency = CURRENCY_FORMATTER_TEST.format(1500.50);
        String reportText = window.textBox("reportTextArea").text();
        assertThat(reportText).contains("Звіт: Продажі за маршрутами");
        assertThat(reportText).contains("Київ - Львів");
        assertThat(reportText).contains(expectedCurrency);
        assertThat(reportText).containsPattern("\\s10"); // Пробіл перед числом

        JScrollPane scrollPane = window.scrollPane("reportScrollPane").targetCastedTo(JScrollPane.class);
        assertThat(scrollPane.getViewport().getView()).isInstanceOf(JTextArea.class);
    }

    @Test
    public void testGenerateSalesReport_StartDateAfterEndDate_ShowsError() {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_SALES);
        window.textBox("txtStartDate").setText("2023-02-01");
        window.textBox("txtEndDate").setText("2023-01-01");

        window.button("btnGenerateReport").click();

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireErrorMessage().requireMessage("Початкова дата не може бути пізніше кінцевої.");
        optionPane.okButton().click();
    }

    @Test
    public void testGenerateSalesReport_InvalidDateFormat_ShowsError() {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_SALES);
        window.textBox("txtStartDate").setText("invalid-date");
        // Переконуємося, що txtEndDate існує, навіть якщо його значення не ключове для цієї помилки
        window.textBox("txtEndDate").setText(LocalDate.now().format(DATE_FORMATTER_UI));

        window.button("btnGenerateReport").click();

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireErrorMessage();
        String message = GuiActionRunner.execute(() -> optionPane.target().getMessage().toString());
        assertThat(message).contains("Неправильний формат дати");
        assertThat(message).contains("РРРР-ММ-ДД");
        optionPane.okButton().click();
    }

    @Test
    public void testGenerateTicketStatusReport_Successful() throws SQLException {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_STATUS);

        Map<TicketStatus, Integer> statusData = new HashMap<>();
        statusData.put(TicketStatus.SOLD, 50);
        statusData.put(TicketStatus.BOOKED, 20);

        when(mockTicketDAO.getTicketCountsByStatus()).thenReturn(statusData);

        window.button("btnGenerateReport").click();
        Pause.pause(200);

        String reportText = window.textBox("reportTextArea").text();
        assertThat(reportText).contains("Звіт: Статистика по статусах квитків");
        assertThat(reportText).containsPattern(Pattern.quote(TicketStatus.SOLD.getDisplayName()) + "\\s*\\|\\s*50");
        assertThat(reportText).containsPattern(Pattern.quote(TicketStatus.BOOKED.getDisplayName()) + "\\s*\\|\\s*20");
        assertThat(reportText).containsPattern("Всього квитків:" + "\\s*\\|\\s*70");

        JScrollPane scrollPane = window.scrollPane("reportScrollPane").targetCastedTo(JScrollPane.class);
        assertThat(scrollPane.getViewport().getView()).isInstanceOf(JTextArea.class);
    }

    @Test
    public void testGenerateReport_NoTypeSelected_ShowsWarning() {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_DEFAULT);
        GuiActionRunner.execute(() -> window.button("btnGenerateReport").target().setEnabled(true));
        window.button("btnGenerateReport").click();

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireWarningMessage().requireMessage("Будь ласка, оберіть тип звіту.");
        optionPane.okButton().click();
    }

    @Test
    public void testHandleSqlException_OnReportGeneration() throws SQLException {
        window.comboBox("cmbReportType").selectItem(REPORT_TYPE_STATUS);
        when(mockTicketDAO.getTicketCountsByStatus()).thenThrow(new SQLException("Test DB error for statuses"));

        window.button("btnGenerateReport").click();
        Pause.pause(100);

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireErrorMessage();
        String messageText = GuiActionRunner.execute(() -> optionPane.target().getMessage().toString());
        assertThat(messageText).contains("Помилка при генерації звіту 'Статистика по статусах квитків'");
        assertThat(messageText).contains("Test DB error for statuses");
        optionPane.okButton().click();
    }

    @Override
    protected void onTearDown() {
        Mockito.reset(mockTicketDAO, mockFlightDAO);
    }
}