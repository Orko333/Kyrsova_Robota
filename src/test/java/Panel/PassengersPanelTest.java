package Panel; // Або ваш дійсний пакет

import DAO.PassengerDAO;
import DAO.TicketDAO;
import Models.Enums.BenefitType;
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;
import Models.Flight;
import Models.Passenger;
import Models.Route;
import Models.Stop;
import Models.Ticket;
import UI.Dialog.PassengerDialog;
import UI.Panel.PassengersPanel;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PassengersPanelTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;

    private PassengerDAO mockPassengerDAO;
    private TicketDAO mockTicketDAO;

    private Passenger passenger1, passenger2;
    private Ticket ticket1_p1, ticket2_p1, ticket3_p2;
    private Flight flight1, flight2;

    private static final DateTimeFormatter HISTORY_TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    protected void onSetUp() {
        // Скидання моків перед кожним тестом, щоб уникнути взаємного впливу налаштувань
        // якщо вони не переналаштовуються повністю.
        // Якщо onSetUp завжди повністю переналаштовує моки, це може бути зайвим.
        if (mockPassengerDAO != null) Mockito.reset(mockPassengerDAO);
        if (mockTicketDAO != null) Mockito.reset(mockTicketDAO);

        mockPassengerDAO = mock(PassengerDAO.class);
        mockTicketDAO = mock(TicketDAO.class);

        passenger1 = new Passenger(1L, "Іван Петренко", "АА123456", "Паспорт громадянина України", "0501234567", "ivan@example.com", BenefitType.NONE);
        passenger2 = new Passenger(2L, "Марія Коваленко", "ВВ654321", "ID-картка", "0978765432", "maria@example.com", BenefitType.STUDENT);

        LocalDateTime now = LocalDateTime.now();
        Stop stopA = new Stop(1, "Автостанція 'Центр'", "Київ");
        Stop stopB = new Stop(2, "Автовокзал 'Східний'", "Харків");
        Route routeAB = new Route(1, stopA, stopB, Collections.emptyList());

        flight1 = new Flight(101L, routeAB, now.plusDays(1).withHour(10), now.plusDays(1).withHour(15), 50, FlightStatus.PLANNED, "Neoplan N316", BigDecimal.valueOf(350.00));
        flight2 = new Flight(102L, routeAB, now.plusDays(3).withHour(12), now.plusDays(3).withHour(17), 40, FlightStatus.DELAYED, "Mercedes Tourismo", BigDecimal.valueOf(320.00));

        ticket1_p1 = new Ticket(1001L, flight1, passenger1, "10A", LocalDateTime.now().minusDays(2), BigDecimal.valueOf(350.00), TicketStatus.SOLD);
        ticket2_p1 = new Ticket(1002L, flight2, passenger1, "5B", LocalDateTime.now().minusDays(1), BigDecimal.valueOf(300.00), TicketStatus.BOOKED);
        ticket3_p2 = new Ticket(1003L, flight1, passenger2, "12C", LocalDateTime.now().minusDays(2), BigDecimal.valueOf(175.00), TicketStatus.SOLD);

        try {
            // Налаштування для початкового завантаження в конструкторі PassengersPanel
            when(mockPassengerDAO.getAllPassengers()).thenReturn(Arrays.asList(passenger1, passenger2));
            // getTicketsByPassengerId викликається тільки після вибору пасажира
            when(mockTicketDAO.getTicketsByPassengerId(passenger1.getId())).thenReturn(Arrays.asList(ticket1_p1, ticket2_p1));
            when(mockTicketDAO.getTicketsByPassengerId(passenger2.getId())).thenReturn(Collections.singletonList(ticket3_p2));
        } catch (SQLException e) {
            org.assertj.core.api.Assertions.fail("SQLException during mock setup: " + e.getMessage());
        }

        PassengersPanel panel = GuiActionRunner.execute(() -> new PassengersPanel(mockPassengerDAO, mockTicketDAO));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame testFrame = new JFrame("Passengers Test");
            testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            testFrame.setContentPane(panel);
            testFrame.pack();
            return testFrame;
        });

        window = new FrameFixture(robot(), frame);
        window.show();
    }

    @Test
    public void testInitialDataLoad() throws SQLException {
        JTableFixture passengersTable = window.table("passengersTable");
        passengersTable.requireRowCount(2);
        // !!! АДАПТУЙТЕ ІНДЕКСИ КОЛОНОК !!!
        // Припустимо: 0-ID, 1-FullName, 2-DocType, 3-DocNum, 4-Phone, 5-Email, 6-Benefit
        passengersTable.requireCellValue(TableCell.row(0).column(1), passenger1.getFullName());
        passengersTable.requireCellValue(TableCell.row(0).column(2), passenger1.getDocumentType());
        passengersTable.requireCellValue(TableCell.row(1).column(1), passenger2.getFullName());
        passengersTable.requireCellValue(TableCell.row(1).column(6), passenger2.getBenefitType().getDisplayName());

        JTableFixture historyTable = window.table("historyTable");
        historyTable.requireRowCount(0);

        // Перевірка, що getAllPassengers викликався 1 раз при ініціалізації
        verify(mockPassengerDAO, times(1)).getAllPassengers();
    }

    @Test
    public void testSelectPassenger_LoadsHistory() throws SQLException {
        JTableFixture passengersTable = window.table("passengersTable");
        JTableFixture historyTable = window.table("historyTable");

        passengersTable.selectRows(0); // Вибираємо passenger1
        Pause.pause(200); // Пауза для обробки ListSelectionListener

        verify(mockTicketDAO, times(1)).getTicketsByPassengerId(passenger1.getId());
        historyTable.requireRowCount(2);
        // !!! АДАПТУЙТЕ ІНДЕКСИ КОЛОНОК !!!
        // Припустимо: 0-TicketID, 1-FlightID, 2-Route, 3-Departure, 4-Seat, 5-Price, 6-Status
        historyTable.requireCellValue(TableCell.row(0).column(0), String.valueOf(ticket1_p1.getId()));
        historyTable.requireCellValue(TableCell.row(0).column(1), String.valueOf(ticket1_p1.getFlight().getId()));
        historyTable.requireCellValue(TableCell.row(0).column(2), ticket1_p1.getFlight().getRoute().getFullRouteDescription());
        historyTable.requireCellValue(TableCell.row(0).column(3), ticket1_p1.getFlight().getDepartureDateTime().format(HISTORY_TABLE_DATE_TIME_FORMATTER));
        historyTable.requireCellValue(TableCell.row(0).column(4), ticket1_p1.getSeatNumber());
        historyTable.requireCellValue(TableCell.row(0).column(5), ticket1_p1.getPricePaid().toString());
        historyTable.requireCellValue(TableCell.row(0).column(6), ticket1_p1.getStatus().getDisplayName());

        passengersTable.selectRows(1); // Вибираємо passenger2
        Pause.pause(200); // Пауза

        verify(mockTicketDAO, times(1)).getTicketsByPassengerId(passenger2.getId());
        historyTable.requireRowCount(1);
        historyTable.requireCellValue(TableCell.row(0).column(0), String.valueOf(ticket3_p2.getId()));
    }

    @Test
    public void testDeselectPassenger_ClearsHistory() throws SQLException {
        JTableFixture passengersTableFixture = window.table("passengersTable");
        JTableFixture historyTable = window.table("historyTable");

        passengersTableFixture.selectRows(0);
        Pause.pause(200); // Пауза для завантаження історії
        verify(mockTicketDAO, times(1)).getTicketsByPassengerId(passenger1.getId());
        historyTable.requireRowCount(2);

        GuiActionRunner.execute(() -> passengersTableFixture.target().clearSelection());
        Pause.pause(200); // Пауза для очищення історії
        historyTable.requireRowCount(0);
    }

    @Test
    public void testRefreshButton_ReloadsPassengers() throws SQLException {
        Passenger newPassenger = new Passenger(3L, "Олег Сидоренко", "СС777777", "Закордонний паспорт", "0671112233", "oleg@test.com", BenefitType.NONE);

        JTableFixture passengersTable = window.table("passengersTable");
        passengersTable.requireRowCount(2); // Початковий стан

        // Налаштовуємо мок для *наступного* виклику getAllPassengers (після кліку)
        when(mockPassengerDAO.getAllPassengers()).thenReturn(Collections.singletonList(newPassenger));

        window.button("btnRefreshPassengers").click();
        Pause.pause(100);

        passengersTable.requireRowCount(1);
        // !!! АДАПТУЙТЕ ІНДЕКСИ КОЛОНОК !!! (якщо потрібно перевіряти більше ніж fullName)
        passengersTable.requireCellValue(TableCell.row(0).column(1), newPassenger.getFullName());

        verify(mockPassengerDAO, times(2)).getAllPassengers(); // 1 в onSetUp, 1 після кліку
    }

    @Test
    public void testEditPassengerButton_NoSelection_ShowsWarning() {
        GuiActionRunner.execute(() -> window.table("passengersTable").target().clearSelection());
        Pause.pause(100); // Пауза, щоб ListSelectionListener встиг обробити (якщо він щось робить)
        window.button("btnEditPassenger").click();

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireWarningMessage().requireMessage("Будь ласка, виберіть пасажира для редагування.");
        optionPane.okButton().click();
    }

    @Test
    public void testEditPassengerButton_WithSelection_OpensDialog_AndSaves() throws SQLException {
        JTableFixture passengersTable = window.table("passengersTable");
        passengersTable.selectRows(0);
        Pause.pause(100);

        try (MockedConstruction<PassengerDialog> mockedDialog = Mockito.mockConstruction(PassengerDialog.class,
                (mock, context) -> {
                    // Перевірка аргументів конструктора
                    assertThat(context.arguments().size()).isGreaterThanOrEqualTo(2); // Owner, Passenger, DAO
                    assertThat(context.arguments().get(1)).isInstanceOf(Passenger.class);
                    assertThat(((Passenger) context.arguments().get(1)).getId()).isEqualTo(passenger1.getId());
                    assertThat(context.arguments().get(2)).isInstanceOf(PassengerDAO.class);
                    when(mock.isSaved()).thenReturn(true);
                })) {

            window.button("btnEditPassenger").click();

            assertThat(mockedDialog.constructed()).hasSize(1);
            verify(mockedDialog.constructed().get(0)).setVisible(true);
            verify(mockPassengerDAO, times(2)).getAllPassengers(); // 1 в onSetUp, 1 після збереження
        }
    }

    @Test
    public void testEditPassengerButton_WithSelection_OpensDialog_AndCancels() throws SQLException {
        JTableFixture passengersTable = window.table("passengersTable");
        passengersTable.selectRows(0);
        Pause.pause(100);

        try (MockedConstruction<PassengerDialog> mockedDialog = Mockito.mockConstruction(PassengerDialog.class,
                (mock, context) -> {
                    assertThat(context.arguments().size()).isGreaterThanOrEqualTo(2);
                    assertThat(context.arguments().get(1)).isInstanceOf(Passenger.class);
                    assertThat(((Passenger) context.arguments().get(1)).getId()).isEqualTo(passenger1.getId());
                    when(mock.isSaved()).thenReturn(false);
                })) {

            window.button("btnEditPassenger").click();

            assertThat(mockedDialog.constructed()).hasSize(1);
            verify(mockedDialog.constructed().get(0)).setVisible(true);
            verify(mockPassengerDAO, times(1)).getAllPassengers(); // Тільки початкове завантаження
        }
    }

    @Test
    public void testDoubleClickPassenger_OpensDialog_AndSaves() throws SQLException {
        JTableFixture passengersTable = window.table("passengersTable");

        try (MockedConstruction<PassengerDialog> mockedDialog = Mockito.mockConstruction(PassengerDialog.class,
                (mock, context) -> {
                    assertThat(context.arguments().size()).isGreaterThanOrEqualTo(2);
                    assertThat(context.arguments().get(1)).isInstanceOf(Passenger.class);
                    assertThat(((Passenger) context.arguments().get(1)).getId()).isEqualTo(passenger1.getId());
                    when(mock.isSaved()).thenReturn(true);
                })) {

            passengersTable.cell(TableCell.row(0).column(0)).doubleClick();

            assertThat(mockedDialog.constructed()).hasSize(1);
            verify(mockedDialog.constructed().get(0)).setVisible(true);
            verify(mockPassengerDAO, times(2)).getAllPassengers();
        }
    }


    @Test
    public void testLoadPassengersData_HandlesSQLException() throws SQLException {
        // Початкове завантаження в onSetUp пройшло з Arrays.asList(passenger1, passenger2)
        // Налаштовуємо, що *наступний* виклик getAllPassengers (після кліку) кине виняток
        when(mockPassengerDAO.getAllPassengers())
                .thenThrow(new SQLException("Test DB error loading passengers"));

        window.button("btnRefreshPassengers").click();
        Pause.pause(100); // Даємо час на показ JOptionPane

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireErrorMessage();
        String messageText = GuiActionRunner.execute(() -> optionPane.target().getMessage().toString());
        assertThat(messageText).contains("Помилка завантаження списку пасажирів");
        assertThat(messageText).contains("Test DB error loading passengers");
        optionPane.okButton().click();

        verify(mockPassengerDAO, times(2)).getAllPassengers(); // 1 в onSetUp, 1 після кліку
    }

    @Test
    public void testLoadPassengerHistory_HandlesSQLException() throws SQLException {
        JTableFixture passengersTable = window.table("passengersTable");
        // Налаштовуємо, що виклик getTicketsByPassengerId для passenger1 кине виняток
        when(mockTicketDAO.getTicketsByPassengerId(passenger1.getId()))
                .thenThrow(new SQLException("Test DB error loading history"));

        passengersTable.selectRows(0); // Вибір пасажира має викликати loadPassengerHistory
        Pause.pause(100); // Даємо час на показ JOptionPane

        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireErrorMessage();
        String messageText = GuiActionRunner.execute(() -> optionPane.target().getMessage().toString());
        assertThat(messageText).contains("Помилка завантаження історії поїздок для пасажира ID: " + passenger1.getId());
        assertThat(messageText).contains("Test DB error loading history");
        optionPane.okButton().click();

        verify(mockTicketDAO, times(1)).getTicketsByPassengerId(passenger1.getId());
    }

    @Override
    protected void onTearDown() {
        // Скидання стану моків після кожного тесту
        Mockito.reset(mockPassengerDAO, mockTicketDAO);
    }
}