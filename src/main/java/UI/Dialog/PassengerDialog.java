package UI.Dialog;

import DAO.PassengerDAO;
import Models.Passenger;
import Models.Enums.BenefitType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

/**
 * Діалогове вікно для редагування інформації про пасажира.
 * Дозволяє змінювати персональні дані пасажира, такі як ПІБ, номер та тип документа,
 * контактну інформацію та тип пільги. Зміни зберігаються в базі даних.
 *
 * @version 1.1
 */
public class PassengerDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger("insurance.log");

    private Passenger currentPassenger;
    private PassengerDAO passengerDAO;
    private boolean saved = false;

    private JTextField txtFullName;
    private JTextField txtDocumentNumber;
    private JTextField txtDocumentType;
    private JTextField txtPhoneNumber;
    private JTextField txtEmail;
    private JComboBox<BenefitType> cmbBenefitType;

    /**
     * Конструктор для створення діалогового вікна редагування пасажира.
     *
     * @param owner батьківське вікно (фрейм), якому належить цей діалог.
     * @param passenger об'єкт {@link Passenger}, дані якого потрібно редагувати.
     * @param passengerDAO об'єкт {@link PassengerDAO} для доступу до даних пасажирів.
     * @throws IllegalArgumentException якщо passenger або passengerDAO є null.
     */
    public PassengerDialog(Frame owner, Passenger passenger, PassengerDAO passengerDAO) {
        super(owner, "Редагувати дані пасажира", true);
        logger.info("Ініціалізація діалогу PassengerDialog для пасажира: {}",
                (passenger != null ? "ID " + passenger.getId() : "null"));

        if (passenger == null) {
            logger.error("Критична помилка: об'єкт Passenger є null при ініціалізації PassengerDialog.");
            JOptionPane.showMessageDialog(null, "Помилка: Не передано дані пасажира для редагування.", "Критична помилка", JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("Пасажир не може бути null.");
        }
        if (passengerDAO == null) {
            logger.error("Критична помилка: об'єкт PassengerDAO є null при ініціалізації PassengerDialog.");
            JOptionPane.showMessageDialog(null, "Помилка: Не передано сервіс для роботи з даними пасажирів.", "Критична помилка", JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("PassengerDAO не може бути null.");
        }

        this.currentPassenger = passenger;
        this.passengerDAO = passengerDAO;

        initComponents();
        populateFields();

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        logger.debug("Діалог PassengerDialog успішно ініціалізовано та відображено.");
    }

    /**
     * Ініціалізує та розміщує компоненти користувацького інтерфейсу діалогового вікна.
     */
    private void initComponents() {
        logger.debug("Ініціалізація компонентів UI для PassengerDialog.");
        setLayout(new BorderLayout(10, 10));
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("ПІБ:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; txtFullName = new JTextField(25); formPanel.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Тип документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; txtDocumentType = new JTextField(20); formPanel.add(txtDocumentType, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Номер документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; txtDocumentNumber = new JTextField(15); formPanel.add(txtDocumentNumber, gbc);

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Телефон:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; txtPhoneNumber = new JTextField(15); formPanel.add(txtPhoneNumber, gbc);

        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; txtEmail = new JTextField(25); formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Пільга:"), gbc);
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
        formPanel.add(cmbBenefitType, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Зберегти");
        JButton btnCancel = new JButton("Скасувати");

        btnSave.addActionListener(this::savePassengerAction);
        btnCancel.addActionListener(e -> {
            logger.debug("Натиснуто кнопку 'Скасувати'. Закриття PassengerDialog.");
            dispose();
        });

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        logger.debug("Компоненти UI для PassengerDialog успішно створені та додані.");
    }

    /**
     * Заповнює поля форми даними поточного пасажира.
     */
    private void populateFields() {
        logger.debug("Заповнення полів даними пасажира ID: {}", currentPassenger.getId());
        if (currentPassenger != null) {
            txtFullName.setText(currentPassenger.getFullName());
            txtDocumentType.setText(currentPassenger.getDocumentType());
            txtDocumentNumber.setText(currentPassenger.getDocumentNumber());
            txtPhoneNumber.setText(currentPassenger.getPhoneNumber());
            txtEmail.setText(currentPassenger.getEmail() != null ? currentPassenger.getEmail() : "");
            cmbBenefitType.setSelectedItem(currentPassenger.getBenefitType());
            logger.trace("Поля заповнені.");
        } else {
            logger.warn("Спроба заповнити поля, але currentPassenger є null.");
        }
    }

    /**
     * Обробляє подію натискання кнопки "Зберегти".
     * @param e об'єкт події {@link ActionEvent}.
     */
    private void savePassengerAction(ActionEvent e) {
        logger.info("Спроба зберегти зміни для пасажира ID: {}", currentPassenger.getId());
        String fullName = txtFullName.getText().trim();
        String documentType = txtDocumentType.getText().trim();
        String documentNumber = txtDocumentNumber.getText().trim();
        String phoneNumber = txtPhoneNumber.getText().trim();
        String email = txtEmail.getText().trim();
        BenefitType selectedBenefit = (BenefitType) cmbBenefitType.getSelectedItem();

        logger.debug("Отримані дані з форми.");

        if (fullName.isEmpty() || documentType.isEmpty() || documentNumber.isEmpty() || phoneNumber.isEmpty()) {
            logger.warn("Помилка валідації: не заповнені обов'язкові поля.");
            JOptionPane.showMessageDialog(this, "Будь ласка, заповніть всі обов'язкові поля (ПІБ, Тип та Номер документа, Телефон).", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            logger.warn("Помилка валідації: некоректний формат Email: '{}'", email);
            JOptionPane.showMessageDialog(this, "Некоректний формат Email.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logger.debug("Валідація пройдена. Оновлення об'єкта Passenger ID: {}", currentPassenger.getId());
        currentPassenger.setFullName(fullName);
        currentPassenger.setDocumentType(documentType);
        currentPassenger.setDocumentNumber(documentNumber);
        currentPassenger.setPhoneNumber(phoneNumber);
        currentPassenger.setEmail(email.isEmpty() ? null : email);
        currentPassenger.setBenefitType(selectedBenefit);

        try {
            logger.debug("Спроба оновити дані пасажира в базі даних.");
            if (passengerDAO.updatePassenger(currentPassenger)) {
                saved = true;
                logger.info("Дані пасажира ID: {} успішно оновлено.", currentPassenger.getId());
                dispose();
            } else {
                logger.warn("Не вдалося оновити дані пасажира ID: {} в базі даних (DAO повернув false).", currentPassenger.getId());
                JOptionPane.showMessageDialog(this, "Не вдалося оновити дані пасажира в базі даних.", "Помилка збереження", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.error("Помилка бази даних під час оновлення даних пасажира ID: {}.", currentPassenger.getId(), ex);
            JOptionPane.showMessageDialog(this, "Помилка бази даних під час оновлення даних пасажира: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
        } catch (Exception exGeneral) {
            logger.error("Непередбачена помилка під час оновлення даних пасажира ID: {}.", currentPassenger.getId(), exGeneral);
            JOptionPane.showMessageDialog(this, "Сталася непередбачена помилка: " + exGeneral.getMessage(), "Внутрішня помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Перевіряє, чи були успішно збережені зміни в діалоговому вікні.
     *
     * @return {@code true}, якщо дані були збережені, {@code false} в іншому випадку.
     */
    public boolean isSaved() {
        logger.trace("Перевірка статусу збереження для PassengerDialog: {}", saved);
        return saved;
    }
}