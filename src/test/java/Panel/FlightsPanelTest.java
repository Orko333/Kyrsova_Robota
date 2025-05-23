package Panel; // Або ваш дійсний пакет

import DAO.FlightDAO;
import DAO.RouteDAO;
import DAO.StopDAO;
import Models.Enums.FlightStatus;
import Models.Flight;
import Models.Route;
import Models.Stop;
import UI.Dialog.FlightDialog;
import UI.Dialog.RouteCreationDialog;
import UI.Panel.FlightsPanel;
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
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FlightsPanelTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;

    private FlightDAO mockFlightDAO;
    private RouteDAO mockRouteDAO;
    private StopDAO mockStopDAO;

    private Flight flight1, flight2_departed, flight3_cancelled;
    private Route route1, route2;
    private Stop stopA, stopB, stopC;

    // !!! ВАЖЛИВО: Адаптуйте ці індекси до вашої FlightsTableModel !!!
    private static final int COL_FLIGHT_ID = 0;
    private static final int COL_FLIGHT_ROUTE_DESC = 1;
    private static final int COL_FLIGHT_DEPARTURE = 2;
    private static final int COL_FLIGHT_ARRIVAL = 3;
    private static final int COL_FLIGHT_TOTAL_SEATS = 4;
    private static final int COL_FLIGHT_STATUS = 5;
    private static final int COL_FLIGHT_PRICE = 6;
    private static final int COL_FLIGHT_BUS_MODEL = 7;

    private static final DateTimeFormatter FLIGHT_TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    @Override
    protected void onSetUp() {
        if (mockFlightDAO != null) Mockito.reset(mockFlightDAO);
        if (mockRouteDAO != null) Mockito.reset(mockRouteDAO);
        if (mockStopDAO != null) Mockito.reset(mockStopDAO);

        mockFlightDAO = mock(FlightDAO.class);
        mockRouteDAO = mock(RouteDAO.class);
        mockStopDAO = mock(StopDAO.class);

        stopA = new Stop(1L, "Київ-Центральний", "Київ");
        stopB = new Stop(2L, "Львів-Головний", "Львів");
        stopC = new Stop(3L, "Одеса-Привоз", "Одеса");

        route1 = new Route(10L, stopA, stopB, Collections.emptyList());
        route2 = new Route(11L, stopB, stopC, Collections.emptyList());

        LocalDateTime now = LocalDateTime.now();
        flight1 = new Flight(101L, route1, now.plusDays(1).withHour(10).withMinute(0), now.plusDays(1).withHour(15).withMinute(0), 50, FlightStatus.PLANNED, "Neoplan", BigDecimal.valueOf(500.00));
        flight2_departed = new Flight(102L, route2, now.minusHours(2), now.minusHours(1).plusMinutes(30), 40, FlightStatus.DEPARTED, "Mercedes", BigDecimal.valueOf(400.00));
        flight3_cancelled = new Flight(103L, route1, now.plusDays(2).withHour(9).withMinute(0), now.plusDays(2).withHour(14).withMinute(0), 30, FlightStatus.CANCELLED, "Setra", BigDecimal.valueOf(550.00));

        try {
            when(mockFlightDAO.getAllFlights()).thenReturn(Arrays.asList(flight1, flight2_departed, flight3_cancelled));
            when(mockStopDAO.getAllStops()).thenReturn(Arrays.asList(stopA, stopB, stopC));
            when(mockRouteDAO.getAllRoutes()).thenReturn(Arrays.asList(route1, route2));
        } catch (SQLException e) {
            org.assertj.core.api.Assertions.fail("SQLException during mock setup: " + e.getMessage());
        }

        FlightsPanel panel = GuiActionRunner.execute(() -> new FlightsPanel(mockFlightDAO, mockRouteDAO, mockStopDAO));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame testFrame = new JFrame("Flights Test");
            testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            testFrame.setContentPane(panel);
            testFrame.pack();
            return testFrame;
        });
        window = new FrameFixture(robot(), frame);
        window.show();
    }


    @Test
    public void testEditFlightButton_NoSelection_ShowsWarning() {
        GuiActionRunner.execute(() -> window.table("flightsTable").target().clearSelection());
        window.button("btnEditFlight").click();
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireWarningMessage().requireMessage("Будь ласка, виберіть рейс для редагування.");
        optionPane.okButton().click();
    }

    @Test
    public void testCancelFlightButton_NoSelection_ShowsWarning() {
        GuiActionRunner.execute(() -> window.table("flightsTable").target().clearSelection());
        window.button("btnCancelFlight").click();
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireWarningMessage().requireMessage("Будь ласка, виберіть рейс для скасування.");
        optionPane.okButton().click();
    }

    @Test
    public void testCancelFlightButton_AlreadyCancelledFlight_ShowsInfo() {
        JTableFixture flightsTable = window.table("flightsTable");
        int flight3RowIndex = -1;
        for (int i = 0; i < flightsTable.rowCount(); i++) {
            if (flightsTable.valueAt(TableCell.row(i).column(COL_FLIGHT_ID)).equals(String.valueOf(flight3_cancelled.getId()))) {
                flight3RowIndex = i;
                break;
            }
        }
        assertThat(flight3RowIndex).isNotEqualTo(-1).as("Flight3 (CANCELLED) not found.");
        flightsTable.selectRows(flight3RowIndex);
        Pause.pause(100);

        window.button("btnCancelFlight").click();
        Pause.pause(100);

        JOptionPaneFixture infoDialog = JOptionPaneFinder.findOptionPane().using(robot());
        infoDialog.requireInformationMessage().requireMessage("Цей рейс вже скасовано.");
        infoDialog.okButton().click();
    }

    @Test
    public void testCancelFlightButton_DepartedFlight_ShowsError() {
        JTableFixture flightsTable = window.table("flightsTable");
        int flight2RowIndex = -1;
        for (int i = 0; i < flightsTable.rowCount(); i++) {
            if (flightsTable.valueAt(TableCell.row(i).column(COL_FLIGHT_ID)).equals(String.valueOf(flight2_departed.getId()))) {
                flight2RowIndex = i;
                break;
            }
        }
        assertThat(flight2RowIndex).isNotEqualTo(-1).as("Flight2 (DEPARTED) not found.");
        flightsTable.selectRows(flight2RowIndex);
        Pause.pause(100);

        window.button("btnCancelFlight").click();
        Pause.pause(100);

        JOptionPaneFixture errorDialog = JOptionPaneFinder.findOptionPane().using(robot());
        errorDialog.requireErrorMessage().requireMessage("Неможливо скасувати рейс, який вже відправлений або прибув.");
        errorDialog.okButton().click();
    }

    @Test
    public void testLoadFlightsData_HandlesSQLException() throws SQLException {
        when(mockFlightDAO.getAllFlights()).thenThrow(new SQLException("Test DB error loading flights"));
        window.button("btnRefreshFlights").click();
        Pause.pause(200);
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot());
        optionPane.requireErrorMessage();
        String messageText = GuiActionRunner.execute(() -> optionPane.target().getMessage().toString());
        assertThat(messageText).contains("Не вдалося завантажити список рейсів");
        assertThat(messageText).contains("Test DB error loading flights");
        optionPane.okButton().click();
        verify(mockFlightDAO, times(2)).getAllFlights(); // 1 в onSetUp, 1 при помилці
    }

    @Override
    protected void onTearDown() {
        Mockito.reset(mockFlightDAO, mockRouteDAO, mockStopDAO);
    }
}