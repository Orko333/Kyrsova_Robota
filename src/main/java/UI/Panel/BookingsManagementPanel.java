package UI.Panel;

import DAO.TicketDAO;
import Models.Enums.FlightStatus;
import Models.Enums.TicketStatus;
import Models.Ticket;
import UI.Model.BookingsTableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;
import java.util.List;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Панель для управління бронюваннями та продажем квитків.
 */
class BookingsManagementPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger("insurance.log");

    private JTable bookingsTable;
    private BookingsTableModel bookingsTableModel;
    private JComboBox<TicketStatus> cmbStatusFilter;
    private JButton btnSellTicket, btnCancelBookingTicket, btnRefresh;

    private TicketDAO ticketDAO;

    public BookingsManagementPanel() {
        logger.info("Ініціалізація BookingsManagementPanel.");
        try {
            this.ticketDAO = new TicketDAO();
            logger.debug("TicketDAO успішно створено.");
        } catch (Exception e) {
            logger.fatal("Не вдалося створити TicketDAO в BookingsManagementPanel.", e);
            // Показати помилку користувачу та, можливо, не ініціалізувати панель далі
            JOptionPane.showMessageDialog(this, "Критична помилка: не вдалося ініціалізувати сервіс даних квитків.", "Помилка ініціалізації", JOptionPane.ERROR_MESSAGE);
            // Якщо це критично, можна кинути RuntimeException або просто не продовжувати
            return; // Зупинити подальшу ініціалізацію, якщо DAO не створено
        }

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        loadBookingsData(null); // Завантажити всі квитки спочатку
        logger.info("BookingsManagementPanel успішно ініціалізовано.");
    }

    private void initComponents() {
        logger.debug("Ініціалізація компонентів UI для BookingsManagementPanel.");
        // Панель фільтрів
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Фільтр за статусом:"));
        // Додаємо опцію "Всі" до комбобоксу
        TicketStatus[] statusesWithAll = new TicketStatus[TicketStatus.values().length + 1];
        statusesWithAll[0] = null; // Для "Всі"
        System.arraycopy(TicketStatus.values(), 0, statusesWithAll, 1, TicketStatus.values().length);

        cmbStatusFilter = new JComboBox<>(statusesWithAll);
        cmbStatusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TicketStatus) {
                    setText(((TicketStatus) value).getDisplayName());
                } else if (value == null) { // Для опції "Всі"
                    setText("Всі статуси");
                }
                return this;
            }
        });
        cmbStatusFilter.addActionListener(e -> {
            TicketStatus selectedStatus = (TicketStatus) cmbStatusFilter.getSelectedItem();
            logger.debug("Змінено фільтр статусу на: {}", (selectedStatus != null ? selectedStatus.getDisplayName() : "Всі статуси"));
            loadBookingsData(selectedStatus);
        });
        filterPanel.add(cmbStatusFilter);

        btnRefresh = new JButton("Оновити");
        btnRefresh.addActionListener(e -> {
            TicketStatus selectedStatus = (TicketStatus) cmbStatusFilter.getSelectedItem();
            logger.info("Натиснуто кнопку 'Оновити'. Поточний фільтр: {}", (selectedStatus != null ? selectedStatus.getDisplayName() : "Всі статуси"));
            loadBookingsData(selectedStatus);
        });
        filterPanel.add(btnRefresh);

        // Таблиця бронювань/квитків
        bookingsTableModel = new BookingsTableModel(new ArrayList<>());
        bookingsTable = new JTable(bookingsTableModel);
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingsTable.setAutoCreateRowSorter(true);
        // Налаштування рендерера для ціни
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        if (bookingsTable.getColumnModel().getColumnCount() > 7) { // Перевірка наявності стовпця
            bookingsTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer); // Ціна
        } else {
            logger.warn("Не вдалося знайти стовпець 'Ціна' (індекс 7) для налаштування рендерера.");
        }


        JScrollPane scrollPane = new JScrollPane(bookingsTable);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnSellTicket = new JButton("Продати квиток");
        btnCancelBookingTicket = new JButton("Скасувати бронювання/квиток");

        btnSellTicket.addActionListener(this::sellTicketAction);
        btnCancelBookingTicket.addActionListener(this::cancelTicketAction);

        // Керування активністю кнопок залежно від вибору
        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Щоб подія спрацьовувала лише один раз після завершення вибору
                updateButtonStates();
            }
        });

        buttonPanel.add(btnSellTicket);
        buttonPanel.add(btnCancelBookingTicket);

        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        updateButtonStates(); // Початковий стан кнопок
        logger.debug("Компоненти UI для BookingsManagementPanel успішно створені та додані.");
    }

    private void updateButtonStates() {
        int selectedRow = bookingsTable.getSelectedRow();
        logger.trace("Оновлення стану кнопок. Вибраний рядок: {}", selectedRow);

        if (selectedRow == -1) {
            btnSellTicket.setEnabled(false);
            btnCancelBookingTicket.setEnabled(false);
            logger.trace("Жоден рядок не вибрано, кнопки деактивовано.");
            return;
        }
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        Ticket selectedTicket = bookingsTableModel.getTicketAt(modelRow);

        if (selectedTicket == null) {
            btnSellTicket.setEnabled(false);
            btnCancelBookingTicket.setEnabled(false);
            logger.warn("Не вдалося отримати квиток для вибраного рядка (модельний індекс: {}). Кнопки деактивовано.", modelRow);
            return;
        }

        logger.debug("Оновлення стану кнопок для квитка ID: {}, Статус: {}", selectedTicket.getId(), selectedTicket.getStatus());
        btnSellTicket.setEnabled(selectedTicket.getStatus() == TicketStatus.BOOKED);
        btnCancelBookingTicket.setEnabled(selectedTicket.getStatus() == TicketStatus.BOOKED || selectedTicket.getStatus() == TicketStatus.SOLD);
        logger.trace("Стан кнопок: Продати={}, Скасувати={}", btnSellTicket.isEnabled(), btnCancelBookingTicket.isEnabled());
        // Можна додати перевірку, чи рейс ще не відбувся для скасування
    }

    private void loadBookingsData(TicketStatus filterStatus) {
        String statusForLog = (filterStatus != null) ? filterStatus.getDisplayName() : "Всі статуси";
        logger.info("Завантаження даних про бронювання/квитки. Фільтр за статусом: {}", statusForLog);
        try {
            List<Ticket> tickets = ticketDAO.getAllTickets(filterStatus);
            bookingsTableModel.setTickets(tickets);
            logger.info("Успішно завантажено {} квитків.", tickets.size());
        } catch (SQLException e) {
            logger.error("Помилка завантаження списку квитків. Фільтр: {}.", statusForLog, e);
            JOptionPane.showMessageDialog(this, "Помилка завантаження списку квитків: " + e.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            logger.error("Непередбачена помилка при завантаженні списку квитків. Фільтр: {}.", statusForLog, e);
            JOptionPane.showMessageDialog(this, "Непередбачена помилка: " + e.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
        updateButtonStates();
    }

    private void sellTicketAction(ActionEvent e) {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            logger.warn("Спроба продати квиток, але жоден рядок не вибрано.");
            return;
        }
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        Ticket ticketToSell = bookingsTableModel.getTicketAt(modelRow);

        if (ticketToSell == null) {
            logger.error("Не вдалося отримати квиток для продажу. Модельний індекс: {}", modelRow);
            JOptionPane.showMessageDialog(this, "Помилка: не вдалося отримати дані вибраного квитка.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logger.info("Спроба продати квиток ID: {}. Пасажир: {}. Поточний статус: {}",
                ticketToSell.getId(), ticketToSell.getPassenger().getFullName(), ticketToSell.getStatus());

        if (ticketToSell.getStatus() == TicketStatus.BOOKED) {
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Продати квиток ID " + ticketToSell.getId() + " пасажиру " + ticketToSell.getPassenger().getFullName() + "?",
                    "Підтвердження продажу", JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                logger.debug("Користувач підтвердив продаж квитка ID: {}", ticketToSell.getId());
                try {
                    if (ticketDAO.updateTicketStatus(ticketToSell.getId(), TicketStatus.SOLD, LocalDateTime.now())) {
                        logger.info("Квиток ID: {} успішно продано.", ticketToSell.getId());
                        JOptionPane.showMessageDialog(this, "Квиток успішно продано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadBookingsData((TicketStatus) cmbStatusFilter.getSelectedItem());
                    } else {
                        logger.warn("Не вдалося продати квиток ID: {} (DAO повернув false).", ticketToSell.getId());
                        JOptionPane.showMessageDialog(this, "Не вдалося продати квиток.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    logger.error("Помилка БД при продажу квитка ID: {}.", ticketToSell.getId(), ex);
                    JOptionPane.showMessageDialog(this, "Помилка БД при продажу: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
                } catch (Exception exGeneral) {
                    logger.error("Непередбачена помилка при продажу квитка ID: {}.", ticketToSell.getId(), exGeneral);
                    JOptionPane.showMessageDialog(this, "Непередбачена помилка: " + exGeneral.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.debug("Користувач скасував продаж квитка ID: {}", ticketToSell.getId());
            }
        } else {
            logger.warn("Спроба продати квиток ID: {}, який не має статусу BOOKED. Поточний статус: {}", ticketToSell.getId(), ticketToSell.getStatus());
            JOptionPane.showMessageDialog(this, "Цей квиток не може бути проданий (поточний статус: " + ticketToSell.getStatus().getDisplayName() + ").", "Помилка", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cancelTicketAction(ActionEvent e) {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            logger.warn("Спроба скасувати квиток, але жоден рядок не вибрано.");
            return;
        }
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        Ticket ticketToCancel = bookingsTableModel.getTicketAt(modelRow);

        if (ticketToCancel == null) {
            logger.error("Не вдалося отримати квиток для скасування. Модельний індекс: {}", modelRow);
            JOptionPane.showMessageDialog(this, "Помилка: не вдалося отримати дані вибраного квитка.", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String actionType = ticketToCancel.getStatus() == TicketStatus.BOOKED ? "бронювання" : "квиток";
        logger.info("Спроба скасувати {} ID: {}. Поточний статус: {}",
                actionType, ticketToCancel.getId(), ticketToCancel.getStatus());

        if (ticketToCancel.getStatus() == TicketStatus.BOOKED || ticketToCancel.getStatus() == TicketStatus.SOLD) {
            if (ticketToCancel.getFlight() == null || ticketToCancel.getFlight().getDepartureDateTime() == null) {
                logger.error("Не вдалося перевірити час відправлення рейсу для квитка ID: {} - дані рейсу неповні.", ticketToCancel.getId());
                JOptionPane.showMessageDialog(this, "Помилка даних рейсу. Неможливо перевірити час відправлення.", "Помилка даних", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Перевірка, чи рейс ще не відбувся (приклад)
            if (ticketToCancel.getFlight().getDepartureDateTime().isBefore(LocalDateTime.now()) &&
                    ticketToCancel.getFlight().getStatus() != null && // Додано перевірку на null
                    ticketToCancel.getFlight().getStatus() != FlightStatus.PLANNED &&
                    ticketToCancel.getFlight().getStatus() != FlightStatus.DELAYED) {
                logger.warn("Спроба скасувати {} ID: {} на рейс, який вже відбувся або скасований. Час відправлення: {}, Статус рейсу: {}",
                        actionType, ticketToCancel.getId(), ticketToCancel.getFlight().getDepartureDateTime(), ticketToCancel.getFlight().getStatus());
                JOptionPane.showMessageDialog(this, "Неможливо скасувати квиток на рейс, який вже відбувся або має статус, що не дозволяє скасування.", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }


            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Скасувати " + actionType + " ID " + ticketToCancel.getId() + "?",
                    "Підтвердження скасування", JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                logger.debug("Користувач підтвердив скасування {} ID: {}", actionType, ticketToCancel.getId());
                try {
                    // При скасуванні проданого квитка дата покупки не змінюється, лише статус
                    if (ticketDAO.updateTicketStatus(ticketToCancel.getId(), TicketStatus.CANCELLED, null)) {
                        logger.info("{} ID: {} успішно скасовано.", actionType.substring(0, 1).toUpperCase() + actionType.substring(1), ticketToCancel.getId());
                        JOptionPane.showMessageDialog(this, actionType.substring(0, 1).toUpperCase() + actionType.substring(1) + " успішно скасовано.", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                        loadBookingsData((TicketStatus) cmbStatusFilter.getSelectedItem());
                    } else {
                        logger.warn("Не вдалося скасувати {} ID: {} (DAO повернув false).", actionType, ticketToCancel.getId());
                        JOptionPane.showMessageDialog(this, "Не вдалося скасувати " + actionType + ".", "Помилка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    logger.error("Помилка БД при скасуванні {} ID: {}.", actionType, ticketToCancel.getId(), ex);
                    JOptionPane.showMessageDialog(this, "Помилка БД при скасуванні: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
                } catch (Exception exGeneral) {
                    logger.error("Непередбачена помилка при скасуванні {} ID: {}.", actionType, ticketToCancel.getId(), exGeneral);
                    JOptionPane.showMessageDialog(this, "Непередбачена помилка: " + exGeneral.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.debug("Користувач скасував операцію скасування {} ID: {}", actionType, ticketToCancel.getId());
            }
        } else {
            logger.warn("Спроба скасувати квиток ID: {}, який не має статусу BOOKED або SOLD. Поточний статус: {}", ticketToCancel.getId(), ticketToCancel.getStatus());
            JOptionPane.showMessageDialog(this, "Цей квиток/бронювання не може бути скасовано (поточний статус: " + ticketToCancel.getStatus().getDisplayName() + ").", "Помилка", JOptionPane.WARNING_MESSAGE);
        }
    }
}