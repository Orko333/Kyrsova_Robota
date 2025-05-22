package DAO;

import DB.DatabaseConnectionManager;
import Models.Route;
import Models.Stop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Route (Маршрути).
 * Надає методи для отримання та управління даними маршрутів.
 */
public class RouteDAO {
    private static final Logger logger = LogManager.getLogger("insurance.log");
    private final StopDAO stopDAO = new StopDAO();

    /**
     * Допоміжний метод для завантаження проміжних зупинок для конкретного маршруту.
     * Використовує передане з'єднання.
     * @param conn Активне з'єднання з БД.
     * @param routeId Ідентифікатор маршруту.
     * @return Список проміжних зупинок.
     * @throws SQLException Якщо виникає помилка SQL.
     */
    private List<Stop> getIntermediateStopsForRoute(Connection conn, long routeId) throws SQLException {
        logger.debug("Завантаження проміжних зупинок для маршруту ID: {}", routeId);
        List<Stop> stops = new ArrayList<>();
        String sqlIntermediate = "SELECT stop_id FROM route_intermediate_stops WHERE route_id = ? ORDER BY stop_order";
        logger.trace("Виконується SQL для проміжних зупинок: {} з routeId={}", sqlIntermediate, routeId);

        try (PreparedStatement pstmtIntermediate = conn.prepareStatement(sqlIntermediate)) {
            pstmtIntermediate.setLong(1, routeId);
            try (ResultSet rsIntermediate = pstmtIntermediate.executeQuery()) {
                while (rsIntermediate.next()) {
                    long intermediateStopId = rsIntermediate.getLong("stop_id");
                    logger.trace("Обробка проміжної зупинки ID: {} для маршруту ID: {}", intermediateStopId, routeId);
                    Optional<Stop> intermediateStopOpt = stopDAO.getStopById(intermediateStopId);
                    if (intermediateStopOpt.isPresent()) {
                        stops.add(intermediateStopOpt.get());
                    } else {
                        logger.warn("Проміжна зупинка з ID {} для маршруту ID {} не знайдена в таблиці зупинок, але на неї є посилання.", intermediateStopId, routeId);
                    }
                }
            }
        }
        logger.debug("Знайдено {} проміжних зупинок для маршруту ID: {}", stops.size(), routeId);
        return stops;
    }

    /**
     * Повертає список всіх маршрутів з бази даних.
     * @return Список об'єктів {@link Route}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Route> getAllRoutes() throws SQLException {
        logger.info("Спроба отримати всі маршрути.");
        List<Route> routes = new ArrayList<>();
        String sqlRoutes = "SELECT id, departure_stop_id, destination_stop_id FROM routes ORDER BY id";
        logger.debug("Виконується SQL-запит для отримання всіх маршрутів: {}", sqlRoutes);

        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rsRoutes = stmt.executeQuery(sqlRoutes)) {

            while (rsRoutes.next()) {
                long routeId = rsRoutes.getLong("id");
                logger.debug("Обробка маршруту ID: {}", routeId);

                long departureStopId = rsRoutes.getLong("departure_stop_id");
                long destinationStopId = rsRoutes.getLong("destination_stop_id");

                logger.trace("Спроба отримати зупинку відправлення ID: {} для маршруту ID: {}", departureStopId, routeId);
                Stop departure = stopDAO.getStopById(departureStopId)
                        .orElseThrow(() -> {
                            String errorMsg = "Зупинка відправлення ID " + departureStopId + " не знайдена для маршруту ID: " + routeId;
                            logger.error(errorMsg);
                            return new SQLException(errorMsg);
                        });

                logger.trace("Спроба отримати зупинку призначення ID: {} для маршруту ID: {}", destinationStopId, routeId);
                Stop destination = stopDAO.getStopById(destinationStopId)
                        .orElseThrow(() -> {
                            String errorMsg = "Зупинка призначення ID " + destinationStopId + " не знайдена для маршруту ID: " + routeId;
                            logger.error(errorMsg);
                            return new SQLException(errorMsg);
                        });
                List<Stop> intermediateStops = getIntermediateStopsForRoute(conn, routeId);

                routes.add(new Route(routeId, departure, destination, intermediateStops));
                logger.trace("Маршрут ID {} успішно оброблено та додано до списку.", routeId);
            }
            logger.info("Успішно отримано {} маршрутів.", routes.size());
        } catch (SQLException e) {
            logger.error("Помилка при отриманні всіх маршрутів.", e);
            throw e;
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
        logger.info("Спроба отримати маршрут за ID: {}", id);
        String sql = "SELECT id, departure_stop_id, destination_stop_id FROM routes WHERE id = ?";
        logger.debug("Виконується SQL-запит для отримання маршруту за ID: {} з ID={}", sql, id);

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    logger.debug("Маршрут з ID {} знайдено. Обробка даних...", id);
                    long departureStopId = rs.getLong("departure_stop_id");
                    long destinationStopId = rs.getLong("destination_stop_id");

                    logger.trace("Спроба отримати зупинку відправлення ID: {} для маршруту ID: {}", departureStopId, id);
                    Stop departure = stopDAO.getStopById(departureStopId)
                            .orElseThrow(() -> {
                                String errorMsg = "Зупинка відправлення ID " + departureStopId + " не знайдена для маршруту ID: " + id;
                                logger.error(errorMsg);
                                return new SQLException(errorMsg);
                            });

                    logger.trace("Спроба отримати зупинку призначення ID: {} для маршруту ID: {}", destinationStopId, id);
                    Stop destination = stopDAO.getStopById(destinationStopId)
                            .orElseThrow(() -> {
                                String errorMsg = "Зупинка призначення ID " + destinationStopId + " не знайдена для маршруту ID: " + id;
                                logger.error(errorMsg);
                                return new SQLException(errorMsg);
                            });
                    List<Stop> intermediateStops = getIntermediateStopsForRoute(conn, id);

                    Route route = new Route(rs.getLong("id"), departure, destination, intermediateStops);
                    logger.info("Маршрут з ID {} успішно отримано.", id);
                    return Optional.of(route);
                } else {
                    logger.info("Маршрут з ID {} не знайдено.", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка при отриманні маршруту за ID {}.", id, e);
            throw e;
        }
    }
}