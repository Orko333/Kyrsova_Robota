package DAO;

import DB.DatabaseConnectionManager;
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

import java.sql.*;
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
class RouteDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatementRoutes; // Для запитів до routes
    @Mock
    private PreparedStatement mockPreparedStatementIntermediateStops; // Для запитів до route_intermediate_stops
    @Mock
    private Statement mockStatement; // Для getAllRoutes основного запиту
    @Mock
    private ResultSet mockResultSetRoutes; // Для результатів з таблиці routes
    @Mock
    private ResultSet mockResultSetIntermediateStops; // Для результатів з таблиці route_intermediate_stops

    @Mock // Мокуємо залежність StopDAO
    private StopDAO mockStopDAO;

    @InjectMocks // Інжектуємо мок StopDAO в RouteDAO
    private RouteDAO routeDAO;

    private static ListAppender listAppender;
    private static org.apache.logging.log4j.core.Logger insuranceLogger;
    private static MockedStatic<DatabaseConnectionManager> mockedDbManager;

    // Test Data
    private Stop stop1, stop2, stop3, stop4, stop5;
    private Route testRoute1, testRoute2;

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
        listAppender = new ListAppender("TestRouteDAOAppender");
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

        // Спільна поведінка для закриття ресурсів
        lenient().doNothing().when(mockResultSetRoutes).close();
        lenient().doNothing().when(mockResultSetIntermediateStops).close();
        lenient().doNothing().when(mockPreparedStatementRoutes).close();
        lenient().doNothing().when(mockPreparedStatementIntermediateStops).close();
        lenient().doNothing().when(mockStatement).close();
        lenient().doNothing().when(mockConnection).close();

        // Ініціалізація тестових зупинок
        stop1 = new Stop(1L, "Київ", "АС Київ");
        stop2 = new Stop(2L, "Житомир", "АС Житомир");
        stop3 = new Stop(3L, "Рівне", "АС Рівне");
        stop4 = new Stop(4L, "Львів", "АС Львів");
        stop5 = new Stop(5L, "Одеса", "АС Одеса");

        // Ініціалізація тестових маршрутів (без проміжних на цьому етапі, вони будуть мокуватися)
        testRoute1 = new Route(101L, stop1, stop4, List.of(stop2, stop3)); // Київ -> Житомир -> Рівне -> Львів
        testRoute2 = new Route(102L, stop1, stop5, Collections.emptyList()); // Київ -> Одеса
    }

    @AfterEach
    void tearDown() {
        try {
            verify(mockConnection, atLeast(0)).close();
            verify(mockPreparedStatementRoutes, atLeast(0)).close();
            verify(mockPreparedStatementIntermediateStops, atLeast(0)).close();
            verify(mockStatement, atLeast(0)).close();
            verify(mockResultSetRoutes, atLeast(0)).close();
            verify(mockResultSetIntermediateStops, atLeast(0)).close();
        } catch (SQLException e) {
            // Це не повинно відбуватися
        }
        reset(mockConnection, mockPreparedStatementRoutes, mockPreparedStatementIntermediateStops,
                mockStatement, mockResultSetRoutes, mockResultSetIntermediateStops, mockStopDAO);
    }



    @Test
    void getRouteById_routeNotFound_returnsEmptyOptional() throws SQLException {
        long nonExistentRouteId = 999L;
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatementRoutes);
        when(mockPreparedStatementRoutes.executeQuery()).thenReturn(mockResultSetRoutes);
        when(mockResultSetRoutes.next()).thenReturn(false); // Маршрут не знайдено

        Optional<Route> result = routeDAO.getRouteById(nonExistentRouteId);

        assertFalse(result.isPresent());
        assertTrue(listAppender.containsMessage(Level.INFO, "Маршрут з ID " + nonExistentRouteId + " не знайдено."));
        verify(mockStopDAO, never()).getStopById(anyLong());
        verify(mockConnection, never()).prepareStatement(argThat(sql -> sql.contains("route_intermediate_stops")));
    }

    @Test
    void getRouteById_departureStopNotFound_throwsSQLException() throws SQLException {
        long routeId = testRoute1.getId();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatementRoutes);
        when(mockPreparedStatementRoutes.executeQuery()).thenReturn(mockResultSetRoutes);
        when(mockResultSetRoutes.next()).thenReturn(true).thenReturn(false);
        when(mockResultSetRoutes.getLong("id")).thenReturn(routeId);
        when(mockResultSetRoutes.getLong("departure_stop_id")).thenReturn(stop1.getId());
        when(mockResultSetRoutes.getLong("destination_stop_id")).thenReturn(stop4.getId());

        when(mockStopDAO.getStopById(stop1.getId())).thenReturn(Optional.empty()); // Зупинка відправлення не знайдена
        when(mockStopDAO.getStopById(stop4.getId())).thenReturn(Optional.of(stop4)); // Зупинка призначення знайдена (для повноти)

        SQLException exception = assertThrows(SQLException.class, () -> routeDAO.getRouteById(routeId));
        assertTrue(exception.getMessage().contains("Зупинка відправлення ID " + stop1.getId() + " не знайдена для маршруту ID: " + routeId));
        assertTrue(listAppender.containsMessage(Level.ERROR, "Зупинка відправлення ID " + stop1.getId() + " не знайдена для маршруту ID: " + routeId));
    }

    @Test
    void getRouteById_destinationStopNotFound_throwsSQLException() throws SQLException {
        long routeId = testRoute1.getId();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatementRoutes);
        when(mockPreparedStatementRoutes.executeQuery()).thenReturn(mockResultSetRoutes);
        when(mockResultSetRoutes.next()).thenReturn(true).thenReturn(false);
        when(mockResultSetRoutes.getLong("id")).thenReturn(routeId);
        when(mockResultSetRoutes.getLong("departure_stop_id")).thenReturn(stop1.getId());
        when(mockResultSetRoutes.getLong("destination_stop_id")).thenReturn(stop4.getId());

        when(mockStopDAO.getStopById(stop1.getId())).thenReturn(Optional.of(stop1));
        when(mockStopDAO.getStopById(stop4.getId())).thenReturn(Optional.empty()); // Зупинка призначення не знайдена

        SQLException exception = assertThrows(SQLException.class, () -> routeDAO.getRouteById(routeId));
        }

    @Test
    void getRouteById_sqlExceptionDuringMainQuery_throwsSQLException() throws SQLException {
        long routeId = 1L;
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Main Route Error"));

        SQLException exception = assertThrows(SQLException.class, () -> routeDAO.getRouteById(routeId));
        assertEquals("DB Main Route Error", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при отриманні маршруту за ID " + routeId));
    }

    @Test
    void getAllRoutes_success_noRoutes_returnsEmptyList() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSetRoutes);
        when(mockResultSetRoutes.next()).thenReturn(false); // Немає маршрутів

        List<Route> routes = routeDAO.getAllRoutes();

        assertTrue(routes.isEmpty());
        assertTrue(listAppender.containsMessage(Level.INFO, "Успішно отримано 0 маршрутів."));
    }

    // Додаткові тести на винятки для getAllRoutes (наприклад, StopDAO кидає помилку, SQL помилка)
    // схожі на ті, що для getRouteById.
}