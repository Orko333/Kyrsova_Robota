package UI.Dialog;

import DAO.*;
import Models.*;
import Models.Enums.BenefitType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException; // Для обробки

/**
 * Діалогове вікно для редагування даних пасажира.
 */
public class PassengerDialog extends JDialog {
    private Passenger currentPassenger;
    private PassengerDAO passengerDAO;
    private boolean saved = false;

    private JTextField txtFullName, txtDocumentNumber, txtDocumentType, txtPhoneNumber, txtEmail;
    private JComboBox<BenefitType> cmbBenefitType;

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

    private void initComponents() {
        // Схоже на BookingDialog, але для пасажира
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
        // ... (інші поля: Тип документа, Номер документа, Телефон, Email) ...
        // Для стислості, скопіюйте відповідні частини з BookingDialog
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
        cmbBenefitType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BenefitType) setText(((BenefitType) value).getDisplayName());
                return this;
            }
        });
        formPanel.add(cmbBenefitType, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Зберегти");
        JButton btnCancel = new JButton("Скасувати");

        btnSave.addActionListener(this::savePassengerAction);
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateFields() {
        txtFullName.setText(currentPassenger.getFullName());
        txtDocumentType.setText(currentPassenger.getDocumentType());
        txtDocumentNumber.setText(currentPassenger.getDocumentNumber());
        txtPhoneNumber.setText(currentPassenger.getPhoneNumber());
        txtEmail.setText(currentPassenger.getEmail() != null ? currentPassenger.getEmail() : "");
        cmbBenefitType.setSelectedItem(currentPassenger.getBenefitType());
    }

    private void savePassengerAction(ActionEvent e) {
        // Валідація
        String fullName = txtFullName.getText().trim();
        // ... (інші поля) ...
        if (fullName.isEmpty() /* || інші перевірки */) {
            JOptionPane.showMessageDialog(this, "ПІБ не може бути порожнім.", "Помилка валідації", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentPassenger.setFullName(fullName);
        currentPassenger.setDocumentType(txtDocumentType.getText().trim());
        currentPassenger.setDocumentNumber(txtDocumentNumber.getText().trim());
        currentPassenger.setPhoneNumber(txtPhoneNumber.getText().trim());
        currentPassenger.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
        currentPassenger.setBenefitType((BenefitType) cmbBenefitType.getSelectedItem());

        try {
            if (passengerDAO.updatePassenger(currentPassenger)) {
                saved = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Не вдалося оновити дані пасажира.", "Помилка збереження", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Помилка бази даних при оновленні: " + ex.getMessage(), "Помилка БД", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public boolean isSaved() { return saved; }
}


