package DAO;

import DB.DatabaseConnectionManager;
import Models.Stop;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Stop (Зупинки).
 * Надає методи для отримання даних про зупинки з бази даних.
 */
public class StopDAO { // Зроблено public для доступу з інших пакетів

    /**
     * Повертає список всіх зупинок з бази даних.
     * @return Список об'єктів {@link Stop}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Stop> getAllStops() throws SQLException {
        List<Stop> stops = new ArrayList<>();
        String sql = "SELECT id, name, city FROM stops ORDER BY city, name";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stops.add(new Stop(rs.getLong("id"), rs.getString("name"), rs.getString("city")));
            }
        }
        // SQLException буде прокинуто, якщо виникне
        return stops;
    }

    /**
     * Повертає зупинку за її ідентифікатором.
     * @param id Ідентифікатор зупинки.
     * @return Optional, що містить {@link Stop}, якщо зупинку не знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Stop> getStopById(long id) throws SQLException {
        String sql = "SELECT id, name, city FROM stops WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Stop(rs.getLong("id"), rs.getString("name"), rs.getString("city")));
                }
            }
        }
        return Optional.empty();
    }
}