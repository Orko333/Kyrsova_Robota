package UI.Dialog;

import DAO.PassengerDAO;
import DAO.TicketDAO;
import Models.*; // Flight, Passenger, Ticket, BenefitType, TicketStatus
import Models.Enums.BenefitType;
import Models.Enums.TicketStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.sql.SQLException; // Для обробки винятків
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale; // Для NumberFormat

/**
 * Діалогове вікно для бронювання квитка.
 */
public class BookingDialog extends JDialog { // Зроблено public
    private final Flight selectedFlight;
    private final String selectedSeat;

    private JTextField txtFullName, txtDocumentNumber, txtDocumentType, txtPhoneNumber, txtEmail;
    private JComboBox<BenefitType> cmbBenefitType;
    private JLabel lblFlightInfo, lblSeatInfo, lblPriceInfo;
    private JButton btnConfirmBooking, btnCancel;

    private final PassengerDAO passengerDAO;
    private final TicketDAO ticketDAO;
    private boolean bookingConfirmed = false;

    private static final DateTimeFormatter DIALOG_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Конструктор діалогу бронювання.
     * @param owner Батьківське вікно.
     * @param flight Обраний рейс.
     * @param seat Обране місце.
     * @param passengerDAO DAO для пасажирів.
     * @param ticketDAO DAO для квитків.
     */
    public BookingDialog(Frame owner, Flight flight, String seat, PassengerDAO passengerDAO, TicketDAO ticketDAO) {
        super(owner, "Бронювання квитка", true); // Модальне вікно
        this.selectedFlight = flight;
        this.selectedSeat = seat;
        this.passengerDAO = passengerDAO;
        this.ticketDAO = ticketDAO;

        initComponents();
        setFlightAndSeatInfo();

        pack(); // Автоматично підбирає розмір вікна
        setLocationRelativeTo(owner); // Центрує відносно батьківського вікна
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Закриття вікна звільняє ресурси
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10)); // Головний менеджер розмітки з відступами

        // --- Інформаційна панель ---
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // 0 рядків означає будь-яку кількість, 1 колонка
        infoPanel.setBorder(BorderFactory.createTitledBorder("Інформація про рейс та місце"));
        lblFlightInfo = new JLabel();
        lblSeatInfo = new JLabel();
        lblPriceInfo = new JLabel();
        infoPanel.add(lblFlightInfo);
        infoPanel.add(lblSeatInfo);
        infoPanel.add(lblPriceInfo);

        // --- Панель даних пасажира ---
        JPanel passengerPanel = new JPanel(new GridBagLayout());
        passengerPanel.setBorder(BorderFactory.createTitledBorder("Дані пасажира"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Відступи між комірками
        gbc.fill = GridBagConstraints.HORIZONTAL; // Розтягувати компоненти по горизонталі
        gbc.anchor = GridBagConstraints.WEST;    // Вирівнювання по лівому краю

        // ПІБ
        gbc.gridx = 0; gbc.gridy = 0; passengerPanel.add(new JLabel("ПІБ:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; txtFullName = new JTextField(25); passengerPanel.add(txtFullName, gbc);

        // Тип документа
        gbc.gridx = 0; gbc.gridy = 1; passengerPanel.add(new JLabel("Тип документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; txtDocumentType = new JTextField(20); passengerPanel.add(txtDocumentType, gbc);

        // Номер документа
        gbc.gridx = 0; gbc.gridy = 2; passengerPanel.add(new JLabel("Номер документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; txtDocumentNumber = new JTextField(15); passengerPanel.add(txtDocumentNumber, gbc);

        // Телефон
        gbc.gridx = 0; gbc.gridy = 3; passengerPanel.add(new JLabel("Телефон:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; txtPhoneNumber = new JTextField(15); passengerPanel.add(txtPhoneNumber, gbc);

        // Email (опціонально)
        gbc.gridx = 0; gbc.gridy = 4; passengerPanel.add(new JLabel("Email (опціонально):"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; txtEmail = new JTextField(25); passengerPanel.add(txtEmail, gbc);

        // Тип пільги
        gbc.gridx = 0; gbc.gridy = 5; passengerPanel.add(new JLabel("Пільга:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        cmbBenefitType = new JComboBox<>(BenefitType.values());
        cmbBenefitType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BenefitType) {
                    setText(((BenefitType) value).getDisplayName());
                }
                return this;
            }
        });
        cmbBenefitType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updatePriceInfo();
            }
        });
        passengerPanel.add(cmbBenefitType, gbc);

        // --- Панель кнопок ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Кнопки вирівняні праворуч
        btnConfirmBooking = new JButton("Підтвердити бронювання");
        btnCancel = new JButton("Скасувати");

        btnConfirmBooking.addActionListener(this::confirmBookingAction); // Посилання на метод
        btnCancel.addActionListener(e -> dispose()); // Лямбда для простого закриття

        buttonPanel.add(btnConfirmBooking);
        buttonPanel.add(btnCancel);

        // --- Додавання панелей до діалогу ---
        add(infoPanel, BorderLayout.NORTH);
        add(passengerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Встановлює інформацію про рейс, місце та початкову ціну.
     */
    private void setFlightAndSeatInfo() {
        String routeDesc = (selectedFlight.getRoute() != null) ? selectedFlight.getRoute().getFullRouteDescription() : "N/A";
        lblFlightInfo.setText(String.format("Рейс: %s (%s - %s)",
                routeDesc,
                selectedFlight.getDepartureDateTime().format(DIALOG_DATE_TIME_FORMATTER),
                selectedFlight.getArrivalDateTime().format(DIALOG_DATE_TIME_FORMATTER)
        ));
        lblSeatInfo.setText("Обране місце: " + selectedSeat);
        updatePriceInfo(); // Розрахувати та відобразити ціну
    }

    /**
     * Оновлює відображення ціни з урахуванням обраної пільги.
     */
    private void updatePriceInfo() {
        BenefitType benefit = (BenefitType) cmbBenefitType.getSelectedItem();
        BigDecimal finalPrice = calculatePriceWithBenefit(selectedFlight.getPricePerSeat(), benefit);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("uk", "UA")); // Український формат валюти
        lblPriceInfo.setText("Ціна до сплати: " + currencyFormat.format(finalPrice));
    }

    /**
     * Розраховує ціну квитка з урахуванням пільги пасажира.
     * @param basePrice Базова ціна квитка.
     * @param benefitType Тип пільги.
     * @return Кінцева ціна квитка.
     */
    private BigDecimal calculatePriceWithBenefit(BigDecimal basePrice, BenefitType benefitType) {
        if (basePrice == null) return BigDecimal.ZERO; // Захист від null
        if (benefitType == null || benefitType == BenefitType.NONE) {
            return basePrice;
        }
        double discountPercentage = 0.0;
        switch (benefitType) {
            case STUDENT: discountPercentage = 0.20; break;
            case PENSIONER: discountPercentage = 0.15; break;
            case COMBATANT: discountPercentage = 0.50; break;
            // default: // Не потрібно, оскільки NONE вже оброблено
        }
        return basePrice.multiply(BigDecimal.valueOf(1.0 - discountPercentage))
                .setScale(2, BigDecimal.ROUND_HALF_UP); // Округлення до 2 знаків після коми
    }

    /**
     * Обробляє подію натискання кнопки "Підтвердити бронювання".
     * Валідує дані, створює пасажира (або отримує існуючого) та бронює квиток.
     * @param event Подія натискання кнопки (не використовується безпосередньо, але потрібна для сигнатури ActionListener).
     */
    private void confirmBookingAction(ActionEvent event) {
        String fullName = txtFullName.getText().trim();
        String docType = txtDocumentType.getText().trim();
        String docNumber = txtDocumentNumber.getText().trim();
        String phone = txtPhoneNumber.getText().trim();
        String email = txtEmail.getText().trim();
        BenefitType benefit = (BenefitType) cmbBenefitType.getSelectedItem();

        if (fullName.isEmpty() || docType.isEmpty() || docNumber.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Будь ласка, заповніть всі обов'язкові поля пасажира (ПІБ, Тип та Номер документа, Телефон).", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Passenger passenger = new Passenger(0, fullName, docNumber, docType, phone, email.isEmpty() ? null : email, benefit);
            long passengerId = passengerDAO.addOrGetPassenger(passenger); // Може кинути SQLException
            passenger.setId(passengerId); // Встановлюємо отриманий ID

            BigDecimal finalPrice = calculatePriceWithBenefit(selectedFlight.getPricePerSeat(), benefit);
            Ticket newTicket = new Ticket(
                    0,
                    selectedFlight,
                    passenger,
                    selectedSeat,
                    LocalDateTime.now(),
                    finalPrice,
                    TicketStatus.BOOKED
            );
            newTicket.setBookingExpiryDateTime(LocalDateTime.now().plusHours(24));

            if (ticketDAO.addTicket(newTicket)) { // Може кинути SQLException або повернути false
                bookingConfirmed = true;
                JOptionPane.showMessageDialog(this, "Місце " + selectedSeat + " успішно заброньовано!\nТермін дії броні: " + newTicket.getBookingExpiryDateTime().format(DIALOG_DATE_TIME_FORMATTER), "Бронювання успішне", JOptionPane.INFORMATION_MESSAGE);
                dispose(); // Закрити діалог
            } else {
                // Цей блок може бути досягнутий, якщо addTicket() повертає false (наприклад, через конфлікт місця, який DAO обробляє без кидання SQLException)
                JOptionPane.showMessageDialog(this, "Не вдалося забронювати місце. Можливо, воно вже зайняте або сталася помилка збереження.", "Помилка бронювання", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            System.err.println("Помилка бази даних під час підтвердження бронювання: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Помилка під час взаємодії з базою даних: " + ex.getMessage(),
                    "Помилка бази даних",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception exGeneral) {
            System.err.println("Непередбачена помилка під час підтвердження бронювання: " + exGeneral.getMessage());
            exGeneral.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Сталася непередбачена помилка: " + exGeneral.getMessage(),
                    "Внутрішня помилка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Повертає {@code true}, якщо бронювання було успішно підтверджено.
     * @return {@code true} якщо бронювання підтверджено, інакше {@code false}.
     */
    public boolean isBookingConfirmed() {
        return bookingConfirmed;
    }
}