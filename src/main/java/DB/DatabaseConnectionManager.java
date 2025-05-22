package DB;

import Config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Клас-менеджер для управління з'єднаннями з базою даних.
 * Надає метод для отримання активного з'єднання.
 */
public class DatabaseConnectionManager {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Критична помилка: JDBC драйвер MySQL не знайдено. Перевірте залежності проекту.");
            throw new RuntimeException("Не вдалося завантажити JDBC драйвер MySQL", e);
        }
    }

    /**
     * Встановлює та повертає з'єднання з базою даних.
     * <p>
     * Важливо: Клієнтський код, який викликає цей метод,
     * відповідає за закриття отриманого з'єднання за допомогою
     * {@code connection.close()} у блоці {@code finally} або використовуючи try-with-resources.
     * </p>
     *
     * @return Об'єкт {@link Connection} для взаємодії з базою даних.
     * @throws SQLException якщо виникає помилка під час спроби підключення до бази даних
     *                      (наприклад, неправильні облікові дані, сервер недоступний,
     *                      конфігураційний файл не знайдено або містить неповні дані).
     */
    public static Connection getConnection() throws SQLException {
        String url = DatabaseConfig.getDbUrl();
        String user = DatabaseConfig.getDbUsername();
        String password = DatabaseConfig.getDbPassword();

        if (url == null || url.trim().isEmpty() ||
                user == null ||
                password == null) {
            System.err.println("Помилка конфігурації: URL, ім'я користувача або пароль для БД не вказані або не завантажені. Перевірте файл 'db.properties'.");
            throw new SQLException("Неповні конфігураційні дані для підключення до БД.");
        }

        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Допоміжний метод для тихого закриття ресурсів (Connection, Statement, ResultSet).
     * Ігнорує винятки, що виникають під час закриття.
     *
     * @param resource ресурс, який потрібно закрити (має реалізовувати AutoCloseable).
     */
    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.err.println("Не вдалося тихо закрити ресурс: " + e.getMessage());
            }
        }
    }
}
