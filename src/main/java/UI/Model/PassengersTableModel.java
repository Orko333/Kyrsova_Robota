package UI.Model;

import Models.Passenger;
import Models.Enums.BenefitType; // Необхідно імпортувати, якщо getBenefitType() повертає цей тип

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель таблиці для відображення інформації про пасажирів.
 * Цей клас розширює {@link AbstractTableModel} і надає дані для {@link javax.swing.JTable},
 * відображаючи деталі кожного пасажира, такі як ID, ПІБ, інформація про документ,
 * контактні дані та тип пільги.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class PassengersTableModel extends AbstractTableModel {
    /**
     * Список об'єктів {@link Passenger}, що відображаються в таблиці.
     */
    private List<Passenger> passengers;
    /**
     * Масив назв стовпців таблиці пасажирів.
     */
    private final String[] columnNames = {"ID", "ПІБ", "Документ", "Номер документа", "Телефон", "Email", "Пільга"};

    /**
     * Конструктор для створення моделі таблиці пасажирів.
     * Ініціалізує модель наданим списком пасажирів. Якщо список {@code null},
     * створюється порожній список.
     *
     * @param passengers список об'єктів {@link Passenger} для відображення.
     */
    public PassengersTableModel(List<Passenger> passengers) {
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
    }

    /**
     * Встановлює новий список пасажирів для моделі.
     * Оновлює внутрішній список пасажирів та сповіщає таблицю про зміну даних,
     * що призводить до перемальовування таблиці.
     *
     * @param passengers новий список об'єктів {@link Passenger}.
     */
    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
        fireTableDataChanged(); // Сповіщення таблиці про оновлення даних
    }

    /**
     * Повертає об'єкт {@link Passenger} за вказаним індексом рядка.
     *
     * @param rowIndex індекс рядка в таблиці.
     * @return об'єкт {@link Passenger} з відповідного рядка, або {@code null}, якщо індекс виходить за межі.
     */
    public Passenger getPassengerAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < passengers.size()) {
            return passengers.get(rowIndex);
        }
        return null;
    }

    /**
     * Повертає кількість рядків у моделі таблиці.
     * Кількість рядків відповідає кількості пасажирів у списку.
     *
     * @return кількість рядків.
     */
    @Override
    public int getRowCount() {
        return passengers.size();
    }

    /**
     * Повертає кількість стовпців у моделі таблиці.
     * Кількість стовпців визначається масивом {@code columnNames}.
     *
     * @return кількість стовпців.
     */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Повертає назву стовпця за його індексом.
     *
     * @param column індекс стовпця.
     * @return назва стовпця.
     */
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    /**
     * Повертає значення для комірки таблиці за вказаними індексами рядка та стовпця.
     * Визначає, які дані з об'єкта {@link Passenger} відображати в кожному стовпці.
     *
     * @param rowIndex індекс рядка.
     * @param columnIndex індекс стовпця.
     * @return об'єкт, що представляє значення комірки.
     *         Повертає {@code null}, якщо індекс стовпця невідомий або дані відсутні.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Passenger p = passengers.get(rowIndex);
        if (p == null) { // Додаткова перевірка, хоча get() з ArrayList зазвичай не повертає null для існуючих індексів
            return "N/A"; // або інший плейсхолдер
        }

        switch (columnIndex) {
            case 0: // ID
                return p.getId();
            case 1: // ПІБ
                return p.getFullName();
            case 2: // Документ (тип)
                return p.getDocumentType();
            case 3: // Номер документа
                return p.getDocumentNumber();
            case 4: // Телефон
                return p.getPhoneNumber();
            case 5: // Email
                return p.getEmail() != null ? p.getEmail() : "-"; // Відображаємо "-" якщо email відсутній
            case 6: // Пільга
                BenefitType benefitType = p.getBenefitType();
                // Передбачається, що Enum BenefitType має метод getDisplayName()
                return benefitType != null ? benefitType.getDisplayName() : "Без пільг";
            default:
                return null; // Для невідомих індексів стовпців
        }
    }
}