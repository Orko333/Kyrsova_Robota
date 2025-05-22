package UI;

import UI.Panel.FlightsPanel;
import UI.Panel.PassengersPanel;
import UI.Panel.ReportsPanel;
import UI.Panel.TicketsPanel;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Головне вікно програми.
 * Використовує JTabbedPane для організації різних функціональних модулів.
 */
class MainFrame extends JFrame { // Клас вже існує, ми його доповнюємо

    public MainFrame() {
        setTitle("Автоматизована система управління автовокзалом");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Використаємо PreferredSize для компонентів, а потім pack() або setSize()
        // setSize(1200, 800); // Можна збільшити розмір за замовчуванням

        JTabbedPane tabbedPane = new JTabbedPane();

        // Вкладка "Управління рейсами"
        FlightsPanel flightsPanel = new FlightsPanel();
        tabbedPane.addTab("Управління рейсами", createIcon("/icons/bus_schedule.png"), flightsPanel, "Операції з рейсами: створення, редагування, скасування");

        // Вкладка "Квитки"
        TicketsPanel ticketsPanel = new TicketsPanel();
        tabbedPane.addTab("Квитки", createIcon("/icons/ticket.png"), ticketsPanel, "Бронювання та продаж квитків");

        PassengersPanel passengersPanel = new PassengersPanel();
        tabbedPane.addTab("Пасажири", createIcon("/icons/passengers.png"), passengersPanel, "Управління даними пасажирів та історія поїздок");

        // Нова вкладка "Звітність"
        ReportsPanel reportsPanel = new ReportsPanel();
        tabbedPane.addTab("Звітність", createIcon("/icons/report.png"), reportsPanel, "Перегляд звітів та статистики");
        add(tabbedPane);
        pack(); // Підганяє розмір вікна під вміст
        setMinimumSize(new Dimension(800, 600)); // Мінімальний розмір
        setLocationRelativeTo(null); // Центрувати вікно на екрані
    }

    /**
     * Допоміжний метод для завантаження іконки.
     * Іконки мають бути в каталозі resources/icons (створіть його).
     * @param path Шлях до файлу іконки відносно classpath (напр., "/icons/my_icon.png").
     * @return ImageIcon або null, якщо іконку не знайдено.
     */
    protected ImageIcon createIcon(String path) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Не вдалося знайти іконку: " + path);
            return null;
        }
    }

    public static void main(String[] args) {
        // Встановлення "красивого" Look and Feel (наприклад, FlatLaf)
        // Потрібно додати бібліотеку FlatLaf до проекту:
        // Maven:
        // <dependency>
        //     <groupId>com.formdev</groupId>
        //     <artifactId>flatlaf</artifactId>
        //     <version>3.4.1</version> <!-- Перевірте актуальну версію -->
        // </dependency>
        try {
            // UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf()); // Світла тема
            // UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf()); // Темна тема
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatIntelliJLaf()); // IntelliJ тема
            // UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatMacLightLaf()); // Mac світла
        } catch (Exception ex) {
            System.err.println("Не вдалося встановити FlatLaf LookAndFeel. Використовується стандартний.");
            // Якщо FlatLaf недоступний, спробуємо Nimbus
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Не вдалося встановити Nimbus LookAndFeel: " + e.getMessage());
            }
        }

        SwingUtilities.invokeLater(() -> {
            // Перевірка підключення до БД (залишається з попереднього прикладу)
            try (Connection conn = DB.DatabaseConnectionManager.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    JOptionPane.showMessageDialog(null,
                            "Не вдалося підключитися до бази даних. Програма не може продовжити роботу.\n" +
                                    "Перевірте налаштування в 'db.properties' та доступність сервера MySQL.",
                            "Критична помилка БД", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                System.out.println("Підключення до БД успішне. Запускаємо GUI...");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Помилка підключення до бази даних: " + ex.getMessage() + "\n" +
                                "Програма не може продовжити роботу. Перевірте консоль для деталей.",
                        "Критична помилка БД", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                System.exit(1);
            }

            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
