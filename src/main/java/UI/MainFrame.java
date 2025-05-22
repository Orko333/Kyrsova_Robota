package UI;

import UI.Panel.FlightsPanel;
import UI.Panel.PassengersPanel;
import UI.Panel.ReportsPanel;
import UI.Panel.TicketsPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Головне вікно програми.
 * Використовує JTabbedPane для організації різних функціональних модулів.
 */
class MainFrame extends JFrame { // Клас вже існує, ми його доповнюємо
    private static final Logger logger = LogManager.getLogger("insurance.log"); // Використання логера "insurance.log"

    public MainFrame() {
        logger.info("Ініціалізація головного вікна програми (MainFrame).");
        setTitle("Автоматизована система управління автовокзалом");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        logger.debug("Створено JTabbedPane.");

        // Вкладка "Управління рейсами"
        try {
            logger.debug("Створення FlightsPanel...");
            FlightsPanel flightsPanel = new FlightsPanel();
            tabbedPane.addTab("Управління рейсами", createIcon("/icons/bus_schedule.png"), flightsPanel, "Операції з рейсами: створення, редагування, скасування");
            logger.info("Вкладку 'Управління рейсами' додано.");
        } catch (Exception e) {
            logger.error("Помилка при створенні або додаванні FlightsPanel.", e);
            JOptionPane.showMessageDialog(this, "Помилка завантаження модуля управління рейсами: " + e.getMessage(), "Помилка модуля", JOptionPane.ERROR_MESSAGE);
        }


        // Вкладка "Квитки"
        try {
            logger.debug("Створення TicketsPanel...");
            TicketsPanel ticketsPanel = new TicketsPanel();
            tabbedPane.addTab("Квитки", createIcon("/icons/ticket.png"), ticketsPanel, "Бронювання та продаж квитків");
            logger.info("Вкладку 'Квитки' додано.");
        } catch (Exception e) {
            logger.error("Помилка при створенні або додаванні TicketsPanel.", e);
            JOptionPane.showMessageDialog(this, "Помилка завантаження модуля квитків: " + e.getMessage(), "Помилка модуля", JOptionPane.ERROR_MESSAGE);
        }


        try {
            logger.debug("Створення PassengersPanel...");
            PassengersPanel passengersPanel = new PassengersPanel();
            tabbedPane.addTab("Пасажири", createIcon("/icons/passengers.png"), passengersPanel, "Управління даними пасажирів та історія поїздок");
            logger.info("Вкладку 'Пасажири' додано.");
        } catch (Exception e) {
            logger.error("Помилка при створенні або додаванні PassengersPanel.", e);
            JOptionPane.showMessageDialog(this, "Помилка завантаження модуля пасажирів: " + e.getMessage(), "Помилка модуля", JOptionPane.ERROR_MESSAGE);
        }


        // Нова вкладка "Звітність"
        try {
            logger.debug("Створення ReportsPanel...");
            ReportsPanel reportsPanel = new ReportsPanel();
            tabbedPane.addTab("Звітність", createIcon("/icons/report.png"), reportsPanel, "Перегляд звітів та статистики");
            logger.info("Вкладку 'Звітність' додано.");
        } catch (Exception e) {
            logger.error("Помилка при створенні або додаванні ReportsPanel.", e);
            JOptionPane.showMessageDialog(this, "Помилка завантаження модуля звітності: " + e.getMessage(), "Помилка модуля", JOptionPane.ERROR_MESSAGE);
        }


        add(tabbedPane);
        pack(); // Підганяє розмір вікна під вміст
        setMinimumSize(new Dimension(800, 600)); // Мінімальний розмір
        setLocationRelativeTo(null); // Центрувати вікно на екрані
        logger.info("Головне вікно програми успішно налаштовано.");
    }

    /**
     * Допоміжний метод для завантаження іконки.
     * Іконки мають бути в каталозі resources/icons (створіть його).
     * @param path Шлях до файлу іконки відносно classpath (напр., "/icons/my_icon.png").
     * @return ImageIcon або null, якщо іконку не знайдено.
     */
    protected ImageIcon createIcon(String path) {
        logger.trace("Спроба завантажити іконку за шляхом: {}", path);
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            logger.trace("Іконку {} успішно завантажено.", path);
            return new ImageIcon(imgURL);
        } else {
            logger.warn("Не вдалося знайти іконку: {}", path);
            return null;
        }
    }

    public static void main(String[] args) {
        logger.info("Запуск програми 'Автоматизована система управління автовокзалом'.");

        try {
            logger.debug("Спроба встановити FlatLaf LookAndFeel.");
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatIntelliJLaf());
            logger.info("FlatLaf LookAndFeel успішно встановлено.");
        } catch (Exception ex) {
            logger.warn("Не вдалося встановити FlatLaf LookAndFeel. Спроба використати Nimbus.", ex);
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        logger.info("Nimbus LookAndFeel успішно встановлено.");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Не вдалося встановити Nimbus LookAndFeel. Буде використано стандартний L&F.", e);
            }
        }

        SwingUtilities.invokeLater(() -> {
            logger.debug("Перевірка підключення до бази даних перед запуском GUI.");
            try (Connection conn = DB.DatabaseConnectionManager.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    logger.fatal("Критична помилка: не вдалося підключитися до бази даних (з'єднання null або закрите). Програма завершує роботу.");
                    JOptionPane.showMessageDialog(null,
                            "Не вдалося підключитися до бази даних. Програма не може продовжити роботу.\n" +
                                    "Перевірте налаштування в 'db.properties' та доступність сервера MySQL.",
                            "Критична помилка БД", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                logger.info("Підключення до БД успішне. Запускаємо GUI...");
            } catch (SQLException ex) {
                logger.fatal("Критична помилка підключення до бази даних. Програма завершує роботу.", ex);
                JOptionPane.showMessageDialog(null,
                        "Помилка підключення до бази даних: " + ex.getMessage() + "\n" +
                                "Програма не може продовжити роботу. Перевірте консоль для деталей.",
                        "Критична помилка БД", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Завершення програми, якщо немає з'єднання з БД
            } catch (Exception ex) { // Для інших можливих винятків при отриманні з'єднання
                logger.fatal("Непередбачена критична помилка під час перевірки з'єднання з БД. Програма завершує роботу.", ex);
                JOptionPane.showMessageDialog(null,
                        "Непередбачена помилка під час ініціалізації з'єднання з БД: " + ex.getMessage() + "\n" +
                                "Програма не може продовжити роботу.",
                        "Критична помилка", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            logger.debug("Створення екземпляра MainFrame.");
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
            logger.info("Головне вікно програми відображено.");
        });
    }
}