package Panel;

import DAO.FlightDAO;
import DAO.RouteDAO;
import DAO.StopDAO;
import Models.Enums.FlightStatus;
import Models.Flight;
import Models.Route;
import Models.Stop;
import UI.Dialog.FlightDialog;
import UI.Dialog.RouteCreationDialog;
import UI.Model.FlightsTableModel;

import UI.Panel.FlightsPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightsPanelTest {

    @Mock private FlightDAO mockFlightDAO;
    @Mock private RouteDAO mockRouteDAO;
    @Mock private StopDAO mockStopDAO;

    // Mockito.mockConstruction буде використовуватися для діалогів
    // @Mock private FlightDialog mockFlightDialog; // Не потрібні як поля, якщо використовуємо mockConstruction
    // @Mock private RouteCreationDialog mockRouteCreationDialog;

    private FlightsPanel flightsPanel;
    private List<Flight> sampleFlights;
    private Flight flight1, flight2;
    private Route route1, route2;
    private JFrame testFrame; // Для коректного батьківського вікна діалогів

    @Captor private ArgumentCaptor<Flight> flightCaptor;
    @Captor private ArgumentCaptor<Route> routeCaptor;

    @BeforeEach
    void setUp() throws SQLException, InvocationTargetException, InterruptedException {
        FlightsPanel.setSuppressMessagesForTesting(true);
        FlightDialog.setSuppressMessagesForTesting(true);
        RouteCreationDialog.setSuppressMessagesForTesting(true);

        // Створюємо JFrame для тестів, щоб діалоги мали коректного власника
        SwingUtilities.invokeAndWait(() -> {
            testFrame = new JFrame();
            testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        });


        Stop stopA = new Stop(1, "A", "CityA");
        Stop stopB = new Stop(2, "B", "CityB");
        Stop stopC = new Stop(3, "C", "CityC");

        route1 = new Route(10, stopA, stopB, new ArrayList<>());
        route2 = new Route(20, stopB, stopC, new ArrayList<>());

        flight1 = new Flight(1L, route1, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), 50, FlightStatus.PLANNED, "Bus1", new BigDecimal("100"));
        flight2 = new Flight(2L, route2, LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(5), 30, FlightStatus.PLANNED, "Bus2", new BigDecimal("150"));
        sampleFlights = new ArrayList<>(Arrays.asList(flight1, flight2));

        when(mockFlightDAO.getAllFlights()).thenReturn(new ArrayList<>(sampleFlights)); // Повертаємо копію

        SwingUtilities.invokeAndWait(() -> {
            flightsPanel = new FlightsPanel(mockFlightDAO, mockRouteDAO, mockStopDAO);
            // Додаємо панель до фрейму, щоб вона мала батьківське вікно
            testFrame.add(flightsPanel);
            testFrame.pack();
            // testFrame.setVisible(true); // Не обов'язково робити видимим для тестів логіки
        });
    }

    @AfterEach
    void tearDown() throws InvocationTargetException, InterruptedException {
        FlightsPanel.setSuppressMessagesForTesting(false);
        FlightDialog.setSuppressMessagesForTesting(false);
        RouteCreationDialog.setSuppressMessagesForTesting(false);

        if (testFrame != null) {
            SwingUtilities.invokeAndWait(() -> testFrame.dispose());
        }
        // Очищення статичних моків, якщо вони використовуються глобально (тут не використовуються)
        // Mockito.framework().clearInlineMocks();
    }

    @Test
    @DisplayName("Ініціалізація: компоненти створені, дані завантажені")
    void constructor_initializesUIAndLoadsData() throws SQLException {
        assertNotNull(flightsPanel.getFlightsTable());
        assertNotNull(flightsPanel.getFlightsTableModel());
        assertEquals(2, flightsPanel.getFlightsTableModel().getRowCount());
        verify(mockFlightDAO, times(1)).getAllFlights();
    }

    @Test
    @DisplayName("loadFlightsData: успішне завантаження")
    void loadFlightsData_success() throws SQLException {
        List<Flight> newFlights = Arrays.asList(
                new Flight(3L, route1, LocalDateTime.now(), LocalDateTime.now().plusHours(1), 20, FlightStatus.ARRIVED, "Bus3", new BigDecimal("50"))
        );
        when(mockFlightDAO.getAllFlights()).thenReturn(newFlights);

        flightsPanel.loadFlightsData();

        assertEquals(1, flightsPanel.getFlightsTableModel().getRowCount());
        verify(mockFlightDAO, times(2)).getAllFlights();
    }

    @Test
    @DisplayName("loadFlightsData: SQLException")
    void loadFlightsData_sqlException() throws SQLException {
        when(mockFlightDAO.getAllFlights()).thenThrow(new SQLException("Test DB error"));
        int initialRowCount = flightsPanel.getFlightsTableModel().getRowCount();

        flightsPanel.loadFlightsData();

        assertEquals(initialRowCount, flightsPanel.getFlightsTableModel().getRowCount());
        // В FlightsPanel JOptionPane придушено, помилка логується
    }


    @Test
    @DisplayName("Кнопка 'Редагувати рейс': нічого не вибрано")
    void editFlightAction_noRowSelected() throws InvocationTargetException, InterruptedException, SQLException {
        SwingUtilities.invokeAndWait(() -> flightsPanel.getFlightsTable().clearSelection());

        SwingUtilities.invokeAndWait(() -> flightsPanel.getBtnEditFlight().doClick());

        // JOptionPane (попередження) придушено. Перевіряємо, що діалог не відкрився.
        // mockFlightDAO.getAllFlights() викликається тільки при ініціалізації.
        verify(mockFlightDAO, times(1)).getAllFlights();
    }

    @Test
    @DisplayName("Кнопка 'Скасувати рейс': успішне скасування")
    void cancelFlightAction_success() throws SQLException, InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> flightsPanel.getFlightsTable().setRowSelectionInterval(0, 0)); // Вибираємо flight1 (SCHEDULED)

        // suppressMessagesForTesting = true, тому showConfirmDialog в панелі поверне YES_OPTION
        when(mockFlightDAO.updateFlightStatus(flight1.getId(), FlightStatus.CANCELLED)).thenReturn(true);

        SwingUtilities.invokeAndWait(() -> flightsPanel.getBtnCancelFlight().doClick());

        verify(mockFlightDAO).updateFlightStatus(flight1.getId(), FlightStatus.CANCELLED);
        verify(mockFlightDAO, times(2)).getAllFlights(); // Початкове + оновлення
    }

    @Test
    @DisplayName("Кнопка 'Скасувати рейс': рейс вже скасовано")
    void cancelFlightAction_alreadyCancelled() throws InvocationTargetException, InterruptedException, SQLException {
        flight1.setStatus(FlightStatus.CANCELLED); // Встановлюємо статус
        // Потрібно оновити модель таблиці, щоб вона відображала змінений статус
        flightsPanel.getFlightsTableModel().setFlights(new ArrayList<>(sampleFlights)); // Оновлюємо модель
        SwingUtilities.invokeAndWait(() -> flightsPanel.getFlightsTable().setRowSelectionInterval(0, 0));

        SwingUtilities.invokeAndWait(() -> flightsPanel.getBtnCancelFlight().doClick());

        verify(mockFlightDAO, never()).updateFlightStatus(anyLong(), any(FlightStatus.class));
    }

    @Test
    @DisplayName("Кнопка 'Скасувати рейс': рейс відправлений")
    void cancelFlightAction_departed() throws InvocationTargetException, InterruptedException, SQLException {
        flight1.setStatus(FlightStatus.DEPARTED);
        flightsPanel.getFlightsTableModel().setFlights(new ArrayList<>(sampleFlights));
        SwingUtilities.invokeAndWait(() -> flightsPanel.getFlightsTable().setRowSelectionInterval(0, 0));

        SwingUtilities.invokeAndWait(() -> flightsPanel.getBtnCancelFlight().doClick());

        verify(mockFlightDAO, never()).updateFlightStatus(anyLong(), any(FlightStatus.class));
    }

    @Test
    @DisplayName("Кнопка 'Оновити список'")
    void refreshFlightsAction_reloadsData() throws SQLException, InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> flightsPanel.getBtnRefreshFlights().doClick());
        verify(mockFlightDAO, times(2)).getAllFlights();
    }
}