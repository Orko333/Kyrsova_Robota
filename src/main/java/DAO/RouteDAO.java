package DAO;

import DB.DatabaseConnectionManager;
import Models.Route;
import Models.Stop;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Route (Маршрути).
 * Надає методи для отримання та управління даними маршрутів.
 */
public class RouteDAO { // Зроблено public
    private final StopDAO stopDAO = new StopDAO(); // Припускаємо, що StopDAO завжди доступний

    /**
     * Допоміжний метод для завантаження проміжних зупинок для конкретного маршруту.
     * Використовує передане з'єднання.
     * @param conn Активне з'єднання з БД.
     * @param routeId Ідентифікатор маршруту.
     * @return Список проміжних зупинок.
     * @throws SQLException Якщо виникає помилка SQL.
     */
    private List<Stop> getIntermediateStopsForRoute(Connection conn, long routeId) throws SQLException {
        List<Stop> stops = new ArrayList<>();
        String sqlIntermediate = "SELECT stop_id FROM route_intermediate_stops WHERE route_id = ? ORDER BY stop_order";
        try (PreparedStatement pstmtIntermediate = conn.prepareStatement(sqlIntermediate)) {
            pstmtIntermediate.setLong(1, routeId);
            try (ResultSet rsIntermediate = pstmtIntermediate.executeQuery()) {
                while (rsIntermediate.next()) {
                    // getStopById тепер повертає Optional
                    Optional<Stop> intermediateStopOpt = stopDAO.getStopById(rsIntermediate.getLong("stop_id"));
                    intermediateStopOpt.ifPresent(stops::add); // Додаємо, якщо зупинка існує
                }
            }
        }
        return stops;
    }

    /**
     * Повертає список всіх маршрутів з бази даних.
     * @return Список об'єктів {@link Route}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Route> getAllRoutes() throws SQLException {
        List<Route> routes = new ArrayList<>();
        String sqlRoutes = "SELECT id, departure_stop_id, destination_stop_id FROM routes ORDER BY id";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rsRoutes = stmt.executeQuery(sqlRoutes)) {

            while (rsRoutes.next()) {
                long routeId = rsRoutes.getLong("id");
                // Використовуємо Optional та orElseThrow для критичних залежностей
                Stop departure = stopDAO.getStopById(rsRoutes.getLong("departure_stop_id"))
                        .orElseThrow(() -> new SQLException("Зупинка відправлення не знайдена для маршруту ID: " + routeId));
                Stop destination = stopDAO.getStopById(rsRoutes.getLong("destination_stop_id"))
                        .orElseThrow(() -> new SQLException("Зупинка призначення не знайдена для маршруту ID: " + routeId));

                List<Stop> intermediateStops = getIntermediateStopsForRoute(conn, routeId); // Використовуємо те саме з'єднання

                routes.add(new Route(routeId, departure, destination, intermediateStops));
            }
        }
        return routes;
    }

    /**
     * Повертає маршрут за його ідентифікатором.
     * @param id Ідентифікатор маршруту.
     * @return Optional, що містить {@link Route}, якщо маршрут знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Route> getRouteById(long id) throws SQLException {
        String sql = "SELECT id, departure_stop_id, destination_stop_id FROM routes WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Stop departure = stopDAO.getStopById(rs.getLong("departure_stop_id"))
                            .orElseThrow(() -> new SQLException("Зупинка відправлення не знайдена для маршруту ID: " + id));
                    Stop destination = stopDAO.getStopById(rs.getLong("destination_stop_id"))
                            .orElseThrow(() -> new SQLException("Зупинка призначення не знайдена для маршруту ID: " + id));
                    List<Stop> intermediateStops = getIntermediateStopsForRoute(conn, id);

                    return Optional.of(new Route(rs.getLong("id"), departure, destination, intermediateStops));
                }
            }
        }
        return Optional.empty();
    }
}