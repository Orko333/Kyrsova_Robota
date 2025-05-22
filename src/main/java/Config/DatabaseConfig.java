package Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Клас для завантаження конфігураційних даних для підключення до бази даних
 * з файлу db.properties.
 */
public class DatabaseConfig {
    private static final String PROPERTIES_FILE = "db.properties";
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                System.err.println("Помилка: Неможливо знайти конфігураційний файл '" + PROPERTIES_FILE + "'. Переконайтесь, що він знаходиться в src/main/resources.");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            System.err.println("Помилка завантаження конфігураційного файлу '" + PROPERTIES_FILE + "': " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Повертає URL для підключення до бази даних.
     *
     * @return URL бази даних або null, якщо властивість не знайдена.
     */
    public static String getDbUrl() {
        return properties.getProperty("db.url");
    }

    /**
     * Повертає ім'я користувача для підключення до бази даних.
     *
     * @return Ім'я користувача або null, якщо властивість не знайдена.
     */
    public static String getDbUsername() {
        return properties.getProperty("db.username");
    }

    /**
     * Повертає пароль для підключення до бази даних.
     *
     * @return Пароль або null, якщо властивість не знайдена.
     */
    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }
}
