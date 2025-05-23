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
import org.junit.jupiter.api.DisplayName; // Додано
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.misusing.PotentialStubbingProblem;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException; // Додано
import java.lang.reflect.Method; // Додано
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
    @Mock private PreparedStatement mockPsRoutes; // Для основного запиту маршрутів
    @Mock private PreparedStatement mockPsIntermediateStops; // Для запиту проміжних зупинок
    @Mock private Statement mockStatement;
    @Mock private ResultSet mockRsRoutes; // Для основного запиту маршрутів
    @Mock private ResultSet mockRsIntermediateStops; // Для запиту проміжних зупинок
    @Mock private ResultSet mockGeneratedKeys;

    @Mock private StopDAO mockStopDAO; // @InjectMocks ін'єктує це в routeDAO

    @InjectMocks
    private RouteDAO routeDAO;

    private static ListAppender listAppender;
    private static org.apache.logging.log4j.core.Logger insuranceLogger;
    private static MockedStatic<DatabaseConnectionManager> mockedDbManager;

    private Stop stop1, stop2, stop3, stop4, stop5;
    private Route routeKyivLviv, routeKyivOdesa;

    // SQL-запит, що використовується в методі getIntermediateStopsForRoute
    private final String SQL_GET_INTERMEDIATE_STOPS = "SELECT stop_id FROM route_intermediate_stops WHERE route_id = ? ORDER BY stop_order";


    @BeforeAll
    static void setupLogAppenderAndStaticMock() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        listAppender = new ListAppender("TestRouteDAOAppender", null, PatternLayout.createDefaultLayout(config), true, Property.EMPTY_ARRAY);
        listAppender.start();
        insuranceLogger = context.getLogger("insurance.log"); // Назва логера така ж, як у RouteDAO
        insuranceLogger.addAppender(listAppender);
        insuranceLogger.setLevel(Level.ALL); // Перехоплювати всі рівні логування
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
        listAppender.clearEvents(); // Очищаємо логи перед кожним тестом
        mockedDbManager.when(DatabaseConnectionManager::getConnection).thenReturn(mockConnection);

        // Загальні налаштування моків, які можуть бути перевизначені в окремих тестах
        // Для getRouteById та getAllRoutes
        lenient().when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM routes WHERE id = ?")))).thenReturn(mockPsRoutes);
        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);
        lenient().when(mockPsRoutes.executeQuery()).thenReturn(mockRsRoutes);
        lenient().when(mockStatement.executeQuery(anyString())).thenReturn(mockRsRoutes);

        // Для addRoute
        lenient().when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.startsWith("INSERT INTO routes")), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPsRoutes);
        lenient().when(mockPsRoutes.getGeneratedKeys()).thenReturn(mockGeneratedKeys);

        // Для getIntermediateStopsForRoute (також використовується в getRouteById, getAllRoutes)
        // Переконайтеся, що цей мок не конфліктує з іншими, якщо використовується та ж змінна mockPsIntermediateStops
        // Якщо SQL_GET_INTERMEDIATE_STOPS унікальний, то eq(SQL_GET_INTERMEDIATE_STOPS) безпечніше
        lenient().when(mockConnection.prepareStatement(eq(SQL_GET_INTERMEDIATE_STOPS))).thenReturn(mockPsIntermediateStops);
        // Альтернатива, якщо SQL_GET_INTERMEDIATE_STOPS не унікальний, але інші запити до route_intermediate_stops є:
        // lenient().when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("route_intermediate_stops") && sql.contains("ORDER BY stop_order")))).thenReturn(mockPsIntermediateStops);
        lenient().when(mockPsIntermediateStops.executeQuery()).thenReturn(mockRsIntermediateStops);


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
        // Закриття ресурсів тут не потрібне для моків, Mockito сам їх обробляє.
        // Скидання стану моків, щоб уникнути взаємного впливу тестів
        reset(mockConnection, mockPsRoutes, mockPsIntermediateStops,
                mockStatement, mockRsRoutes, mockRsIntermediateStops, mockStopDAO, mockGeneratedKeys);
    }

    // ===================================================================================
    // ТЕСТИ ДЛЯ getIntermediateStopsForRoute (інтегровані)
    // ===================================================================================

    // Допоміжний метод для виклику приватного методу getIntermediateStopsForRoute
    @SuppressWarnings("unchecked")
    private List<Stop> invokeGetIntermediateStopsForRoute(Connection conn, long routeId) throws Exception {
        Method method = RouteDAO.class.getDeclaredMethod("getIntermediateStopsForRoute", Connection.class, long.class);
        method.setAccessible(true);
        try {
            return (List<Stop>) method.invoke(routeDAO, conn, routeId);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) throw (SQLException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause != null) throw new RuntimeException("InvocationTargetException with unexpected cause: " + cause.getClass().getName(), cause);
            throw new RuntimeException("InvocationTargetException with null cause", e);
        }
    }


    @Test
    @DisplayName("[GISFR] Повинен повертати порожній список, якщо немає проміжних зупинок")
    void getIntermediateStopsForRoute_shouldReturnEmptyList_whenNoStopsExist() throws Exception {
        long routeId = 2L;
        when(mockRsIntermediateStops.next()).thenReturn(false);

        List<Stop> result = invokeGetIntermediateStopsForRoute(mockConnection, routeId);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Список зупинок має бути порожнім");
        verify(mockStopDAO, never()).getStopById(anyLong());
        verify(mockPsIntermediateStops).close();
        verify(mockRsIntermediateStops).close();
    }


    @Test
    @DisplayName("[GISFR] Повинен кидати SQLException, якщо prepareStatement для проміжних зупинок кидає виняток")
    void getIntermediateStopsForRoute_shouldThrowSQLException_whenPrepareStatementFails() throws SQLException {
        long routeId = 5L;
        SQLException expectedException = new SQLException("Помилка підготовки PreparedStatement для проміжних");
        // Перевизначаємо мок для цього тесту
        when(mockConnection.prepareStatement(eq(SQL_GET_INTERMEDIATE_STOPS))).thenThrow(expectedException);

        SQLException actualException = assertThrows(SQLException.class,
                () -> invokeGetIntermediateStopsForRoute(mockConnection, routeId));
        assertEquals(expectedException.getMessage(), actualException.getMessage());
        // PreparedStatement не був створений, тому close не викликається
        verify(mockPsIntermediateStops, never()).close();
        verify(mockRsIntermediateStops, never()).close();
    }

    @Test
    @DisplayName("[GISFR] Повинен кидати SQLException, якщо executeQuery для проміжних зупинок кидає виняток")
    void getIntermediateStopsForRoute_shouldThrowSQLException_whenExecuteQueryFails() throws SQLException {
        long routeId = 6L;
        SQLException expectedException = new SQLException("Помилка виконання запиту для проміжних");
        when(mockPsIntermediateStops.executeQuery()).thenThrow(expectedException);

        SQLException actualException = assertThrows(SQLException.class,
                () -> invokeGetIntermediateStopsForRoute(mockConnection, routeId));
        assertEquals(expectedException.getMessage(), actualException.getMessage());
        verify(mockPsIntermediateStops).setLong(1, routeId);
        verify(mockPsIntermediateStops).close(); // Закривається через try-with-resources
        verify(mockRsIntermediateStops, never()).close(); // ResultSet не був створений
    }

    @Test
    @DisplayName("[GISFR] Повинен кидати SQLException, якщо rs.next для проміжних зупинок кидає виняток")
    void getIntermediateStopsForRoute_shouldThrowSQLException_whenResultSetNextFails() throws SQLException {
        long routeId = 7L;
        SQLException expectedException = new SQLException("Помилка ResultSet.next() для проміжних");
        when(mockRsIntermediateStops.next()).thenThrow(expectedException);

        SQLException actualException = assertThrows(SQLException.class,
                () -> invokeGetIntermediateStopsForRoute(mockConnection, routeId));
        assertEquals(expectedException.getMessage(), actualException.getMessage());
        verify(mockPsIntermediateStops).close();
        verify(mockRsIntermediateStops).close();
    }

    @Test
    @DisplayName("[GISFR] Повинен кидати SQLException, якщо rs.getLong для проміжних зупинок кидає виняток")
    void getIntermediateStopsForRoute_shouldThrowSQLException_whenResultSetGetLongFails() throws SQLException {
        long routeId = 8L;
        SQLException expectedException = new SQLException("Помилка ResultSet.getLong() для проміжних");
        when(mockRsIntermediateStops.next()).thenReturn(true);
        when(mockRsIntermediateStops.getLong("stop_id")).thenThrow(expectedException);

        SQLException actualException = assertThrows(SQLException.class,
                () -> invokeGetIntermediateStopsForRoute(mockConnection, routeId));
        assertEquals(expectedException.getMessage(), actualException.getMessage());
        verify(mockPsIntermediateStops).close();
        verify(mockRsIntermediateStops).close();
    }


    // ===================================================================================
    // ІСНУЮЧІ ТЕСТИ (getRouteById, getAllRoutes, addRoute)
    // Переконайтеся, що вони не конфліктують з новими моками, особливо
    // коли mockConnection.prepareStatement викликається для route_intermediate_stops
    // ===================================================================================

    // Приклад існуючого тесту, який може потребувати уваги:


    @Test
    void getRouteById_routeNotFound_returnsEmptyOptional() throws SQLException {
        long nonExistentRouteId = 999L;
        when(mockRsRoutes.next()).thenReturn(false); // Основний запит не знаходить маршрут

        Optional<Route> result = routeDAO.getRouteById(nonExistentRouteId);

        assertFalse(result.isPresent());
        // getIntermediateStopsForRoute не має викликатися, якщо основний маршрут не знайдено
        verify(mockConnection, never()).prepareStatement(eq(SQL_GET_INTERMEDIATE_STOPS));
    }




    @Test
    void getRouteById_sqlExceptionDuringMainQuery_throwsSQLException() throws SQLException {
        long routeId = 1L;
        when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM routes WHERE id = ?"))))
                .thenThrow(new SQLException("DB Main Route Error"));
        assertThrows(SQLException.class, () -> routeDAO.getRouteById(routeId));
    }



    @Test
    void getAllRoutes_success_noRoutes_returnsEmptyList() throws SQLException {
        when(mockRsRoutes.next()).thenReturn(false); // Немає маршрутів
        List<Route> routes = routeDAO.getAllRoutes();
        assertTrue(routes.isEmpty());
        verify(mockConnection, never()).prepareStatement(eq(SQL_GET_INTERMEDIATE_STOPS));
    }



    @Test
    void getAllRoutes_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("DB Error on getAllRoutes"));
        assertThrows(SQLException.class, () -> routeDAO.getAllRoutes());
    }

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
        // Переконайтеся, що mockPsIntermediateStops.addBatch() не викликався
        verify(mockPsIntermediateStops, never()).addBatch();
    }

    @Test
    void addRoute_withIntermediateStops_success() throws SQLException {
        Route newRoute = new Route(0, stop1, stop4, Arrays.asList(stop2, stop3));
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(202L);
        // Налаштовуємо mockConnection.prepareStatement для запиту вставки проміжних зупинок
        PreparedStatement mockPsInsertIntermediate = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(argThat(sql -> sql.contains("INSERT INTO route_intermediate_stops")))).thenReturn(mockPsInsertIntermediate);
        when(mockPsInsertIntermediate.executeBatch()).thenReturn(new int[]{1, 1});


        boolean result = routeDAO.addRoute(newRoute);

        assertTrue(result);
        assertEquals(202L, newRoute.getId());
        verify(mockPsInsertIntermediate, times(2)).addBatch();
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

        PreparedStatement mockPsInsertIntermediate = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(argThat(sql -> sql.contains("INSERT INTO route_intermediate_stops")))).thenReturn(mockPsInsertIntermediate);
        when(mockPsInsertIntermediate.executeBatch()).thenReturn(new int[]{1, 1});


        boolean result = routeDAO.addRoute(newRoute);

        assertTrue(result);
        verify(mockPsInsertIntermediate, times(2)).addBatch(); // null зупинка пропускається
        assertTrue(listAppender.containsMessage(Level.WARN, "Проміжна зупинка є null або має ID 0 для маршруту ID 203, пропуск."));
        verify(mockConnection).commit();
    }

    @Test
    void addRoute_intermediateBatchExecuteFails_throwsSQLExceptionAndRollbacks() throws SQLException {
        Route newRoute = new Route(0, stop1, stop4, Arrays.asList(stop2, stop3));
        when(mockPsRoutes.executeUpdate()).thenReturn(1);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(204L);

        PreparedStatement mockPsInsertIntermediate = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(argThat(sql -> sql.contains("INSERT INTO route_intermediate_stops")))).thenReturn(mockPsInsertIntermediate);
        when(mockPsInsertIntermediate.executeBatch()).thenReturn(new int[]{1, Statement.EXECUTE_FAILED});

        assertThrows(SQLException.class, () -> routeDAO.addRoute(newRoute));
        verify(mockConnection, atLeastOnce()).rollback();
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

        PreparedStatement mockPsInsertIntermediate = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(argThat(sql -> sql.contains("INSERT INTO route_intermediate_stops")))).thenReturn(mockPsInsertIntermediate);
        when(mockPsInsertIntermediate.executeBatch()).thenThrow(new SQLException("DB Intermediate Batch Error"));


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