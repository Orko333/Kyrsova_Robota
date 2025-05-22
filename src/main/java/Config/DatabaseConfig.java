package Config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Клас для завантаження конфігураційних даних для підключення до бази даних
 * з файлу db.properties.
 */
public class DatabaseConfig {
    private static final Logger logger = LogManager.getLogger(DatabaseConfig.class);
    private static final String PROPERTIES_FILE = "db.properties";
    private static final Properties properties = new Properties();

    static {
        logger.info("Початок завантаження конфігурації бази даних з файлу '{}'", PROPERTIES_FILE);
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.error("Помилка: Неможливо знайти конфігураційний файл '{}'. Переконайтесь, що він знаходиться в src/main/resources.", PROPERTIES_FILE);
                // Можна кинути RuntimeException, якщо файл конфігурації є критичним
                // throw new RuntimeException("Конфігураційний файл " + PROPERTIES_FILE + " не знайдено.");
            } else {
                properties.load(input);
                logger.info("Конфігураційний файл '{}' успішно завантажено.", PROPERTIES_FILE);
                // Логування завантажених властивостей (опціонально, може бути корисно для налагодження)
                // if (logger.isDebugEnabled()) {
                //     properties.forEach((key, value) -> logger.debug("Завантажена властивість: {} = {}", key, value));
                // }
            }
        } catch (IOException ex) {
            logger.error("Помилка завантаження конфігураційного файлу '{}': {}", PROPERTIES_FILE, ex.getMessage(), ex);
            // Можна кинути RuntimeException тут також, якщо завантаження конфігурації критичне
            // throw new RuntimeException("Помилка завантаження конфігураційного файлу " + PROPERTIES_FILE, ex);
        }
    }

    /**
     * Повертає URL для підключення до бази даних.
     *
     * @return URL бази даних або null, якщо властивість не знайдена.
     */
    public static String getDbUrl() {
        String url = properties.getProperty("db.url");
        if (url == null) {
            logger.warn("Властивість 'db.url' не знайдена у файлі '{}'", PROPERTIES_FILE);
        }
        // logger.debug("Повертається db.url: {}", url); // Увімкніть для детального логування
        return url;
    }

    /**
     * Повертає ім'я користувача для підключення до бази даних.
     *
     * @return Ім'я користувача або null, якщо властивість не знайдена.
     */
    public static String getDbUsername() {
        String username = properties.getProperty("db.username");
        if (username == null) {
            logger.warn("Властивість 'db.username' не знайдена у файлі '{}'", PROPERTIES_FILE);
        }
        // logger.debug("Повертається db.username: {}", username); // Увімкніть для детального логування
        return username;
    }

    /**
     * Повертає пароль для підключення до бази даних.
     *
     * @return Пароль або null, якщо властивість не знайдена.
     */
    public static String getDbPassword() {
        String password = properties.getProperty("db.password");
        if (password == null) {
            logger.warn("Властивість 'db.password' не знайдена у файлі '{}'", PROPERTIES_FILE);
        }
        // logger.debug("Повертається db.password: {}", (password != null ? "****" : null)); // Не логуйте пароль напряму
        return password;
    }

    // Для тестування (опціонально)
    public static void main(String[] args) {
        logger.info("Тестування DatabaseConfig...");
        System.out.println("DB URL: " + DatabaseConfig.getDbUrl());
        System.out.println("DB Username: " + DatabaseConfig.getDbUsername());
        System.out.println("DB Password: " + DatabaseConfig.getDbPassword());

        // Симуляція помилки для тестування логування помилок
        if (DatabaseConfig.getDbUrl() == null) {
            logger.error("Критична помилка: URL бази даних не налаштовано!");
        }
        logger.info("Тестування DatabaseConfig завершено.");
    }
}