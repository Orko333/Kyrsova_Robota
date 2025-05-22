package Config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class DatabaseConfigTest {

    private static ListAppender listAppender;
    // Logger for DatabaseConfig class, to capture its logs
    private static org.apache.logging.log4j.core.Logger sutLogger;
    // Logger for this test class, to log test-specific simulation messages
    private static final org.apache.logging.log4j.Logger testClassLogger = LogManager.getLogger(DatabaseConfigTest.class);


    // Original PROPERTIES_FILE constant from DatabaseConfig
    private static final String SUT_PROPERTIES_FILENAME = "db.properties";


    // Helper class to capture log messages
    private static class ListAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        ListAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        public List<LogEvent> getEvents() {
            return events;
        }

        public void clearEvents() {
            events.clear();
        }

        public boolean containsMessage(Level level, String partialMessage) {
            return events.stream().anyMatch(event ->
                    event.getLevel().equals(level) &&
                            event.getMessage().getFormattedMessage().contains(partialMessage));
        }
    }

    @BeforeClass
    public static void setupClass() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        // Capture logs specifically from the DatabaseConfig class
        sutLogger = context.getLogger(DatabaseConfig.class.getName());

        listAppender = new ListAppender("ListAppenderForTest");
        listAppender.start();
        sutLogger.addAppender(listAppender);
        sutLogger.setLevel(Level.ALL); // Capture all levels from SUT

        // Note: The actual static block of DatabaseConfig will run once when the class
        // is first loaded by the test runner. Its logs (e.g., initial load attempt)
        // will be captured if this happens after listAppender is set up.
        // If DatabaseConfig is loaded before this @BeforeClass, initial logs might be missed.
        // Usually, JUnit loads test class, then SUT when first referenced.
    }

    @AfterClass
    public static void teardownClass() {
        if (listAppender != null) {
            sutLogger.removeAppender(listAppender);
            listAppender.stop();
        }
    }

    /**
     * Clears the content of the existing static final Properties object in DatabaseConfig.
     */
    private void clearSutProperties() throws NoSuchFieldException, IllegalAccessException {
        Field propertiesField = DatabaseConfig.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Properties propsInstance = (Properties) propertiesField.get(null); // Get the existing final instance
        assertNotNull("DatabaseConfig.properties instance should not be null", propsInstance);
        propsInstance.clear(); // Clear its contents
    }

    /**
     * Simulates loading properties content into DatabaseConfig's internal Properties object.
     * It clears the existing properties first (done in setup now) and then loads new ones.
     *
     * @param propertiesContent The string content of the properties to load.
     *                          If null, simulates that the properties file was not found/empty.
     */
    private void simulatePropertiesLoad(String propertiesContent) {
        try {
            Field propertiesField = DatabaseConfig.class.getDeclaredField("properties");
            propertiesField.setAccessible(true);
            Properties propsInstance = (Properties) propertiesField.get(null);
            assertNotNull("DatabaseConfig.properties instance should not be null for simulation", propsInstance);

            // propsInstance is already cleared by clearSutProperties() in setup()

            if (propertiesContent != null) {
                try (InputStream stream = new ByteArrayInputStream(propertiesContent.getBytes(StandardCharsets.UTF_8))) {
                    propsInstance.load(stream); // Load into the existing, cleared instance
                }
                // Log using the test class's logger for clarity that this is a test simulation step
                testClassLogger.info("TEST_SIMULATION: Content loaded into DatabaseConfig.properties for '{}'.", SUT_PROPERTIES_FILENAME);
            } else {
                // If content is null, propsInstance remains empty (already cleared)
                testClassLogger.info("TEST_SIMULATION: DatabaseConfig.properties remains empty for '{}' (simulating not found/empty).", SUT_PROPERTIES_FILENAME);
            }
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new RuntimeException("Failed to simulate properties load: " + e.getMessage(), e);
        }
    }


    @Before
    public void setup() {
        listAppender.clearEvents(); // Clear logs from previous test
        try {
            clearSutProperties(); // Clear the SUT's properties before each test
        } catch (Exception e) {
            fail("Failed to clear SUT properties in @Before: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        // listAppender.clearEvents(); // Already done in setup()
    }

    @Test
    public void testAllPropertiesPresent() {
        String propsContent = "db.url=jdbc:mysql://host1:3306/db1\n" +
                "db.username=user1\n" +
                "db.password=pass1";
        simulatePropertiesLoad(propsContent);

        assertEquals("jdbc:mysql://host1:3306/db1", DatabaseConfig.getDbUrl());
        assertEquals("user1", DatabaseConfig.getDbUsername());
        assertEquals("pass1", DatabaseConfig.getDbPassword());

        List<LogEvent> logs = listAppender.getEvents();
        // Check that NO warnings were logged by getters from SUT
        assertFalse("SUT should not log 'db.url' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("db.url")));
        assertFalse("SUT should not log 'db.username' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("db.username")));
        assertFalse("SUT should not log 'db.password' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("db.password")));
    }

    @Test
    public void testSomePropertiesMissing() {
        String propsContent = "db.url=jdbc:mysql://host2:3306/db2\n" +
                "#db.username is missing\n" +
                "db.password=pass2";
        simulatePropertiesLoad(propsContent);

        assertEquals("jdbc:mysql://host2:3306/db2", DatabaseConfig.getDbUrl());
        assertNull(DatabaseConfig.getDbUsername()); // Username is missing
        assertEquals("pass2", DatabaseConfig.getDbPassword());

        List<LogEvent> logs = listAppender.getEvents();

        // Check for specific SUT warning for missing username
        assertTrue("SUT should log 'db.username' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("Властивість 'db.username' не знайдена")));
        // Check that NO warnings for present properties
        assertFalse("SUT should not log 'db.url' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("db.url")));
    }

    @Test
    public void testPropertiesFileCompletelyMissingOrEmptySimulation() {
        simulatePropertiesLoad(null); // null content means properties object remains empty after clear

        assertNull(DatabaseConfig.getDbUrl());
        assertNull(DatabaseConfig.getDbUsername());
        assertNull(DatabaseConfig.getDbPassword());

        List<LogEvent> logs = listAppender.getEvents();

        // These warnings come from the SUT's getters
        assertTrue("SUT should log 'db.url' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("Властивість 'db.url' не знайдена")));
        assertTrue("SUT should log 'db.username' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("Властивість 'db.username' не знайдена")));
        assertTrue("SUT should log 'db.password' not found warning",
                logs.stream().anyMatch(event -> event.getLoggerName().equals(DatabaseConfig.class.getName()) &&
                        event.getLevel() == Level.WARN &&
                        event.getMessage().getFormattedMessage().contains("Властивість 'db.password' не знайдена")));
    }

    @Test
    public void testIOExceptionDuringInternalLoadSimulation() {
        // This test simulates an IOException occurring if properties.load() itself fails INSIDE SUT.
        // The SUT's static initializer runs once. This test checks behavior IF that load had failed with IOException.
        // We achieve this by simulating the state *after* such a failure (empty properties)
        // and checking that appropriate error/warnings are logged by the getters.

        // `clearSutProperties()` in @Before handles making the Properties object empty.
        // Now, we need to simulate the ERROR log that DatabaseConfig's static block *would have* produced.
        // This is tricky because the static block only runs once.
        // A more accurate test for the static block's IOException path would require PowerMock
        // or running the test in an environment where db.properties IS malformed for DatabaseConfig's initial load.

        // For this unit test, we focus on the state *after* such an event.
        // The properties are already empty due to setup().
        // If we want to assert the ERROR log message that *would have been logged by the SUT's static block*,
        // it's hard without controlling the SUT's first load.
        // What we *can* do is check the getter warnings.

        // Simulate that an IOException occurred during the SUT's static properties.load()
        // This means properties would be empty, and an error would have been logged by SUT.
        // We can't easily re-trigger the static block's catch(IOException).
        // Instead, we assert the outcome: properties are empty, getters return null and warn.

        // Let's assume the SUT itself did log an error if properties.load() failed initially.
        // We are testing the state where properties are empty (due to load failure)
        // and getters are called.

        // This test becomes similar to testPropertiesFileCompletelyMissingOrEmptySimulation
        // in terms of getter behavior. The main difference would be the initial ERROR log
        // from the static block, which is hard to isolate per test without advanced techniques.

        // For now, let's ensure getters behave correctly if properties are empty.
        simulatePropertiesLoad(null); // Ensures properties are empty

        assertNull("URL should be null", DatabaseConfig.getDbUrl());
        assertNull("Username should be null", DatabaseConfig.getDbUsername());
        assertNull("Password should be null", DatabaseConfig.getDbPassword());

        List<LogEvent> logs = listAppender.getEvents();
        assertTrue("Getter for URL should warn",
                listAppender.containsMessage(Level.WARN, "Властивість 'db.url' не знайдена"));
        assertTrue("Getter for Username should warn",
                listAppender.containsMessage(Level.WARN, "Властивість 'db.username' не знайдена"));
        assertTrue("Getter for Password should warn",
                listAppender.containsMessage(Level.WARN, "Властивість 'db.password' не знайдена"));

        // To truly test the SUT's static 'catch (IOException ex)' block, you'd ideally:
        // 1. Place a malformed 'db.properties' file that causes Properties.load() to throw IOException.
        // 2. Ensure DatabaseConfig class is loaded for the first time with this malformed file.
        // 3. Check that the specific ERROR log from the static block is present.
        // This often requires a separate test module or careful classpath manipulation.
        testClassLogger.info("testIOExceptionDuringInternalLoadSimulation: Verifying getter behavior when properties are empty, simulating aftermath of SUT's internal load IOException.");
    }
}