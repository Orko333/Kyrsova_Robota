package DAO;

import DB.DatabaseConnectionManager;
import Models.*;
import Models.Enums.BenefitType;
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;

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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private Statement mockStatement;
    @Mock
    private ResultSet mockResultSet;

    // Мокуємо залежності DAO
    @Mock
    private FlightDAO mockFlightDAO;
    @Mock
    private PassengerDAO mockPassengerDAO;
    @Mock
    private RouteDAO mockRouteDAO;

    @InjectMocks // Інжектуємо моки в TicketDAO
    private TicketDAO ticketDAO;

    private static ListAppender listAppender;
    private static org.apache.logging.log4j.core.Logger insuranceLogger;
    private static MockedStatic<DatabaseConnectionManager> mockedDbManager;

    // Test Data
    private Flight testFlight;
    private Passenger testPassenger;
    private Route testRoute;
    private Stop departureStop, destinationStop;
    private Ticket testTicket1;
    private Ticket testTicket2;


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
        listAppender = new ListAppender("TestTicketDAOAppender");
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

        departureStop = new Stop(1L, "Київ", "АС Київ");
        destinationStop = new Stop(2L, "Львів", "АС Львів");
        testRoute = new Route(10L, departureStop, destinationStop, Collections.emptyList());
        testFlight = new Flight(100L, testRoute, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(5),
                50, FlightStatus.PLANNED, "Богдан А092", BigDecimal.valueOf(250));
        testPassenger = new Passenger(1000L, "Тест Пасажирович", "АА123456", "Паспорт", "0501112233", "test@p.com", BenefitType.NONE);

        testTicket1 = new Ticket(1L, testFlight, testPassenger, "A1", LocalDateTime.now().minusHours(1),
                BigDecimal.valueOf(250), TicketStatus.BOOKED);
        testTicket1.setBookingExpiryDateTime(LocalDateTime.now().plusHours(23));

        testTicket2 = new Ticket(2L, testFlight, testPassenger, "B2", LocalDateTime.now().minusDays(1),
                BigDecimal.valueOf(200), TicketStatus.SOLD);
        testTicket2.setPurchaseDateTime(LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        try {
            verify(mockConnection, atLeast(0)).close();
            verify(mockPreparedStatement, atLeast(0)).close();
            verify(mockStatement, atLeast(0)).close();
            verify(mockResultSet, atLeast(0)).close();
        } catch (SQLException e) { /* ignore */ }
        reset(mockConnection, mockPreparedStatement, mockStatement, mockResultSet,
                mockFlightDAO, mockPassengerDAO, mockRouteDAO);
    }

    // --- getOccupiedSeatsForFlight ---
    @Test
    void getOccupiedSeatsForFlight_success_returnsSeats() throws SQLException {
        long flightId = testFlight.getId();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("seat_number")).thenReturn("A1").thenReturn("B2");

        List<String> seats = ticketDAO.getOccupiedSeatsForFlight(flightId);

        assertNotNull(seats);
        assertEquals(2, seats.size());
        assertTrue(seats.contains("A1"));
        assertTrue(seats.contains("B2"));
        assertTrue(listAppender.containsMessage(Level.INFO, "Знайдено 2 зайнятих місць для рейсу ID: " + flightId));
        verify(mockPreparedStatement).setLong(1, flightId);
    }

    @Test
    void getOccupiedSeatsForFlight_noSeats_returnsEmptyList() throws SQLException {
        long flightId = testFlight.getId();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<String> seats = ticketDAO.getOccupiedSeatsForFlight(flightId);

        assertNotNull(seats);
        assertTrue(seats.isEmpty());
        assertTrue(listAppender.containsMessage(Level.INFO, "Знайдено 0 зайнятих місць для рейсу ID: " + flightId));
    }

    @Test
    void getOccupiedSeatsForFlight_sqlException_throwsSQLException() throws SQLException {
        long flightId = testFlight.getId();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Error Seats"));

        SQLException ex = assertThrows(SQLException.class, () -> ticketDAO.getOccupiedSeatsForFlight(flightId));
        assertEquals("DB Error Seats", ex.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при отриманні зайнятих місць для рейсу ID " + flightId));
    }


    // --- addTicket ---
    @Test
    void addTicket_success_returnsTrueAndSetsId() throws SQLException {
        long generatedId = 123L;
        testTicket1.setId(0L); // Скидаємо ID для тестування генерації

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(generatedId);

        assertTrue(ticketDAO.addTicket(testTicket1));
        assertEquals(generatedId, testTicket1.getId());
        assertTrue(listAppender.containsMessage(Level.INFO, "Квиток успішно додано. ID нового квитка: " + generatedId));
        verify(mockPreparedStatement).setLong(1, testTicket1.getFlight().getId());
        verify(mockPreparedStatement).setLong(2, testTicket1.getPassenger().getId());
        verify(mockPreparedStatement).setString(3, testTicket1.getSeatNumber());
        verify(mockPreparedStatement).setTimestamp(4, Timestamp.valueOf(testTicket1.getBookingDateTime()));
        verify(mockPreparedStatement).setTimestamp(5, Timestamp.valueOf(testTicket1.getBookingExpiryDateTime()));
        verify(mockPreparedStatement).setBigDecimal(6, testTicket1.getPricePaid());
        verify(mockPreparedStatement).setString(7, testTicket1.getStatus().name());
    }

    @Test
    void addTicket_success_nullExpiryDate_setsNullTimestamp() throws SQLException {
        long generatedId = 124L;
        testTicket1.setId(0L);
        testTicket1.setBookingExpiryDateTime(null); // Тестуємо null expiry

        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        // ... решта моків як у попередньому тесті ...
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(generatedId);

        assertTrue(ticketDAO.addTicket(testTicket1));
        verify(mockPreparedStatement).setNull(5, Types.TIMESTAMP); // Перевіряємо встановлення null
    }


    @Test
    void addTicket_failure_noGeneratedKey_returnsFalse() throws SQLException {
        testTicket1.setId(0L);
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Ключ не згенеровано

        assertFalse(ticketDAO.addTicket(testTicket1));
        assertEquals(0L, testTicket1.getId()); // ID не повинен змінитися
        assertTrue(listAppender.containsMessage(Level.WARN, "Квиток додано (1 рядків), але не вдалося отримати згенерований ID."));
    }

    @Test
    void addTicket_failure_affectedRowsZero_returnsFalse() throws SQLException {
        testTicket1.setId(0L);
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // Жоден рядок не змінено

        assertFalse(ticketDAO.addTicket(testTicket1));
        assertTrue(listAppender.containsMessage(Level.WARN, "Квиток не було додано (affectedRows = 0)."));
    }

    @Test
    void addTicket_failure_uniqueConstraintViolation_returnsFalse() throws SQLException {
        testTicket1.setId(0L);
        SQLException uniqueEx = new SQLException("Порушення uq_ticket_flight_seat", "23000"); // Або 23505
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(uniqueEx);

        assertFalse(ticketDAO.addTicket(testTicket1));
        assertTrue(listAppender.containsMessage(Level.WARN, "Помилка додавання квитка: Місце " + testTicket1.getSeatNumber() + " на рейсі " + testTicket1.getFlight().getId() + " вже зайняте."));
    }

    @Test
    void addTicket_failure_otherSqlException_throwsSQLException() throws SQLException {
        testTicket1.setId(0L);
        SQLException otherEx = new SQLException("Інша помилка SQL", "XXXXX");
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(otherEx);

        SQLException thrown = assertThrows(SQLException.class, () -> ticketDAO.addTicket(testTicket1));
        assertSame(otherEx, thrown);
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка SQL при додаванні квитка: Рейс ID=" + testTicket1.getFlight().getId()));
    }


    @Test
    void updateTicketStatus_notFound_returnsFalse() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // Квиток не знайдено

        assertFalse(ticketDAO.updateTicketStatus(testTicket1.getId(), TicketStatus.SOLD, LocalDateTime.now()));
        assertTrue(listAppender.containsMessage(Level.WARN, "Квиток з ID " + testTicket1.getId() + " не знайдено або статус не було оновлено."));
    }

    @Test
    void updateTicketStatus_sqlException_throwsSQLException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB Update Status Error"));

        SQLException ex = assertThrows(SQLException.class, () -> ticketDAO.updateTicketStatus(testTicket1.getId(), TicketStatus.SOLD, LocalDateTime.now()));
        assertEquals("DB Update Status Error", ex.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка при оновленні статусу квитка ID " + testTicket1.getId()));
    }


    @Test
    void getAllTickets_flightNotFoundForTicket_throwsSQLException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getLong("id")).thenReturn(testTicket1.getId());
        when(mockResultSet.getLong("flight_id")).thenReturn(testFlight.getId());
        // ...
        when(mockFlightDAO.getFlightById(testFlight.getId())).thenReturn(Optional.empty()); // Рейс не знайдено

        SQLException ex = assertThrows(SQLException.class, () -> ticketDAO.getAllTickets(null));
        assertTrue(ex.getMessage().contains("Рейс ID " + testFlight.getId() + " не знайдено для квитка ID: " + testTicket1.getId()));
    }


    @Test
    void getSalesByRouteForPeriod_routeForSaleNotFound_usesDefaultDescription() throws SQLException {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        long unknownRouteId = 999L;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getLong("route_id")).thenReturn(unknownRouteId);
        // ...
        when(mockRouteDAO.getRouteById(unknownRouteId)).thenReturn(Optional.empty()); // Маршрут не знайдено

        Map<String, Map<String, Object>> salesData = ticketDAO.getSalesByRouteForPeriod(startDate, endDate);

        String expectedKey = "Невідомий або видалений маршрут (ID: " + unknownRouteId + ")";
        assertTrue(salesData.containsKey(expectedKey));
        assertTrue(listAppender.containsMessage(Level.WARN, "Маршрут з ID " + unknownRouteId + " не знайдено під час генерації звіту продажів"));
    }

    // --- getTicketCountsByStatus ---
    @Test
    void getTicketCountsByStatus_success_returnsCounts() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("status")).thenReturn(TicketStatus.BOOKED.name()).thenReturn(TicketStatus.SOLD.name());
        when(mockResultSet.getInt("count")).thenReturn(5).thenReturn(10);

        Map<TicketStatus, Integer> counts = ticketDAO.getTicketCountsByStatus();

        assertNotNull(counts);
        assertEquals(Integer.valueOf(5), counts.get(TicketStatus.BOOKED));
        assertEquals(Integer.valueOf(10), counts.get(TicketStatus.SOLD));
        // Перевірка, що всі статуси присутні, навіть якщо з count = 0
        for (TicketStatus ts : TicketStatus.values()) {
            assertTrue(counts.containsKey(ts));
        }
        assertEquals(Integer.valueOf(0), counts.get(TicketStatus.CANCELLED)); // Приклад
        assertTrue(listAppender.containsMessage(Level.INFO, "Кількість квитків за статусами отримана"));
    }

    @Test
    void getTicketCountsByStatus_unknownStatusInDb_logsWarning() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("status")).thenReturn("INVALID_DB_STATUS");
        when(mockResultSet.getInt("count")).thenReturn(3);

        ticketDAO.getTicketCountsByStatus();
        assertTrue(listAppender.containsMessage(Level.WARN, "Невідомий статус квитка 'INVALID_DB_STATUS' знайдено в базі даних під час підрахунку."));
    }
}