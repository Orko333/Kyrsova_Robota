package DAO;

import DB.DatabaseConnectionManager;
import Models.Flight;
import Models.Enums.FlightStatus;
import Models.Route;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Flight (Рейси).
 */
public class FlightDAO { // Зроблено public
    private final RouteDAO routeDAO = new RouteDAO();

    /**
     * Повертає список всіх рейсів з бази даних.
     * @return Список об'єктів {@link Flight}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Flight> getAllFlights() throws SQLException {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT id, route_id, departure_date_time, arrival_date_time, total_seats, bus_model, price_per_seat, status FROM flights ORDER BY departure_date_time DESC";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long routeId = rs.getLong("route_id");
                Route route = routeDAO.getRouteById(routeId)
                        .orElseThrow(() -> {
                            try {
                                return new SQLException("Маршрут ID " + routeId + " не знайдено для рейсу ID: " + rs.getLong("id"));
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });

                flights.add(new Flight(
                        rs.getLong("id"),
                        route,
                        rs.getTimestamp("departure_date_time").toLocalDateTime(),
                        rs.getTimestamp("arrival_date_time").toLocalDateTime(),
                        rs.getInt("total_seats"),
                        FlightStatus.valueOf(rs.getString("status").toUpperCase()),
                        rs.getString("bus_model"),
                        rs.getBigDecimal("price_per_seat")
                ));
            }
        }
        return flights;
    }

    /**
     * Додає новий рейс до бази даних.
     * @param flight Об'єкт {@link Flight} для додавання.
     * @return {@code true}, якщо рейс успішно додано та ID встановлено, {@code false} в іншому випадку.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public boolean addFlight(Flight flight) throws SQLException {
        String sql = "INSERT INTO flights (route_id, departure_date_time, arrival_date_time, total_seats, bus_model, price_per_seat, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, flight.getRoute().getId());
            pstmt.setTimestamp(2, Timestamp.valueOf(flight.getDepartureDateTime()));
            pstmt.setTimestamp(3, Timestamp.valueOf(flight.getArrivalDateTime()));
            pstmt.setInt(4, flight.getTotalSeats());
            pstmt.setString(5, flight.getBusModel());
            pstmt.setBigDecimal(6, flight.getPricePerSeat());
            pstmt.setString(7, flight.getStatus().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        flight.setId(generatedKeys.getLong(1));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Оновлює дані існуючого рейсу в базі даних.
     * @param flight Об'єкт {@link Flight} з оновленими даними.
     * @return {@code true}, якщо рейс успішно оновлено, {@code false} в іншому випадку.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public boolean updateFlight(Flight flight) throws SQLException {
        String sql = "UPDATE flights SET route_id = ?, departure_date_time = ?, arrival_date_time = ?, total_seats = ?, bus_model = ?, price_per_seat = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, flight.getRoute().getId());
            pstmt.setTimestamp(2, Timestamp.valueOf(flight.getDepartureDateTime()));
            pstmt.setTimestamp(3, Timestamp.valueOf(flight.getArrivalDateTime()));
            pstmt.setInt(4, flight.getTotalSeats());
            pstmt.setString(5, flight.getBusModel());
            pstmt.setBigDecimal(6, flight.getPricePerSeat());
            pstmt.setString(7, flight.getStatus().name());
            pstmt.setLong(8, flight.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Оновлює статус рейсу.
     * @param flightId Ідентифікатор рейсу.
     * @param status Новий статус рейсу.
     * @return {@code true}, якщо статус успішно оновлено, {@code false} в іншому випадку.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public boolean updateFlightStatus(long flightId, FlightStatus status) throws SQLException {
        String sql = "UPDATE flights SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setLong(2, flightId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Повертає кількість зайнятих місць (заброньованих або проданих) для конкретного рейсу.
     * @param flightId Ідентифікатор рейсу.
     * @return Кількість зайнятих місць.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public int getOccupiedSeatsCount(long flightId) throws SQLException {
        String sql = "SELECT COUNT(id) FROM tickets WHERE flight_id = ? AND (status = 'BOOKED' OR status = 'SOLD')";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, flightId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Повертає рейс за його ідентифікатором.
     * @param id Ідентифікатор рейсу.
     * @return Optional, що містить {@link Flight} якщо знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Flight> getFlightById(long id) throws SQLException {
        String sql = "SELECT id, route_id, departure_date_time, arrival_date_time, total_seats, bus_model, price_per_seat, status FROM flights WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Route route = routeDAO.getRouteById(rs.getLong("route_id"))
                            .orElseThrow(() -> new SQLException("Маршрут не знайдено для рейсу ID: " + id));
                    return Optional.of(new Flight(
                            rs.getLong("id"),
                            route,
                            rs.getTimestamp("departure_date_time").toLocalDateTime(),
                            rs.getTimestamp("arrival_date_time").toLocalDateTime(),
                            rs.getInt("total_seats"),
                            FlightStatus.valueOf(rs.getString("status").toUpperCase()),
                            rs.getString("bus_model"),
                            rs.getBigDecimal("price_per_seat")
                    ));
                }
            }
        }
        return Optional.empty();
    }
        /**
         * Повертає список рейсів на конкретну дату.
         * @param date Дата, на яку потрібно знайти рейси.
         * @return Список об'єктів {@link Flight}.
         * @throws SQLException якщо виникає помилка доступу до бази даних.
         */
        public List<Flight> getFlightsByDate(LocalDate date) throws SQLException {
            List<Flight> flightsOnDate = new ArrayList<>();
            String sql = "SELECT id, route_id, departure_date_time, arrival_date_time, total_seats, bus_model, price_per_seat, status " +
                    "FROM flights WHERE DATE(departure_date_time) = ? ORDER BY departure_date_time";
            try (Connection conn = DatabaseConnectionManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long routeId = rs.getLong("route_id");
                        Route route = routeDAO.getRouteById(routeId)
                                .orElseThrow(() -> {
                                    try {
                                        return new SQLException("Маршрут ID " + routeId + " не знайдено для рейсу ID: " + rs.getLong("id"));
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                        flightsOnDate.add(new Flight(
                                rs.getLong("id"),
                                route,
                                rs.getTimestamp("departure_date_time").toLocalDateTime(),
                                rs.getTimestamp("arrival_date_time").toLocalDateTime(),
                                rs.getInt("total_seats"),
                                Models.Enums.FlightStatus.valueOf(rs.getString("status").toUpperCase()),
                                rs.getString("bus_model"),
                                rs.getBigDecimal("price_per_seat")
                        ));
                    }
                }
            }
            return flightsOnDate;
        }
    }