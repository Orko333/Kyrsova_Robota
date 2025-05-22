package DAO;

import DB.DatabaseConnectionManager;
import Models.Enums.BenefitType;
import Models.Passenger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для роботи з об'єктами Passenger (Пасажири).
 */
public class PassengerDAO { // Зроблено public
    private static final Logger logger = LogManager.getLogger("insurance.log"); // Використання логера "insurance.log"

    /**
     * Додає нового пасажира до бази даних.
     * Якщо пасажир з таким документом вже існує, повертає існуючого.
     * @param passenger Об'єкт {@link Passenger} для додавання.
     * @return ID доданого або існуючого пасажира.
     * @throws SQLException якщо виникає помилка доступу до бази даних або не вдалося отримати/створити пасажира.
     */
    public long addOrGetPassenger(Passenger passenger) throws SQLException {
        logger.info("Спроба додати або отримати пасажира: Тип документа={}, Номер документа={}",
                passenger.getDocumentType(), passenger.getDocumentNumber());

        Optional<Passenger> existingPassenger = findByDocument(passenger.getDocumentType(), passenger.getDocumentNumber());
        if (existingPassenger.isPresent()) {
            long existingId = existingPassenger.get().getId();
            logger.info("Пасажир з документом Тип={}, Номер={} вже існує з ID={}. Повертається існуючий ID.",
                    passenger.getDocumentType(), passenger.getDocumentNumber(), existingId);
            return existingId;
        }

        logger.info("Пасажир з документом Тип={}, Номер={} не знайдений. Спроба додати нового пасажира: {}",
                passenger.getDocumentType(), passenger.getDocumentNumber(), passenger);
        String sql = "INSERT INTO passengers (full_name, document_number, document_type, phone_number, email, benefit_type) VALUES (?, ?, ?, ?, ?, ?)";
        logger.debug("Виконується SQL-запит для додавання пасажира: {}", sql);

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
                        long newId = generatedKeys.getLong(1);
                        logger.info("Нового пасажира успішно додано. ID нового пасажира: {}", newId);
                        return newId;
                    } else {
                        logger.error("Не вдалося створити пасажира, ключі не згенеровано, хоча affectedRows > 0.");
                        throw new SQLException("Не вдалося створити пасажира, ключі не згенеровано.");
                    }
                }
            } else {
                logger.error("Пасажира не було додано (affectedRows = 0). Пасажир: {}", passenger);
                throw new SQLException("Не вдалося створити пасажира, жоден рядок не було змінено.");
            }
        } catch (SQLException e) {
            logger.warn("Помилка SQL під час спроби додати пасажира: SQLState={}, Повідомлення={}", e.getSQLState(), e.getMessage());
            // Обробка унікального ключа uq_passenger_document
            // Код помилки для порушення унікальності може відрізнятися для різних БД. '23000' є загальним, але '23505' для PostgreSQL.
            // Краще перевіряти на основі назви обмеження, якщо це можливо, або на більш специфічний SQLState, якщо відомо.
            if (e.getSQLState() != null && (e.getSQLState().equals("23000") || e.getSQLState().equals("23505") || e.getMessage().toLowerCase().contains("unique constraint") || e.getMessage().toLowerCase().contains("uq_passenger_document"))) {
                logger.info("Виник конфлікт унікальності (SQLState={}). Спроба знайти пасажира ще раз, можливо, через гонку потоків.", e.getSQLState());
                // Спробувати знайти ще раз, якщо виникла гонка потоків
                return findByDocument(passenger.getDocumentType(), passenger.getDocumentNumber())
                        .map(p -> {
                            logger.info("Пасажир знайдений після конфлікту унікальності. ID: {}", p.getId());
                            return p.getId();
                        })
                        .orElseThrow(() -> {
                            logger.error("Пасажир з таким документом вже існує, але не вдалося його отримати після конфлікту.", e);
                            return new SQLException("Пасажир з таким документом вже існує, але не вдалося його отримати після конфлікту.", e);
                        });
            }
            logger.error("Непередбачена помилка SQL при додаванні пасажира: {}", passenger, e);
            throw e; // Прокинути оригінальний SQLException, якщо це не помилка унікальності
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
        logger.info("Пошук пасажира за документом: Тип={}, Номер={}", documentType, documentNumber);
        String sql = "SELECT id, full_name, document_number, document_type, phone_number, email, benefit_type FROM passengers WHERE document_type = ? AND document_number = ?";
        logger.debug("Виконується SQL-запит: {}", sql);

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, documentType);
            pstmt.setString(2, documentNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Passenger passenger = mapRowToPassenger(rs);
                    logger.info("Пасажира знайдено за документом: Тип={}, Номер={}. ID={}", documentType, documentNumber, passenger.getId());
                    return Optional.of(passenger);
                } else {
                    logger.info("Пасажира не знайдено за документом: Тип={}, Номер={}", documentType, documentNumber);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка при пошуку пасажира за документом: Тип={}, Номер={}", documentType, documentNumber, e);
            throw e;
        }
    }

    /**
     * Знаходить пасажира за його ID.
     * @param passengerId ID пасажира.
     * @return Optional, що містить {@link Passenger} якщо знайдено.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public Optional<Passenger> findById(long passengerId) throws SQLException {
        logger.info("Пошук пасажира за ID: {}", passengerId);
        String sql = "SELECT id, full_name, document_number, document_type, phone_number, email, benefit_type FROM passengers WHERE id = ?";
        logger.debug("Виконується SQL-запит: {}", sql);

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, passengerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Passenger passenger = mapRowToPassenger(rs);
                    logger.info("Пасажира знайдено за ID {}: {}", passengerId, passenger);
                    return Optional.of(passenger);
                } else {
                    logger.info("Пасажира з ID {} не знайдено.", passengerId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка при пошуку пасажира за ID {}:", passengerId, e);
            throw e;
        }
    }

    /**
     * Повертає список всіх пасажирів з бази даних.
     * @return Список об'єктів {@link Passenger}.
     * @throws SQLException якщо виникає помилка доступу до бази даних.
     */
    public List<Passenger> getAllPassengers() throws SQLException {
        logger.info("Спроба отримати всіх пасажирів.");
        List<Passenger> passengers = new ArrayList<>();
        String sql = "SELECT id, full_name, document_number, document_type, phone_number, email, benefit_type FROM passengers ORDER BY full_name";
        logger.debug("Виконується SQL-запит: {}", sql);

        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                passengers.add(mapRowToPassenger(rs));
            }
            logger.info("Успішно отримано {} пасажирів.", passengers.size());
        } catch (SQLException e) {
            logger.error("Помилка при отриманні всіх пасажирів", e);
            throw e;
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
        logger.info("Спроба оновити пасажира з ID {}: {}", passenger.getId(), passenger);
        String sql = "UPDATE passengers SET full_name = ?, document_number = ?, document_type = ?, phone_number = ?, email = ?, benefit_type = ? WHERE id = ?";
        logger.debug("Виконується SQL-запит для оновлення пасажира: {}", sql);

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, passenger.getFullName());
            pstmt.setString(2, passenger.getDocumentNumber());
            pstmt.setString(3, passenger.getDocumentType());
            pstmt.setString(4, passenger.getPhoneNumber());
            pstmt.setString(5, passenger.getEmail());
            pstmt.setString(6, passenger.getBenefitType().name());
            pstmt.setLong(7, passenger.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Пасажира з ID {} успішно оновлено.", passenger.getId());
                return true;
            } else {
                logger.warn("Пасажира з ID {} не знайдено або не було оновлено.", passenger.getId());
                return false;
            }
        } catch (SQLException e) {
            logger.error("Помилка при оновленні пасажира з ID {}: {}", passenger.getId(), passenger, e);
            throw e;
        }
    }

    private Passenger mapRowToPassenger(ResultSet rs) throws SQLException {
        // Логування тут може бути надмірним, оскільки цей метод викликається часто.
        // Якщо потрібне детальне логування мапінгу, його можна додати на рівні DEBUG.
        // logger.trace("Мапінг рядка ResultSet на об'єкт Passenger. ID: {}", rs.getLong("id"));
        String benefitTypeStr = rs.getString("benefit_type");
        BenefitType benefitType;
        try {
            benefitType = BenefitType.valueOf(benefitTypeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            long passengerId = rs.getLong("id"); // Отримуємо ID для контексту помилки
            logger.error("Недійсний або відсутній тип пільги '{}' для пасажира з ID {}. Встановлюється NONE.", benefitTypeStr, passengerId, e);
            // Можна встановити значення за замовчуванням або кинути виняток, залежно від бізнес-логіки
            benefitType = BenefitType.NONE; // Або кинути new SQLException("Недійсний тип пільги: " + benefitTypeStr, e);
        }

        return new Passenger(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("document_number"),
                rs.getString("document_type"),
                rs.getString("phone_number"),
                rs.getString("email"),
                benefitType
        );
    }
}