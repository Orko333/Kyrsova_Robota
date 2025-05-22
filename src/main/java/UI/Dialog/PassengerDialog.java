package UI.Dialog;

import DAO.*;
import Models.*;
import Models.Enums.BenefitType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

/**
 * Діалогове вікно для редагування інформації про пасажира.
 * Дозволяє змінювати персональні дані пасажира, такі як ПІБ, номер та тип документа,
 * контактну інформацію та тип пільги. Зміни зберігаються в базі даних.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class PassengerDialog extends JDialog {
    /**
     * Об'єкт пасажира, дані якого редагуються.
     */
    private Passenger currentPassenger;
    /**
     * Об'єкт доступу до даних (DAO) для взаємодії з таблицею пасажирів у базі даних.
     */
    private PassengerDAO passengerDAO;
    /**
     * Прапорець, що вказує, чи були успішно збережені зміни.
     */
    private boolean saved = false;

    /**
     * Текстове поле для введення ПІБ пасажира.
     */
    private JTextField txtFullName;
    /**
     * Текстове поле для введення номера документа пасажира.
     */
    private JTextField txtDocumentNumber;
    /**
     * Текстове поле для введення типу документа пасажира.
     */
    private JTextField txtDocumentType;
    /**
     * Текстове поле для введення номера телефону пасажира.
     */
    private JTextField txtPhoneNumber;
    /**
     * Текстове поле для введення адреси електронної пошти пасажира.
     */
    private JTextField txtEmail;
    /**
     * Випадаючий список для вибору типу пільги пасажира.
     */
    private JComboBox<BenefitType> cmbBenefitType;

    /**
     * Конструктор для створення діалогового вікна редагування пасажира.
     *
     * @param owner батьківське вікно (фрейм), якому належить цей діалог.
     * @param passenger об'єкт {@link Passenger}, дані якого потрібно редагувати.
     *                  Якщо {@code null}, може означати створення нового пасажира (але поточна логіка передбачає редагування).
     * @param passengerDAO об'єкт {@link PassengerDAO} для доступу до даних пасажирів.
     */
    public PassengerDialog(Frame owner, Passenger passenger, PassengerDAO passengerDAO) {
        super(owner, "Редагувати дані пасажира", true);
        this.currentPassenger = passenger;
        this.passengerDAO = passengerDAO;

        initComponents();
        populateFields();

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    /**
     * Ініціалізує та розміщує компоненти користувацького інтерфейсу діалогового вікна.
     * Створює текстові поля для даних пасажира, випадаючий список для пільг та кнопки "Зберегти" і "Скасувати".
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // ПІБ
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("ПІБ:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; txtFullName = new JTextField(25); formPanel.add(txtFullName, gbc);

        // Поле: Тип документа
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Тип документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; txtDocumentType = new JTextField(20); formPanel.add(txtDocumentType, gbc);

        // Поле: Номер документа
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Номер документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; txtDocumentNumber = new JTextField(15); formPanel.add(txtDocumentNumber, gbc);

        // Поле: Телефон
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Телефон:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; txtPhoneNumber = new JTextField(15); formPanel.add(txtPhoneNumber, gbc);

        // Поле: Email (опціонально)
        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; txtEmail = new JTextField(25); formPanel.add(txtEmail, gbc);

        // Тип пільги
        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Пільга:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        cmbBenefitType = new JComboBox<>(BenefitType.values());
        // Налаштування відображення назв пільг у JComboBox
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

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Зберегти");
        JButton btnCancel = new JButton("Скасувати");

        btnSave.addActionListener(this::savePassengerAction);
        btnCancel.addActionListener(e -> dispose()); // Закриває діалогове вікно

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Заповнює поля форми даними поточного пасажира {@code currentPassenger}.
     * Якщо пасажир не {@code null}, його дані відображаються у відповідних текстових полях та JComboBox.
     */
    private void populateFields() {
        if (currentPassenger != null) {
            txtFullName.setText(currentPassenger.getFullName());
            txtDocumentType.setText(currentPassenger.getDocumentType());
            txtDocumentNumber.setText(currentPassenger.getDocumentNumber());
            txtPhoneNumber.setText(currentPassenger.getPhoneNumber());
            txtEmail.setText(currentPassenger.getEmail() != null ? currentPassenger.getEmail() : "");
            cmbBenefitType.setSelectedItem(currentPassenger.getBenefitType());
        }
    }

    /**
     * Обробляє подію натискання кнопки "Зберегти".
     * Проводить валідацію введених даних. Якщо дані коректні,
     * оновлює об'єкт {@code currentPassenger} та намагається зберегти зміни в базі даних
     * через {@code passengerDAO}. У разі успішного збереження встановлює прапорець {@code saved} в {@code true}
     * та закриває діалогове вікно. У випадку помилки показує відповідне повідомлення.
     *
     * @param e об'єкт події {@link ActionEvent}, що викликав цей метод (натискання кнопки).
     */
    private void savePassengerAction(ActionEvent e) {
        // Валідація введених даних
        String fullName = txtFullName.getText().trim();
        String documentType = txtDocumentType.getText().trim();
        String documentNumber = txtDocumentNumber.getText().trim();
        String phoneNumber = txtPhoneNumber.getText().trim();
        String email = txtEmail.getText().trim();

        // Приклад простої валідації. Розширте за потреби.
        if (fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Поле 'ПІБ' не може бути порожнім.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (documentType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Поле 'Тип документа' не може бути порожнім.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (documentNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Поле 'Номер документа' не може бути порожнім.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (phoneNumber.isEmpty()) { // Додайте більш складну валідацію номера телефону, якщо потрібно
            JOptionPane.showMessageDialog(this, "Поле 'Телефон' не може бути порожнім.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Валідація Email може бути складнішою, тут простий приклад
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Некоректний формат Email.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }


        // Оновлення даних об'єкта пасажира
        currentPassenger.setFullName(fullName);
        currentPassenger.setDocumentType(documentType);
        currentPassenger.setDocumentNumber(documentNumber);
        currentPassenger.setPhoneNumber(phoneNumber);
        currentPassenger.setEmail(email.isEmpty() ? null : email); // Встановлюємо null, якщо поле email порожнє
        currentPassenger.setBenefitType((BenefitType) cmbBenefitType.getSelectedItem());

        try {
            // Спроба оновити дані в базі
            if (passengerDAO.updatePassenger(currentPassenger)) {
                saved = true; // Позначити, що збереження було успішним
                dispose();    // Закрити діалогове вікно
            } else {
                JOptionPane.showMessageDialog(this, "Не вдалося оновити дані пасажира в базі даних.", "Помилка збереження", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            // Обробка помилок SQL
            JOptionPane.showMessageDialog(this, "Помилка бази даних під час оновлення даних пасажира: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(); // Логування винятку для розробника
        }
    }

    /**
     * Перевіряє, чи були успішно збережені зміни в діалоговому вікні.
     *
     * @return {@code true}, якщо дані були збережені, {@code false} в іншому випадку.
     */
    public boolean isSaved() {
        return saved;
    }
}