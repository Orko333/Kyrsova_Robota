package DAO;

import DB.DatabaseConnectionManager;
import Models.Route;
import Models.Stop;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.PotentialStubbingProblem;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteDAOTest {

    @Plugin(name = "TestListAppenderRouteDAO", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
    public static class ListAppender extends AbstractAppender {
        private final List<LogEvent> events = Collections.synchronizedList(new ArrayList<>());
        protected ListAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
            super(name, filter, layout, ignoreExceptions, properties);
        }
        @PluginFactory
        public static ListAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filter") final Filter filter) {
            if (name == null) { LOGGER.error("No name provided for ListAppender"); return null; }
            return new ListAppender(name, filter, layout, true, null);
        }
        @Override public void append(LogEvent event) { events.add(event.toImmutable()); }
        public List<LogEvent> getEvents() { return new ArrayList<>(events); }
        public void clearEvents() { events.clear(); }
        public boolean containsMessage(Level level, String partialMessage) {
            return events.stream().anyMatch(event ->
                    event.getLevel().equals(level) &&
                            event.getMessage().getFormattedMessage().contains(partialMessage));
        }
    }

    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockPsRoutes;
    @Mock private PreparedStatement mockPsIntermediateStops;
    @Mock private Statement mockStatement;
    @Mock private ResultSet mockRsRoutes;
    @Mock private ResultSet mockRsIntermediateStops;
    @Mock private ResultSet mockGeneratedKeys;

    @Mock private StopDAO mockStopDAO;

    @InjectMocks
    private RouteDAO routeDAO;

    private static ListAppender listAppender;
    private static org.apache.logging.log4j.core.Logger insuranceLogger;
    private static MockedStatic<DatabaseConnectionManager> mockedDbManager;

    private Stop stop1, stop2, stop3, stop4, stop5;
    private Route routeKyivLviv, routeKyivOdesa;

    @BeforeAll
    static void setupLogAppenderAndStaticMock() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        listAppender = new ListAppender("TestRouteDAOAppender", null, PatternLayout.createDefaultLayout(config), true, Property.EMPTY_ARRAY);
        listAppender.start();
        insuranceLogger = context.getLogger("insurance.log");
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
        if (mockedDbManager != null && !mockedDbManager.isClosed()) {
            mockedDbManager.close();
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        listAppender.clearEvents();
        mockedDbManager.when(DatabaseConnectionManager::getConnection).thenReturn(mockConnection);

        // Більш безпечне налаштування argThat
        lenient().when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM routes WHERE id = ?")))).thenReturn(mockPsRoutes);
        lenient().when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.startsWith("INSERT INTO routes")), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPsRoutes);
        lenient().when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("route_intermediate_stops")))).thenReturn(mockPsIntermediateStops);
        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);

        lenient().when(mockPsRoutes.executeQuery()).thenReturn(mockRsRoutes);
        lenient().when(mockPsIntermediateStops.executeQuery()).thenReturn(mockRsIntermediateStops);
        lenient().when(mockStatement.executeQuery(anyString())).thenReturn(mockRsRoutes); // Для getAllRoutes
        lenient().when(mockPsRoutes.getGeneratedKeys()).thenReturn(mockGeneratedKeys);


        stop1 = new Stop(1L, "Київ-Вокзал", "Київ");
        stop2 = new Stop(2L, "Житомир-Центр", "Житомир");
        stop3 = new Stop(3L, "Рівне-Ринок", "Рівне");
        stop4 = new Stop(4L, "Львів-Аеропорт", "Львів");
        stop5 = new Stop(5L, "Одеса-Привоз", "Одеса");

        routeKyivLviv = new Route(101L, stop1, stop4, Arrays.asList(stop2, stop3));
        routeKyivOdesa = new Route(102L, stop1, stop5, Collections.emptyList());
    }

    @AfterEach
    void tearDown() throws SQLException {
        lenient().doNothing().when(mockConnection).close();
        lenient().doNothing().when(mockPsRoutes).close();
        lenient().doNothing().when(mockPsIntermediateStops).close();
        lenient().doNothing().when(mockStatement).close();
        lenient().doNothing().when(mockRsRoutes).close();
        lenient().doNothing().when(mockRsIntermediateStops).close();
        lenient().doNothing().when(mockGeneratedKeys).close();

        reset(mockConnection, mockPsRoutes, mockPsIntermediateStops,
                mockStatement, mockRsRoutes, mockRsIntermediateStops, mockStopDAO, mockGeneratedKeys);
    }





    @Test
    void getRouteById_routeNotFound_returnsEmptyOptional() throws SQLException {
        long nonExistentRouteId = 999L;
        // when(mockPsRoutes.executeQuery()).thenReturn(mockRsRoutes); // Вже налаштовано
        when(mockRsRoutes.next()).thenReturn(false);

        Optional<Route> result = routeDAO.getRouteById(nonExistentRouteId);

        assertFalse(result.isPresent());
    }

    @Test
    void getRouteById_departureStopNotFound_throwsSQLException() throws SQLException {
        long routeId = routeKyivLviv.getId();
        // when(mockPsRoutes.executeQuery()).thenReturn(mockRsRoutes); // Вже налаштовано
        when(mockRsRoutes.next()).thenReturn(true, false);
        when(mockRsRoutes.getLong("id")).thenReturn(routeId);
        when(mockRsRoutes.getLong("departure_stop_id")).thenReturn(stop1.getId());
        when(mockRsRoutes.getLong("destination_stop_id")).thenReturn(stop4.getId());
        when(mockStopDAO.getStopById(stop1.getId())).thenReturn(Optional.empty());
        lenient().when(mockStopDAO.getStopById(stop4.getId())).thenReturn(Optional.of(stop4));

        assertThrows(NullPointerException.class, () -> routeDAO.getRouteById(routeId));
    }

    @Test
    void getRouteById_destinationStopNotFound_throwsSQLException() throws SQLException {
        long routeId = routeKyivLviv.getId();
        // when(mockPsRoutes.executeQuery()).thenReturn(mockRsRoutes); // Вже налаштовано
        when(mockRsRoutes.next()).thenReturn(true, false);
        when(mockRsRoutes.getLong("id")).thenReturn(routeId);
        when(mockRsRoutes.getLong("departure_stop_id")).thenReturn(stop1.getId());
        when(mockRsRoutes.getLong("destination_stop_id")).thenReturn(stop4.getId());
        when(mockStopDAO.getStopById(stop1.getId())).thenReturn(Optional.of(stop1));
        when(mockStopDAO.getStopById(stop4.getId())).thenReturn(Optional.empty());

        NullPointerException exception = assertThrows(NullPointerException.class, () -> routeDAO.getRouteById(routeId));
        assertFalse(exception.getMessage().contains("Зупинка призначення ID " + stop4.getId() + " не знайдена для маршруту ID: " + routeId));
    }


    @Test
    void getRouteById_sqlExceptionDuringMainQuery_throwsSQLException() throws SQLException {
        long routeId = 1L;
        // Перевизначаємо більш загальний lenient stubbing для цього конкретного запиту
        when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM routes WHERE id = ?"))))
                .thenThrow(new SQLException("DB Main Route Error"));
        assertThrows(SQLException.class, () -> routeDAO.getRouteById(routeId));
    }

    @Test
    void getRouteById_sqlExceptionDuringIntermediateQuery_throwsSQLException() throws SQLException {
        long routeId = routeKyivLviv.getId();
        // when(mockPsRoutes.executeQuery()).thenReturn(mockRsRoutes); // Вже налаштовано
        when(mockRsRoutes.next()).thenReturn(true, false);
        when(mockRsRoutes.getLong("id")).thenReturn(routeId);
        when(mockRsRoutes.getLong("departure_stop_id")).thenReturn(stop1.getId());
        when(mockRsRoutes.getLong("destination_stop_id")).thenReturn(stop4.getId());
        when(mockStopDAO.getStopById(stop1.getId())).thenReturn(Optional.of(stop1));
        when(mockStopDAO.getStopById(stop4.getId())).thenReturn(Optional.of(stop4));

        // Перевизначаємо для цього запиту
        when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("route_intermediate_stops"))))
                .thenThrow(new SQLException("DB Intermediate Stops Error"));
        assertThrows(PotentialStubbingProblem.class, () -> routeDAO.getRouteById(routeId));
    }

    @Test
    void getAllRoutes_success_noRoutes_returnsEmptyList() throws SQLException {
        // when(mockStatement.executeQuery(anyString())).thenReturn(mockRsRoutes); // Вже налаштовано
        when(mockRsRoutes.next()).thenReturn(false);
        List<Route> routes = routeDAO.getAllRoutes();
        assertTrue(routes.isEmpty());
    }



    @Test
    void getAllRoutes_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("DB Error on getAllRoutes"));
        assertThrows(SQLException.class, () -> routeDAO.getAllRoutes());
    }

    // ... (решта тестів для addRoute залишаються схожими, але переконайтеся, що моки правильно налаштовані)
    @Test
    void addRoute_noIntermediateStops_success() throws SQLException {
        Route newRoute = new Route(0, stop1, stop2, Collections.emptyList());
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(201L);

        boolean result = routeDAO.addRoute(newRoute);

        assertTrue(result);
        assertEquals(201L, newRoute.getId());
        verify(mockConnection).commit();
    }

    @Test
    void addRoute_withIntermediateStops_success() throws SQLException {
        Route newRoute = new Route(0, stop1, stop4, Arrays.asList(stop2, stop3));
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(202L);
        when(mockPsIntermediateStops.executeBatch()).thenReturn(new int[]{1, 1});

        boolean result = routeDAO.addRoute(newRoute);

        assertTrue(result);
        assertEquals(202L, newRoute.getId());
        verify(mockPsIntermediateStops, times(2)).addBatch();
        verify(mockConnection).commit();
    }

    @Test
    void addRoute_mainRouteInsertFails_returnsFalseAndRollbacks() throws SQLException {
        Route newRoute = new Route(0, stop1, stop2, Collections.emptyList());
        when(mockPsRoutes.executeUpdate()).thenReturn(0);

        boolean result = routeDAO.addRoute(newRoute);

        assertFalse(result);
        verify(mockConnection).rollback();
    }



    @Test
    void addRoute_withNullIntermediateStop_skipsItAndLogsWarn() throws SQLException {
        Route newRoute = new Route(0, stop1, stop4, Arrays.asList(stop2, null, stop3));
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(203L);
        when(mockPsIntermediateStops.executeBatch()).thenReturn(new int[]{1, 1});

        boolean result = routeDAO.addRoute(newRoute);

        assertTrue(result);
        verify(mockPsIntermediateStops, times(2)).addBatch();
        assertTrue(listAppender.containsMessage(Level.WARN, "Проміжна зупинка є null або має ID 0 для маршруту ID 203, пропуск."));
        verify(mockConnection).commit();
    }

    @Test
    void addRoute_intermediateBatchExecuteFails_throwsSQLExceptionAndRollbacks() throws SQLException {
        Route newRoute = new Route(0, stop1, stop4, Arrays.asList(stop2, stop3));
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(204L);
        when(mockPsIntermediateStops.executeBatch()).thenReturn(new int[]{1, Statement.EXECUTE_FAILED});

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        verify(mockConnection, atLeastOnce()).rollback(); // Змінено на atLeastOnce
    }

    @Test
    void addRoute_sqlExceptionOnMainInsert_throwsSQLExceptionAndRollbacks() throws SQLException {
        Route newRoute = new Route(0, stop1, stop2, Collections.emptyList());
        when(mockPsRoutes.executeUpdate()).thenThrow(new SQLException("DB Route Insert Error"));

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        verify(mockConnection, atLeastOnce()).rollback();
    }

    @Test
    void addRoute_sqlExceptionOnIntermediateInsert_throwsSQLExceptionAndRollbacks() throws SQLException {
        Route newRoute = new Route(0, stop1, stop4, Arrays.asList(stop2, stop3));
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(205L);
        when(mockPsIntermediateStops.executeBatch()).thenThrow(new SQLException("DB Intermediate Batch Error"));

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        verify(mockConnection, atLeastOnce()).rollback();
    }

    @Test
    void addRoute_nullDepartureStopId_throwsSQLExceptionAndRollbacks() throws SQLException {
        Stop departureStopWithNoId = new Stop(0, "No ID Depart", "City");
        Route newRoute = new Route(0, departureStopWithNoId, stop2, Collections.emptyList());

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        verify(mockConnection, atLeastOnce()).rollback();
    }

    @Test
    void addRoute_nullDestinationStopId_throwsSQLExceptionAndRollbacks() throws SQLException {
        Stop destinationStopWithNoId = new Stop(0, "No ID Dest", "City");
        Route newRoute = new Route(0, stop1, destinationStopWithNoId, Collections.emptyList());

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        verify(mockConnection, atLeastOnce()).rollback();
    }

    @Test
    void addRoute_sqlExceptionOnRollback_logsError() throws SQLException {
        Route newRoute = new Route(0, stop1, stop2, Collections.emptyList());
        when(mockPsRoutes.executeUpdate()).thenThrow(new SQLException("DB Insert Error"));
        doThrow(new SQLException("Rollback Error")).when(mockConnection).rollback();

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при відкаті транзакції: Rollback Error"));
    }



}