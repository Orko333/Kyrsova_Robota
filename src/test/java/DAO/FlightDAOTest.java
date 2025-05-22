package DAO;

import DB.DatabaseConnectionManager;
import Models.Enums.FlightStatus;
import Models.Flight;
import Models.Route;
import Models.Stop;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private Statement mockStatement;
    @Mock
    private ResultSet mockResultSet;

    @Mock // Мокуємо залежність RouteDAO
    private RouteDAO mockRouteDAO;

    @InjectMocks // Інжектуємо моки в FlightDAO
    private FlightDAO flightDAO;

    private static ListAppender listAppender;
    private static org.apache.logging.log4j.core.Logger insuranceLogger;
    private static MockedStatic<DatabaseConnectionManager> mockedDbManager;

    // Test Data
    private Stop departureStop;
    private Stop destinationStop;
    private Route testRoute1;
    private Route testRoute2; // Може знадобитися для різноманітності
    private Flight testFlight1;
    private Flight testFlight2;

    private static class ListAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();
        ListAppender(String name) { super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY); }
        @Override public void append(LogEvent event) { events.add(event.toImmutable()); }
        public List<LogEvent> getEvents() { return events; }
        public void clearEvents() { events.clear(); }
        public boolean containsMessage(Level level, String partialMessage) {
            return events.stream().anyMatch(event ->
                    event.getLevel().equals(level) &&
                            event.getMessage().getFormattedMessage().contains(partialMessage));
        }
    }

    @BeforeAll
    static void setupLogAppenderAndStaticMock() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        insuranceLogger = context.getLogger("insurance.log");
        listAppender = new ListAppender("TestFlightDAOAppender");
        listAppender.start();
        insuranceLogger.addAppender(listAppender);
        insuranceLogger.setLevel(Level.ALL);
        mockedDbManager = Mockito.mockStatic(DatabaseConnectionManager.class);
    }

    @AfterAll
    static void tearDownLogAppenderAndStaticMock() {
        if (listAppender != null) {
            insuranceLogger.removeAppender(listAppender);
            listAppender.stop();
        }
        mockedDbManager.close();
    }

    @BeforeEach
    void setUp() throws SQLException {
        listAppender.clearEvents();
        mockedDbManager.when(DatabaseConnectionManager::getConnection).thenReturn(mockConnection);

        lenient().doNothing().when(mockResultSet).close();
        lenient().doNothing().when(mockPreparedStatement).close();
        lenient().doNothing().when(mockStatement).close();
        lenient().doNothing().when(mockConnection).close();

        departureStop = new Stop(10L, "Київ", "Центральний автовокзал");
        destinationStop = new Stop(20L, "Львів", "Автовокзал Стрийський");
        testRoute1 = new Route(1L, departureStop, destinationStop, Collections.emptyList());

        Stop departureStop2 = new Stop(30L, "Одеса", "АС Привоз");
        Stop destinationStop2 = new Stop(40L, "Харків", "АС-1");
        testRoute2 = new Route(2L, departureStop2, destinationStop2, Collections.emptyList());


        testFlight1 = new Flight(1L, testRoute1,
                LocalDateTime.of(2024, 1, 10, 10, 0),
                LocalDateTime.of(2024, 1, 10, 12, 0),
                50, FlightStatus.PLANNED, "BusModelX", new BigDecimal("25.00"));
        testFlight2 = new Flight(2L, testRoute2, // Використовуємо інший маршрут
                LocalDateTime.of(2024, 1, 11, 14, 0),
                LocalDateTime.of(2024, 1, 11, 16, 0),
                50, FlightStatus.DEPARTED, "BusModelY", new BigDecimal("30.00"));
    }

    @AfterEach
    void tearDown() {
        try {
            verify(mockConnection, atLeast(0)).close();
            verify(mockPreparedStatement, atLeast(0)).close();
            verify(mockStatement, atLeast(0)).close();
            verify(mockResultSet, atLeast(0)).close();
        } catch (SQLException e) {
            fail("SQLException during resource close verification: " + e.getMessage());
        }
    }


    @Test
    void getAllFlights_success_noFlights_returnsEmptyList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Flight> flights = flightDAO.getAllFlights();

        assertNotNull(flights);
        assertTrue(flights.isEmpty());
        assertTrue(listAppender.containsMessage(Level.INFO, "Успішно отримано 0 рейсів."));
    }


    @Test
    void getAllFlights_failure_sqlExceptionOnQuery_throwsSQLException() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException("DB Query Error"));

        SQLException exception = assertThrows(SQLException.class, () -> flightDAO.getAllFlights());
        assertEquals("DB Query Error", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при отриманні всіх рейсів"));
    }

    // --- addFlight ---
    @Test
    void addFlight_success_returnsTrueAndSetsId() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(123L);

        Flight newFlight = new Flight(0L, testRoute1, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                30, FlightStatus.PLANNED, "NewBus", BigDecimal.TEN);

        assertTrue(flightDAO.addFlight(newFlight));
        assertEquals(123L, newFlight.getId());
        assertTrue(listAppender.containsMessage(Level.INFO, "Рейс успішно додано. ID нового рейсу: 123"));

        verify(mockPreparedStatement).setLong(1, testRoute1.getId());
        verify(mockPreparedStatement).setString(7, FlightStatus.PLANNED.name());
    }

    @Test
    void addFlight_failure_executeUpdateReturnsZero_returnsFalse() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        Flight newFlight = new Flight(0L, testRoute1, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                30, FlightStatus.PLANNED, "NewBus", BigDecimal.TEN);

        assertFalse(flightDAO.addFlight(newFlight));
        assertEquals(0L, newFlight.getId());
        assertTrue(listAppender.containsMessage(Level.WARN, "Рейс не було додано (affectedRows = 0)."));
    }

    @Test
    void addFlight_failure_noGeneratedKey_returnsFalse() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Flight newFlight = new Flight(0L, testRoute1, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                30, FlightStatus.PLANNED, "NewBus", BigDecimal.TEN);

        assertFalse(flightDAO.addFlight(newFlight));
        assertEquals(0L, newFlight.getId());
        assertTrue(listAppender.containsMessage(Level.WARN, "Рейс додано (1 рядків), але не вдалося отримати згенерований ID."));
    }

    @Test
    void addFlight_failure_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("DB Insert Error"));

        Flight newFlight = new Flight(0L, testRoute1, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                30, FlightStatus.PLANNED, "NewBus", BigDecimal.TEN);

        SQLException exception = assertThrows(SQLException.class, () -> flightDAO.addFlight(newFlight));
        assertEquals("DB Insert Error", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при додаванні рейсу"));
    }

    // --- updateFlight ---
    @Test
    void updateFlight_success_returnsTrue() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(flightDAO.updateFlight(testFlight1));
        assertTrue(listAppender.containsMessage(Level.INFO, "Рейс з ID " + testFlight1.getId() + " успішно оновлено."));
        verify(mockPreparedStatement).setLong(8, testFlight1.getId());
        verify(mockPreparedStatement).setString(7, testFlight1.getStatus().name());
    }

    @Test
    void updateFlight_failure_executeUpdateReturnsZero_returnsFalse() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(flightDAO.updateFlight(testFlight1));
        assertTrue(listAppender.containsMessage(Level.WARN, "Рейс з ID " + testFlight1.getId() + " не знайдено або не було оновлено."));
    }

    @Test
    void updateFlight_failure_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Update Error"));

        SQLException exception = assertThrows(SQLException.class, () -> flightDAO.updateFlight(testFlight1));
        assertEquals("DB Update Error", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при оновленні рейсу з ID " + testFlight1.getId()));
    }

    // --- updateFlightStatus ---
    @Test
    void updateFlightStatus_success_returnsTrue() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(flightDAO.updateFlightStatus(testFlight1.getId(), FlightStatus.DELAYED));
    }

    @Test
    void updateFlightStatus_failure_executeUpdateReturnsZero_returnsFalse() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(flightDAO.updateFlightStatus(testFlight1.getId(), FlightStatus.CANCELLED));
        assertTrue(listAppender.containsMessage(Level.WARN, "Рейс з ID " + testFlight1.getId() + " не знайдено або статус не було оновлено."));
    }

    @Test
    void updateFlightStatus_failure_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Status Update Error"));

        SQLException exception = assertThrows(SQLException.class, () -> flightDAO.updateFlightStatus(testFlight1.getId(), FlightStatus.CANCELLED));
        assertEquals("DB Status Update Error", exception.getMessage());
    }

    // --- getOccupiedSeatsCount ---
    @Test
    void getOccupiedSeatsCount_success_returnsCount() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(15);

        assertEquals(15, flightDAO.getOccupiedSeatsCount(testFlight1.getId()));
        assertTrue(listAppender.containsMessage(Level.INFO, "Кількість зайнятих місць для рейсу ID " + testFlight1.getId() + ": 15"));
        verify(mockPreparedStatement).setLong(1, testFlight1.getId());
    }

    @Test
    void getOccupiedSeatsCount_success_noOccupiedSeats_returnsZero() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(0);

        assertEquals(0, flightDAO.getOccupiedSeatsCount(testFlight1.getId()));
        assertTrue(listAppender.containsMessage(Level.INFO, "Кількість зайнятих місць для рейсу ID " + testFlight1.getId() + ": 0"));
    }

    @Test
    void getOccupiedSeatsCount_failure_resultSetNextFalse_logsAndReturnsZero() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        assertEquals(0, flightDAO.getOccupiedSeatsCount(testFlight1.getId()));
        assertTrue(listAppender.containsMessage(Level.INFO, "Не знайдено даних про зайняті місця для рейсу ID " + testFlight1.getId() + ". Повертається 0."));
    }

    @Test
    void getOccupiedSeatsCount_failure_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Count Error"));

        SQLException exception = assertThrows(SQLException.class, () -> flightDAO.getOccupiedSeatsCount(testFlight1.getId()));
        assertEquals("DB Count Error", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при отриманні кількості зайнятих місць для рейсу ID " + testFlight1.getId()));
    }


    @Test
    void getFlightById_success_flightNotFound_returnsEmptyOptional() throws SQLException {
        long nonExistentFlightId = 999L;
        when(mockConnection.prepareStatement(argThat(sql -> sql.toLowerCase().contains("from flights where id = ?"))))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<Flight> result = flightDAO.getFlightById(nonExistentFlightId);

        assertFalse(result.isPresent());
        assertTrue(listAppender.containsMessage(Level.INFO, "Рейс з ID " + nonExistentFlightId + " не знайдено."));
        verify(mockPreparedStatement).setLong(1, nonExistentFlightId);
        verify(mockRouteDAO, never()).getRouteById(anyLong()); // mockRouteDAO не повинен викликатися, якщо рейс не знайдено
    }


    @Test
    void getFlightsByDate_success_noFlightsOnDate_returnsEmptyList() throws SQLException {
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Flight> flights = flightDAO.getFlightsByDate(date);

        assertNotNull(flights);
        assertTrue(flights.isEmpty());
        assertTrue(listAppender.containsMessage(Level.INFO, "Успішно отримано 0 рейсів на дату " + date + "."));
        verify(mockPreparedStatement).setDate(1, java.sql.Date.valueOf(date));
        verify(mockRouteDAO, never()).getRouteById(anyLong());
    }

    @Test
    void getFlightsByDate_failure_sqlExceptionOnQuery_throwsSQLException() throws SQLException {
        LocalDate date = LocalDate.of(2024, 1, 10);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Date Query Error"));

        SQLException exception = assertThrows(SQLException.class, () -> flightDAO.getFlightsByDate(date));
        assertEquals("DB Date Query Error", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при отриманні рейсів на дату " + date));
        assertTrue(listAppender.containsMessage(Level.ERROR, "DB Date Query Error"));
    }
}