package Panel; // Або ваш дійсний пакет

import DAO.TicketDAO;
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;
import Models.Flight;
import Models.Passenger;
import Models.Route;
import Models.Stop;
import Models.Ticket;
import UI.Panel.BookingsManagementPanel;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BookingsManagementPanelTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;
    private TicketDAO mockTicketDAO;

    private Ticket ticketBooked, ticketSold, ticketCancelled, ticketForPastFlight;
    private Flight flightUpcoming, flightPast, flightPlannedCanBeCancelled;
    private Passenger passenger1;

    // !!! ВАЖЛИВО: Адаптуйте ці індекси до вашої BookingsTableModel !!!
    private static final int COL_BOOKING_ID = 0;
    private static final int COL_BOOKING_FLIGHT_ID = 1;
    private static final int COL_BOOKING_PASSENGER = 2; // Припускаємо, що це ПІБ пасажира
    private static final int COL_BOOKING_SEAT = 3;
    private static final int COL_BOOKING_ROUTE = 4; // Припускаємо, що це опис маршруту
    private static final int COL_BOOKING_DEPARTURE = 5;
    private static final int COL_BOOKING_PRICE = 6; // Припускаємо, що це PricePaid
    private static final int COL_BOOKING_STATUS = 7;

    private static final DateTimeFormatter BOOKING_TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    @Override
    protected void onSetUp() {
        if (mockTicketDAO != null) Mockito.reset(mockTicketDAO);
        mockTicketDAO = mock(TicketDAO.class);

        // Тестові дані
        passenger1 = new Passenger(1L, "Тест Пасажиренко", "TE123456", "Паспорт", "0509998877", "test@test.com", Models.Enums.BenefitType.NONE);
        Stop stopA = new Stop(1L, "Місто А", "А");
        Stop stopB = new Stop(2L, "Місто Б", "Б");
        Route routeAB = new Route(10L, stopA, stopB, Collections.emptyList());

        LocalDateTime now = LocalDateTime.now();
        flightUpcoming = new Flight(101L, routeAB, now.plusDays(2), now.plusDays(2).plusHours(2), 50, FlightStatus.PLANNED, "BusModern", BigDecimal.valueOf(200));
        flightPast = new Flight(102L, routeAB, now.minusDays(1), now.minusDays(1).plusHours(2), 50, FlightStatus.ARRIVED, "BusOld", BigDecimal.valueOf(150));
        flightPlannedCanBeCancelled = new Flight(103L, routeAB, now.plusHours(5), now.plusHours(7), 50, FlightStatus.PLANNED, "BusOkay", BigDecimal.valueOf(180));


        ticketBooked = new Ticket(1L, flightUpcoming, passenger1, "A1", now.minusHours(5), BigDecimal.valueOf(200), TicketStatus.BOOKED);
        ticketSold = new Ticket(2L, flightUpcoming, passenger1, "B2", now.minusHours(3), BigDecimal.valueOf(200), TicketStatus.SOLD);
        ticketSold.setPurchaseDateTime(now.minusHours(2)); // Встановлюємо час покупки
        ticketCancelled = new Ticket(3L, flightUpcoming, passenger1, "C3", now.minusHours(1), BigDecimal.valueOf(190), TicketStatus.CANCELLED);
        ticketForPastFlight = new Ticket(4L, flightPast, passenger1, "D4", now.minusDays(2), BigDecimal.valueOf(150), TicketStatus.SOLD);
        ticketForPastFlight.setPurchaseDateTime(now.minusDays(2).plusHours(1));


        try {
            // За замовчуванням (фільтр null - всі) повертаємо всі квитки
            when(mockTicketDAO.getAllTickets(null)).thenReturn(Arrays.asList(ticketBooked, ticketSold, ticketCancelled, ticketForPastFlight));
            // Налаштування для інших фільтрів, якщо потрібно в onSetUp, або в конкретних тестах
            when(mockTicketDAO.getAllTickets(TicketStatus.BOOKED)).thenReturn(Collections.singletonList(ticketBooked));
            when(mockTicketDAO.getAllTickets(TicketStatus.SOLD)).thenReturn(Arrays.asList(ticketSold, ticketForPastFlight));

        } catch (SQLException e) {
            org.assertj.core.api.Assertions.fail("SQLException during mock setup: " + e.getMessage());
        }

        BookingsManagementPanel panel = GuiActionRunner.execute(() -> new BookingsManagementPanel(mockTicketDAO));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame testFrame = new JFrame("Bookings Management Test");
            testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            testFrame.setContentPane(panel);
            testFrame.pack();
            return testFrame;
        });
        window = new FrameFixture(robot(), frame);
        window.show();
    }

    @Test
    public void testInitialDataLoad_ShowsAllTickets() throws SQLException {
        JTableFixture bookingsTable = window.table("bookingsTable");
        bookingsTable.requireRowCount(4); // Всі 4 квитки
        verify(mockTicketDAO, times(1)).getAllTickets(null);

        // Перевірка одного з рядків (наприклад, ticketBooked)
        // Вам потрібно знайти цей рядок, бо порядок може бути не гарантований сортуванням
        int bookedRow = -1;
        for (int i = 0; i < bookingsTable.rowCount(); i++) {
            if (bookingsTable.valueAt(TableCell.row(i).column(COL_BOOKING_ID)).equals(String.valueOf(ticketBooked.getId()))) {
                bookedRow = i;
                break;
            }
        }
        assertThat(bookedRow).isNotEqualTo(-1);
        bookingsTable.requireCellValue(TableCell.row(bookedRow).column(COL_BOOKING_STATUS), TicketStatus.BOOKED.getDisplayName());
    }

    @Test
    public void testFilterByStatus_Booked() throws SQLException {
        window.comboBox("cmbStatusFilter").selectItem(TicketStatus.BOOKED.getDisplayName()); // Вибір за текстом з рендерера
        Pause.pause(200); // Пауза для оновлення таблиці

        JTableFixture bookingsTable = window.table("bookingsTable");
        bookingsTable.requireRowCount(1);
        bookingsTable.requireCellValue(TableCell.row(0).column(COL_BOOKING_ID), String.valueOf(ticketBooked.getId()));
        bookingsTable.requireCellValue(TableCell.row(0).column(COL_BOOKING_STATUS), TicketStatus.BOOKED.getDisplayName());

        verify(mockTicketDAO, times(1)).getAllTickets(TicketStatus.BOOKED);
    }

    @Test
    public void testFilterByStatus_All() throws SQLException {
        // Спочатку встановлюємо фільтр, потім скидаємо на "Всі статуси"
        window.comboBox("cmbStatusFilter").selectItem(TicketStatus.BOOKED.getDisplayName());
        Pause.pause(200);
        window.table("bookingsTable").requireRowCount(1);

        window.comboBox("cmbStatusFilter").selectItem("Всі статуси"); // Вибір "Всі статуси"
        Pause.pause(200);

        window.table("bookingsTable").requireRowCount(4);
        verify(mockTicketDAO, times(2)).getAllTickets(null); // 1 в onSetUp, 1 після зміни фільтра на "Всі"
    }

    @Test
    public void testRefreshButton_ReloadsDataWithCurrentFilter() throws SQLException {
        // Встановлюємо фільтр BOOKED
        window.comboBox("cmbStatusFilter").selectItem(TicketStatus.BOOKED.getDisplayName());
        Pause.pause(200);
        window.table("bookingsTable").requireRowCount(1);
        verify(mockTicketDAO, times(1)).getAllTickets(TicketStatus.BOOKED);

        // Симулюємо, що з'явився ще один заброньований квиток
        Ticket newBookedTicket = new Ticket(5L, flightUpcoming, passenger1, "Z5", LocalDateTime.now(), BigDecimal.TEN, TicketStatus.BOOKED);
        when(mockTicketDAO.getAllTickets(TicketStatus.BOOKED)).thenReturn(Arrays.asList(ticketBooked, newBookedTicket));

        window.button("btnRefresh").click();
        Pause.pause(200);

        window.table("bookingsTable").requireRowCount(2);
        // Перевіряємо, що getAllTickets викликався знову з фільтром BOOKED
        verify(mockTicketDAO, times(2)).getAllTickets(TicketStatus.BOOKED);
    }

    @Test
    public void testSellButton_EnabledOnlyForBookedTickets() {
        JTableFixture bookingsTable = window.table("bookingsTable");
        // Рядок з ticketBooked (має бути першим, якщо початкове сортування за ID або часом)
        // Краще знайти рядок за ID, щоб тест був надійнішим
        int bookedRow = findRowById(bookingsTable, ticketBooked.getId());
        assertThat(bookedRow).isNotEqualTo(-1);

        bookingsTable.selectRows(bookedRow);
        Pause.pause(100);
        window.button("btnSellTicket").requireEnabled();
        window.button("btnCancelBookingTicket").requireEnabled();

        // Рядок з ticketSold
        int soldRow = findRowById(bookingsTable, ticketSold.getId());
        assertThat(soldRow).isNotEqualTo(-1);

        bookingsTable.selectRows(soldRow);
        Pause.pause(100);
        window.button("btnSellTicket").requireDisabled();
        window.button("btnCancelBookingTicket").requireEnabled(); // Можна скасувати проданий
    }

    @Test
    public void testSellTicketAction_Successful() throws SQLException {
        JTableFixture bookingsTable = window.table("bookingsTable");
        int bookedRow = findRowById(bookingsTable, ticketBooked.getId());
        assertThat(bookedRow).isNotEqualTo(-1);
        bookingsTable.selectRows(bookedRow);
        Pause.pause(100);

        when(mockTicketDAO.updateTicketStatus(eq(ticketBooked.getId()), eq(TicketStatus.SOLD), any(LocalDateTime.class)))
                .thenReturn(true);

        window.button("btnSellTicket").click();
        Pause.pause(100); // Для діалогу підтвердження

        JOptionPaneFinder.findOptionPane().using(robot()).yesButton().click();
        Pause.pause(100); // Для діалогу успіху

        JOptionPaneFinder.findOptionPane().using(robot()).requireInformationMessage().requireMessage("Квиток успішно продано.").okButton().click();

        verify(mockTicketDAO).updateTicketStatus(eq(ticketBooked.getId()), eq(TicketStatus.SOLD), any(LocalDateTime.class));
        // Перевірка, що дані перезавантажились (з поточним фільтром "Всі")
        verify(mockTicketDAO, times(2)).getAllTickets(null); // 1 в onSetUp, 1 після продажу
    }

    @Test
    public void testCancelTicketAction_BookedTicket_Successful() throws SQLException {
        JTableFixture bookingsTable = window.table("bookingsTable");
        int bookedRow = findRowById(bookingsTable, ticketBooked.getId());
        assertThat(bookedRow).isNotEqualTo(-1);
        bookingsTable.selectRows(bookedRow);
        Pause.pause(100);

        when(mockTicketDAO.updateTicketStatus(ticketBooked.getId(), TicketStatus.CANCELLED, null)).thenReturn(true);

        window.button("btnCancelBookingTicket").click();
        Pause.pause(100);

        JOptionPaneFinder.findOptionPane().using(robot()).yesButton().click(); // Підтвердження
        Pause.pause(100);

        JOptionPaneFinder.findOptionPane().using(robot()).requireInformationMessage().requireMessage("Бронювання успішно скасовано.").okButton().click(); // Або "Квиток...", залежно від логіки

        verify(mockTicketDAO).updateTicketStatus(ticketBooked.getId(), TicketStatus.CANCELLED, null);
        verify(mockTicketDAO, times(2)).getAllTickets(null);
    }

    @Test
    public void testCancelTicketAction_SoldTicket_ForUpcomingFlight_Successful() throws SQLException {
        // Створюємо "проданий" квиток на майбутній рейс, який можна скасувати
        Ticket soldUpcomingCancellable = new Ticket(5L, flightPlannedCanBeCancelled, passenger1, "E5", LocalDateTime.now().minusDays(1), BigDecimal.valueOf(180), TicketStatus.SOLD);
        soldUpcomingCancellable.setPurchaseDateTime(LocalDateTime.now().minusHours(1));

        // Переналаштовуємо мок для getAllTickets(null)
        when(mockTicketDAO.getAllTickets(null)).thenReturn(Arrays.asList(ticketBooked, soldUpcomingCancellable, ticketCancelled, ticketForPastFlight));

        // Оновлюємо таблицю, щоб побачити цей новий квиток
        GuiActionRunner.execute(() -> ((BookingsManagementPanel)window.panel().target()).loadBookingsData(null));
        Pause.pause(100);


        JTableFixture bookingsTable = window.table("bookingsTable");
        int soldRow = findRowById(bookingsTable, soldUpcomingCancellable.getId());
        assertThat(soldRow).isNotEqualTo(-1).as("Sold upcoming cancellable ticket not found");

        bookingsTable.selectRows(soldRow);
        Pause.pause(100);
        window.button("btnCancelBookingTicket").requireEnabled();


        when(mockTicketDAO.updateTicketStatus(soldUpcomingCancellable.getId(), TicketStatus.CANCELLED, null)).thenReturn(true);

        window.button("btnCancelBookingTicket").click();
        Pause.pause(100);

        JOptionPaneFinder.findOptionPane().using(robot()).yesButton().click();
        Pause.pause(100);

        JOptionPaneFinder.findOptionPane().using(robot()).requireInformationMessage().requireMessage("Квиток успішно скасовано.").okButton().click();

        verify(mockTicketDAO).updateTicketStatus(soldUpcomingCancellable.getId(), TicketStatus.CANCELLED, null);
    }


    @Test
    public void testCancelTicketAction_TicketForPastFlight_ShowsError() {
        JTableFixture bookingsTable = window.table("bookingsTable");
        int pastFlightRow = findRowById(bookingsTable, ticketForPastFlight.getId());
        assertThat(pastFlightRow).isNotEqualTo(-1);

        bookingsTable.selectRows(pastFlightRow);
        Pause.pause(100);
        window.button("btnCancelBookingTicket").requireEnabled(); // Кнопка може бути активною

        window.button("btnCancelBookingTicket").click();
        Pause.pause(100);

        JOptionPaneFinder.findOptionPane().using(robot())
                .requireErrorMessage().requireMessage("Неможливо скасувати квиток на рейс, який вже відбувся або має статус, що не дозволяє скасування.")
                .okButton().click();
    }

    private int findRowById(JTableFixture table, long ticketId) {
        for (int i = 0; i < table.rowCount(); i++) {
            if (table.valueAt(TableCell.row(i).column(COL_BOOKING_ID)).equals(String.valueOf(ticketId))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onTearDown() {
        Mockito.reset(mockTicketDAO);
    }
}