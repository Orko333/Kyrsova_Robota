package DAO;

import DB.DatabaseConnectionManager;
import Models.*; // Flight, Passenger, Route, Stop, Ticket, TicketStatus
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Ticket (Квитки).
 */
public class TicketDAO { // Зроблено public
    private final FlightDAO flightDAO = new FlightDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final RouteDAO routeDAO = new RouteDAO(); // Додано для getSalesByRouteForPeriod

    /**
     * Повертає список заброньованих або проданих місць для конкретного рейсу.
     * @param flightId Ідентифікатор рейсу.
     * @return Список рядків з номерами зайнятих місць.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<String> getOccupiedSeatsForFlight(long flightId) throws SQLException {
        List<String> occupiedSeats = new ArrayList<>();
        String sql = "SELECT seat_number FROM tickets WHERE flight_id = ? AND (status = 'BOOKED' OR status = 'SOLD')";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, flightId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    occupiedSeats.add(rs.getString("seat_number"));
                }
            }
        }
        return occupiedSeats;
    }

    /**
     * Додає новий квиток (бронювання) до бази даних.
     * @param ticket Об'єкт {@link Ticket} для додавання.
     * @return {@code true}, якщо квиток успішно додано та ID встановлено, {@code false} в іншому випадку.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public boolean addTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO tickets (flight_id, passenger_id, seat_number, booking_date_time, booking_expiry_date_time, price_paid, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, ticket.getFlight().getId());
            pstmt.setLong(2, ticket.getPassenger().getId());
            pstmt.setString(3, ticket.getSeatNumber());
            pstmt.setTimestamp(4, Timestamp.valueOf(ticket.getBookingDateTime()));
            if (ticket.getBookingExpiryDateTime() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(ticket.getBookingExpiryDateTime()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }
            pstmt.setBigDecimal(6, ticket.getPricePaid());
            pstmt.setString(7, ticket.getStatus().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ticket.setId(generatedKeys.getLong(1));
                        return true;
                    }
                }
            }
            // Якщо не вдалося додати або отримати ключ, кидаємо виняток або повертаємо false
            // Залежно від логіки, може бути краще кинути виняток, щоб викликаючий код знав про проблему
            // throw new SQLException("Не вдалося додати квиток, ключі не згенеровано.");
        } catch (SQLException e) {
            // Обробка унікального ключа uq_ticket_flight_seat
            if (e.getSQLState() != null && e.getSQLState().equals("23000") && e.getMessage().toLowerCase().contains("uq_ticket_flight_seat")) {
                // Можна прокинути кастомний виняток або повернути false
                System.err.println("Помилка додавання квитка: Місце " + ticket.getSeatNumber() + " на рейсі " + ticket.getFlight().getId() + " вже зайняте.");
                // throw new SeatAlreadyBookedException("Місце " + ticket.getSeatNumber() + " на рейсі " + ticket.getFlight().getId() + " вже зайняте.", e);
                return false; // Або так, щоб UI міг обробити це
            }
            throw e; // Прокинути інші SQL винятки
        }
        return false;
    }

    /**
     * Знаходить квиток за ID рейсу та номером місця.
     * @param flightId ID рейсу.
     * @param seatNumber Номер місця.
     * @return Optional, що містить {@link Ticket} якщо знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Ticket> findByFlightAndSeat(long flightId, String seatNumber) throws SQLException {
        String sql = "SELECT id, flight_id, passenger_id, seat_number, booking_date_time, purchase_date_time, booking_expiry_date_time, price_paid, status FROM tickets WHERE flight_id = ? AND seat_number = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, flightId);
            pstmt.setString(2, seatNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Flight flight = flightDAO.getFlightById(rs.getLong("flight_id"))
                            .orElseThrow(() -> {
                                try {
                                    return new SQLException("Рейс не знайдено для квитка на рейс ID: " + rs.getLong("flight_id"));
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    Passenger passenger = passengerDAO.findById(rs.getLong("passenger_id"))
                            .orElseThrow(() -> {
                                try {
                                    return new SQLException("Пасажира не знайдено для квитка з пасажиром ID: " + rs.getLong("passenger_id"));
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    Ticket ticket = new Ticket(
                            rs.getLong("id"),
                            flight,
                            passenger,
                            rs.getString("seat_number"),
                            rs.getTimestamp("booking_date_time").toLocalDateTime(),
                            rs.getBigDecimal("price_paid"),
                            TicketStatus.valueOf(rs.getString("status"))
                    );
                    Timestamp purchaseTs = rs.getTimestamp("purchase_date_time");
                    if (purchaseTs != null) ticket.setPurchaseDateTime(purchaseTs.toLocalDateTime());
                    Timestamp expiryTs = rs.getTimestamp("booking_expiry_date_time");
                    if (expiryTs != null) ticket.setBookingExpiryDateTime(expiryTs.toLocalDateTime());
                    return Optional.of(ticket);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Оновлює статус квитка та, опціонально, дату покупки.
     * @param ticketId Ідентифікатор квитка.
     * @param newStatus Новий статус квитка.
     * @param purchaseDateTime Дата та час покупки (може бути null, якщо статус не 'SOLD').
     * @return {@code true}, якщо статус успішно оновлено, {@code false} в іншому випадку.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public boolean updateTicketStatus(long ticketId, TicketStatus newStatus, LocalDateTime purchaseDateTime) throws SQLException {
        String sql;
        if (newStatus == TicketStatus.SOLD && purchaseDateTime != null) {
            sql = "UPDATE tickets SET status = ?, purchase_date_time = ?, booking_expiry_date_time = NULL WHERE id = ?"; // При продажу скидаємо термін броні
        } else if (newStatus == TicketStatus.CANCELLED) {
            sql = "UPDATE tickets SET status = ?, booking_expiry_date_time = NULL WHERE id = ?"; // При скасуванні теж
        }
        else {
            sql = "UPDATE tickets SET status = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            if (newStatus == TicketStatus.SOLD && purchaseDateTime != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(purchaseDateTime));
                pstmt.setLong(3, ticketId);
            } else {
                pstmt.setLong(2, ticketId);
            }
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Повертає список всіх квитків для конкретного пасажира (історія поїздок).
     * @param passengerId Ідентифікатор пасажира.
     * @return Список об'єктів {@link Ticket}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Ticket> getTicketsByPassengerId(long passengerId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        // Запит залишається з попередньої версії, він досить повний для історії
        String sql = "SELECT t.id, t.flight_id, t.passenger_id, t.seat_number, t.booking_date_time, t.purchase_date_time, t.booking_expiry_date_time, t.price_paid, t.status, " +
                "f.departure_date_time AS flight_departure_date_time, f.arrival_date_time AS flight_arrival_date_time, f.total_seats AS flight_total_seats, f.bus_model AS flight_bus_model, f.price_per_seat AS flight_price_per_seat, f.status AS flight_status, " +
                "r.id AS route_id, r.departure_stop_id, r.destination_stop_id, " +
                "ds.name AS dep_stop_name, ds.city AS dep_stop_city, " +
                "as_s.name AS arr_stop_name, as_s.city AS arr_stop_city " +
                "FROM tickets t " +
                "JOIN flights f ON t.flight_id = f.id " +
                "JOIN routes r ON f.route_id = r.id " +
                "JOIN stops ds ON r.departure_stop_id = ds.id " +
                "JOIN stops as_s ON r.destination_stop_id = as_s.id " +
                "WHERE t.passenger_id = ? ORDER BY f.departure_date_time DESC";

        Passenger passenger = passengerDAO.findById(passengerId)
                .orElseThrow(() -> new SQLException("Пасажира з ID " + passengerId + " не знайдено для історії поїздок."));

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, passengerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Stop departureStop = new Stop(rs.getLong("departure_stop_id"), rs.getString("dep_stop_name"), rs.getString("dep_stop_city"));
                    Stop arrivalStop = new Stop(rs.getLong("destination_stop_id"), rs.getString("arr_stop_name"), rs.getString("arr_stop_city"));
                    Route route = new Route(rs.getLong("route_id"), departureStop, arrivalStop, new ArrayList<>()); // Проміжні зупинки тут не завантажуються для спрощення

                    Flight flight = new Flight(
                            rs.getLong("flight_id"), route,
                            rs.getTimestamp("flight_departure_date_time").toLocalDateTime(),
                            rs.getTimestamp("flight_arrival_date_time").toLocalDateTime(),
                            rs.getInt("flight_total_seats"), FlightStatus.valueOf(rs.getString("flight_status")),
                            rs.getString("flight_bus_model"), rs.getBigDecimal("flight_price_per_seat"));

                    Ticket ticket = new Ticket(
                            rs.getLong("id"), flight, passenger, rs.getString("seat_number"),
                            rs.getTimestamp("booking_date_time").toLocalDateTime(),
                            rs.getBigDecimal("price_paid"), TicketStatus.valueOf(rs.getString("status")));
                    Timestamp purchaseTs = rs.getTimestamp("purchase_date_time");
                    if (purchaseTs != null) ticket.setPurchaseDateTime(purchaseTs.toLocalDateTime());
                    Timestamp expiryTs = rs.getTimestamp("booking_expiry_date_time");
                    if (expiryTs != null) ticket.setBookingExpiryDateTime(expiryTs.toLocalDateTime());
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }

    /**
     * Повертає список всіх квитків, опціонально фільтрованих за статусом.
     * @param statusFilter Статус для фільтрації (може бути null, щоб отримати всі квитки).
     * @return Список об'єктів {@link Ticket}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Ticket> getAllTickets(TicketStatus statusFilter) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT t.id, t.flight_id, t.passenger_id, t.seat_number, t.booking_date_time, t.purchase_date_time, t.booking_expiry_date_time, t.price_paid, t.status " +
                        "FROM tickets t "
        );
        List<Object> params = new ArrayList<>();
        if (statusFilter != null) {
            sqlBuilder.append("WHERE t.status = ? ");
            params.add(statusFilter.name());
        }
        sqlBuilder.append("ORDER BY t.booking_date_time DESC");

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Flight flight = flightDAO.getFlightById(rs.getLong("flight_id"))
                            .orElseThrow(() -> {
                                try {
                                    return new SQLException("Рейс не знайдено для квитка ID: " + rs.getLong("id"));
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    Passenger passenger = passengerDAO.findById(rs.getLong("passenger_id"))
                            .orElseThrow(() -> {
                                try {
                                    return new SQLException("Пасажира не знайдено для квитка ID: " + rs.getLong("id"));
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    Ticket ticket = new Ticket(
                            rs.getLong("id"), flight, passenger, rs.getString("seat_number"),
                            rs.getTimestamp("booking_date_time").toLocalDateTime(),
                            rs.getBigDecimal("price_paid"), TicketStatus.valueOf(rs.getString("status")));
                    Timestamp purchaseTs = rs.getTimestamp("purchase_date_time");
                    if (purchaseTs != null) ticket.setPurchaseDateTime(purchaseTs.toLocalDateTime());
                    Timestamp expiryTs = rs.getTimestamp("booking_expiry_date_time");
                    if (expiryTs != null) ticket.setBookingExpiryDateTime(expiryTs.toLocalDateTime());
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }

    /**
     * Повертає статистику продажів (сума та кількість) за вказаний період, згруповану по маршрутах.
     * @param startDate Початкова дата періоду.
     * @param endDate Кінцева дата періоду.
     * @return Мапа, де ключ - назва маршруту, а значення - мапа {"totalSales": BigDecimal, "ticketCount": Integer}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Map<String, Map<String, Object>> getSalesByRouteForPeriod(LocalDate startDate, LocalDate endDate) throws SQLException {
        Map<String, Map<String, Object>> salesData = new HashMap<>();
        String sql = "SELECT r.id as route_id, SUM(t.price_paid) as total_amount, COUNT(t.id) as tickets_sold " +
                "FROM tickets t " +
                "JOIN flights f ON t.flight_id = f.id " +
                "JOIN routes r ON f.route_id = r.id " +
                "WHERE t.status = 'SOLD' AND DATE(t.purchase_date_time) BETWEEN ? AND ? " +
                "GROUP BY r.id";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long routeId = rs.getLong("route_id");
                    Route route = routeDAO.getRouteById(routeId)
                            .orElse(null); // Використовуємо orElse(null) тут, бо маршрут може бути видалений, але продажі по ньому залишились
                    String routeDescription = (route != null) ? route.getFullRouteDescription() : "Невідомий маршрут (ID: " + routeId + ")";

                    Map<String, Object> data = new HashMap<>();
                    data.put("totalSales", rs.getBigDecimal("total_amount"));
                    data.put("ticketCount", rs.getInt("tickets_sold"));
                    salesData.put(routeDescription, data);
                }
            }
        }
        return salesData;
    }

    /**
     * Повертає кількість квитків за кожним статусом.
     * @return Мапа, де ключ - {@link TicketStatus}, а значення - кількість квитків.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Map<TicketStatus, Integer> getTicketCountsByStatus() throws SQLException {
        Map<TicketStatus, Integer> statusCounts = new HashMap<>();
        String sql = "SELECT status, COUNT(id) as count FROM tickets GROUP BY status";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try { // Додано try-catch для безпечного парсингу статусу
                    TicketStatus status = TicketStatus.valueOf(rs.getString("status"));
                    statusCounts.put(status, rs.getInt("count"));
                } catch (IllegalArgumentException e) {
                    System.err.println("Невідомий статус квитка в БД: " + rs.getString("status"));
                }
            }
        }
        for (TicketStatus ts : TicketStatus.values()) {
            statusCounts.putIfAbsent(ts, 0);
        }
        return statusCounts;
    }
}