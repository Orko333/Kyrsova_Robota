package DAO;

import DB.DatabaseConnectionManager;
import Models.Enums.BenefitType;
import Models.Passenger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Passenger (Пасажири).
 */
public class PassengerDAO { // Зроблено public

    /**
     * Додає нового пасажира до бази даних.
     * Якщо пасажир з таким документом вже існує, повертає існуючого.
     * @param passenger Об'єкт {@link Passenger} для додавання.
     * @return ID доданого або існуючого пасажира.
     * @throws SQLException якщо виникає помилка доступу до бази даних або не вдалося отримати/створити пасажира.
     */
    public long addOrGetPassenger(Passenger passenger) throws SQLException {
        Optional<Passenger> existingPassenger = findByDocument(passenger.getDocumentType(), passenger.getDocumentNumber());
        if (existingPassenger.isPresent()) {
            return existingPassenger.get().getId();
        }

        String sql = "INSERT INTO passengers (full_name, document_number, document_type, phone_number, email, benefit_type) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, passenger.getFullName());
            pstmt.setString(2, passenger.getDocumentNumber());
            pstmt.setString(3, passenger.getDocumentType());
            pstmt.setString(4, passenger.getPhoneNumber());
            pstmt.setString(5, passenger.getEmail());
            pstmt.setString(6, passenger.getBenefitType().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
            throw new SQLException("Не вдалося створити пасажира, ключі не згенеровано.");
        } catch (SQLException e) {
            // Обробка унікального ключа uq_passenger_document
            if (e.getSQLState() != null && e.getSQLState().equals("23000")) { // Код помилки для порушення унікальності
                // Спробувати знайти ще раз, якщо виникла гонка потоків
                return findByDocument(passenger.getDocumentType(), passenger.getDocumentNumber())
                        .map(Passenger::getId)
                        .orElseThrow(() -> new SQLException("Пасажир з таким документом вже існує, але не вдалося його отримати після конфлікту.", e));
            }
            throw e; // Прокинути оригінальний SQLException
        }
    }

    /**
     * Знаходить пасажира за типом та номером документа.
     * @param documentType Тип документа.
     * @param documentNumber Номер документа.
     * @return Optional, що містить {@link Passenger} якщо знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Passenger> findByDocument(String documentType, String documentNumber) throws SQLException {
        String sql = "SELECT id, full_name, document_number, document_type, phone_number, email, benefit_type FROM passengers WHERE document_type = ? AND document_number = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, documentType);
            pstmt.setString(2, documentNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPassenger(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Знаходить пасажира за його ID.
     * @param passengerId ID пасажира.
     * @return Optional, що містить {@link Passenger} якщо знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Passenger> findById(long passengerId) throws SQLException {
        String sql = "SELECT id, full_name, document_number, document_type, phone_number, email, benefit_type FROM passengers WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, passengerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPassenger(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Повертає список всіх пасажирів з бази даних.
     * @return Список об'єктів {@link Passenger}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Passenger> getAllPassengers() throws SQLException {
        List<Passenger> passengers = new ArrayList<>();
        String sql = "SELECT id, full_name, document_number, document_type, phone_number, email, benefit_type FROM passengers ORDER BY full_name";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                passengers.add(mapRowToPassenger(rs));
            }
        }
        return passengers;
    }

    /**
     * Оновлює дані існуючого пасажира в базі даних.
     * @param passenger Об'єкт {@link Passenger} з оновленими даними.
     * @return {@code true}, якщо пасажира успішно оновлено, {@code false} в іншому випадку.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public boolean updatePassenger(Passenger passenger) throws SQLException {
        String sql = "UPDATE passengers SET full_name = ?, document_number = ?, document_type = ?, phone_number = ?, email = ?, benefit_type = ? WHERE id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, passenger.getFullName());
            pstmt.setString(2, passenger.getDocumentNumber());
            pstmt.setString(3, passenger.getDocumentType());
            pstmt.setString(4, passenger.getPhoneNumber());
            pstmt.setString(5, passenger.getEmail());
            pstmt.setString(6, passenger.getBenefitType().name());
            pstmt.setLong(7, passenger.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    private Passenger mapRowToPassenger(ResultSet rs) throws SQLException {
        return new Passenger(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("document_number"),
                rs.getString("document_type"),
                rs.getString("phone_number"),
                rs.getString("email"),
                BenefitType.valueOf(rs.getString("benefit_type"))
        );
    }
}