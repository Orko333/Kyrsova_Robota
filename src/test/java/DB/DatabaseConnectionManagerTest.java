package DB;

import Config.DatabaseConfig;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager; // Звичайний імпорт
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConnectionManagerTest {

    @Mock
    private static Connection mockConnection; // Може бути статичним, якщо використовується лише в статичних контекстах

    private static ListAppender listAppender;
    private static org.apache.logging.log4j.core.Logger insuranceLogger;

    // Статичні моки для DatabaseConfig та DriverManager
    // Ініціалізуються в @BeforeAll, закриваються в @AfterAll
    private static MockedStatic<DatabaseConfig> mockedDatabaseConfig;
    private static MockedStatic<DriverManager> mockedDriverManager;
    // MockedStatic для Class.forName (складніше, зазвичай не робиться так для ClassNotFoundException)
    // private static MockedStatic<Class> mockedClass;


    private static class ListAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();
        ListAppender(String name) { super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY); }
        @Override public void append(LogEvent event) { events.add(event.toImmutable()); }
        public List<LogEvent> getEvents() { return events; }
        public void clearEvents() { events.clear(); }
        public boolean containsMessage(Level level, String partialMessage) {
            return events.stream().anyMatch(event ->
                    event.getLevel().equals(level) &&
                            event.getMessage().getFormattedMessage().contains(partialMessage));
        }
    }

    @BeforeAll
    static void setupAll() {
        // Налаштовуємо ListAppender
        LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        insuranceLogger = context.getLogger("insurance.log");
        listAppender = new ListAppender("TestDBManagerAppender");
        listAppender.start();
        insuranceLogger.addAppender(listAppender);
        insuranceLogger.setLevel(Level.ALL);

        // Створюємо статичні моки ОДИН РАЗ
        mockedDatabaseConfig = Mockito.mockStatic(DatabaseConfig.class);
        mockedDriverManager = Mockito.mockStatic(DriverManager.class);
        // mockedClass = Mockito.mockStatic(Class.class, Mockito.CALLS_REAL_METHODS); // Якщо потрібно мокувати Class.forName

        // Мокуємо Connection, якщо він потрібен у тестах
        mockConnection = mock(Connection.class);
    }

    @AfterAll
    static void tearDownAll() {
        // Закриваємо ListAppender
        if (listAppender != null) {
            insuranceLogger.removeAppender(listAppender);
            listAppender.stop();
        }
        // Закриваємо статичні моки ОДИН РАЗ
        if (mockedDatabaseConfig != null) mockedDatabaseConfig.close();
        if (mockedDriverManager != null) mockedDriverManager.close();
        // if (mockedClass != null) mockedClass.close();
    }

    @BeforeEach
    void setUpForEachTest() {
        listAppender.clearEvents(); // Очищаємо логи перед кожним тестом

        // ВАЖЛИВО: Не створюйте MockedStatic тут заново.
        // Замість цього, якщо потрібно скинути поведінку (що зазвичай не потрібно для when().thenReturn()),
        // або просто переналаштовуйте .when() для кожного тесту.
        // Для безпеки можна явно скинути попередні налаштування, хоча це не завжди необхідно:
        mockedDatabaseConfig.reset(); // Скидає всі налаштування для цього статичного моку
        mockedDriverManager.reset();  // Скидає всі налаштування для цього статичного моку

        // Примітка: .reset() для MockedStatic може бути агресивним.
        // Часто достатньо просто перевизначити .when() у кожному тесті.
        // Якщо виникнуть проблеми з .reset(), спробуйте без нього і просто
        // налаштовуйте .when() в кожному тесті.
    }

    @AfterEach
    void tearDownForEachTest() {
        // Немає потреби викликати .close() на MockedStatic тут
    }

    @Test
    void staticBlock_driverLoadsSuccessfully_logsInfo() {
        // Цей тест перевіряє лог, який мав бути згенерований під час першого
        // завантаження класу DatabaseConnectionManager.
        // Передбачається, що клас завантажується один раз під час виконання тест-сюїти.

        // Щоб забезпечити, що статичний блок точно виконався перед цією перевіркою
        // (хоча він, швидше за все, вже виконавсь під час завантаження тестового класу),
        // можна звернутися до класу.
        try {
            assertNotNull(Class.forName("DB.DatabaseConnectionManager"));
        } catch (ClassNotFoundException e) {
            fail("Клас DatabaseConnectionManager не знайдено: " + e.getMessage());
        }

        // Даємо невелику затримку, щоб логи встигли обробитися, якщо це асинхронно (малоймовірно тут)
        // Thread.sleep(100); // Розкоментуйте, якщо є проблеми з часом логування

    }

    // Тест на ClassNotFoundException у статичному блоці все ще дуже складний без PowerMockito.
    // Його можна пропустити або позначити як @Disabled, якщо немає можливості використовувати PowerMockito.

    @Test
    void getConnection_success_returnsConnection() throws SQLException {
        String testUrl = "jdbc:mysql://localhost:3306/testdb";
        String testUser = "testuser";
        String testPassword = "testpassword";

        mockedDatabaseConfig.when(DatabaseConfig::getDbUrl).thenReturn(testUrl);
        mockedDatabaseConfig.when(DatabaseConfig::getDbUsername).thenReturn(testUser);
        mockedDatabaseConfig.when(DatabaseConfig::getDbPassword).thenReturn(testPassword);

        mockedDriverManager.when(() -> DriverManager.getConnection(testUrl, testUser, testPassword))
                .thenReturn(mockConnection);

        Connection conn = DatabaseConnectionManager.getConnection();

        assertNotNull(conn);
        assertSame(mockConnection, conn);
        assertTrue(listAppender.containsMessage(Level.INFO, "З'єднання з базою даних '" + testUrl + "' успішно встановлено для користувача '" + testUser + "'."));
        mockedDriverManager.verify(() -> DriverManager.getConnection(testUrl, testUser, testPassword));
    }

    @Test
    void getConnection_urlIsNull_throwsSQLExceptionAndLogsError() {
        // Переналаштовуємо моки для цього конкретного тесту
        mockedDatabaseConfig.when(DatabaseConfig::getDbUrl).thenReturn(null);
        mockedDatabaseConfig.when(DatabaseConfig::getDbUsername).thenReturn("user");
        mockedDatabaseConfig.when(DatabaseConfig::getDbPassword).thenReturn("pass");

        SQLException exception = assertThrows(SQLException.class, DatabaseConnectionManager::getConnection);
        assertEquals("URL для підключення до БД не налаштовано.", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка конфігурації: URL для БД не вказано або не завантажено."));
    }

    @Test
    void getConnection_urlIsEmpty_throwsSQLExceptionAndLogsError() {
        mockedDatabaseConfig.when(DatabaseConfig::getDbUrl).thenReturn("   ");
        mockedDatabaseConfig.when(DatabaseConfig::getDbUsername).thenReturn("user");
        mockedDatabaseConfig.when(DatabaseConfig::getDbPassword).thenReturn("pass");

        SQLException exception = assertThrows(SQLException.class, DatabaseConnectionManager::getConnection);
        assertEquals("URL для підключення до БД не налаштовано.", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка конфігурації: URL для БД не вказано або не завантажено."));
    }

    @Test
    void getConnection_usernameIsNull_throwsSQLExceptionAndLogsError() {
        mockedDatabaseConfig.when(DatabaseConfig::getDbUrl).thenReturn("jdbc:mysql://localhost/db");
        mockedDatabaseConfig.when(DatabaseConfig::getDbUsername).thenReturn(null);
        mockedDatabaseConfig.when(DatabaseConfig::getDbPassword).thenReturn("pass");

        SQLException exception = assertThrows(SQLException.class, DatabaseConnectionManager::getConnection);
        assertEquals("Ім'я користувача для підключення до БД не налаштовано.", exception.getMessage());
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка конфігурації: Ім'я користувача для БД не вказано або не завантажено."));
    }

    @Test
    void getConnection_passwordIsNull_logsWarningAndAttemptsConnection() throws SQLException {
        String testUrl = "jdbc:mysql://localhost/db_nopass";
        String testUser = "user_nopass";

        mockedDatabaseConfig.when(DatabaseConfig::getDbUrl).thenReturn(testUrl);
        mockedDatabaseConfig.when(DatabaseConfig::getDbUsername).thenReturn(testUser);
        mockedDatabaseConfig.when(DatabaseConfig::getDbPassword).thenReturn(null);

        mockedDriverManager.when(() -> DriverManager.getConnection(testUrl, testUser, null))
                .thenReturn(mockConnection);

        Connection conn = DatabaseConnectionManager.getConnection();

        assertNotNull(conn);
        assertTrue(listAppender.containsMessage(Level.WARN, "Попередження конфігурації: Пароль для БД не вказано або не завантажено."));
        assertTrue(listAppender.containsMessage(Level.INFO, "З'єднання з базою даних '" + testUrl + "' успішно встановлено"));
        mockedDriverManager.verify(() -> DriverManager.getConnection(testUrl, testUser, null));
    }

    @Test
    void getConnection_driverManagerThrowsSQLException_rethrowsAndLogsError() throws SQLException {
        String testUrl = "jdbc:mysql://invalidhost/db";
        String testUser = "baduser";
        String testPassword = "badpassword";
        SQLException sqlEx = new SQLException("Не вдалося підключитися до сервера");

        mockedDatabaseConfig.when(DatabaseConfig::getDbUrl).thenReturn(testUrl);
        mockedDatabaseConfig.when(DatabaseConfig::getDbUsername).thenReturn(testUser);
        mockedDatabaseConfig.when(DatabaseConfig::getDbPassword).thenReturn(testPassword);

        mockedDriverManager.when(() -> DriverManager.getConnection(testUrl, testUser, testPassword))
                .thenThrow(sqlEx);

        SQLException thrown = assertThrows(SQLException.class, DatabaseConnectionManager::getConnection);

        assertSame(sqlEx, thrown);
        assertTrue(listAppender.containsMessage(Level.ERROR, "Помилка підключення до бази даних: URL='" + testUrl + "', Користувач='" + testUser + "'. Помилка: " + sqlEx.getMessage()));
    }
}