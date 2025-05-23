package Models.Enums;

/**
 * Перелік можливих статусів рейсу.
 * <p>
 * Цей enum визначає різні стани, в яких може перебувати авіарейс
 * протягом свого життєвого циклу, від планування до прибуття.
 * Кожен статус має асоційоване відображуване ім'я (displayName),
 * яке використовується для представлення статусу користувачеві.
 * </p>
 *
 * @author [Ваше ім'я або назва команди, якщо доречно]
 * @version 1.0
 */
public enum FlightStatus {
    /**
     * Рейс запланований, але ще не розпочався.
     * Відображуване ім'я: "Запланований".
     */
    PLANNED("Запланований"),

    /**
     * Рейс відкладено, час відправлення змінено на пізніший.
     * Відображуване ім'я: "Відкладений".
     */
    DELAYED("Відкладений"),

    /**
     * Рейс скасовано, він не відбудеться.
     * Відображуване ім'я: "Скасований".
     */
    CANCELLED("Скасований"),

    /**
     * Літак відправився з аеропорту вильоту.
     * Відображуване ім'я: "Відправлений".
     */
    DEPARTED("Відправлений"),

    /**
     * Літак прибув до аеропорту призначення.
     * Відображуване ім'я: "Прибув".
     */
    ARRIVED("Прибув");

    /**
     * Відображуване ім'я статусу рейсу, призначене для показу користувачеві.
     * Наприклад, "Запланований", "Відкладений".
     */
    private final String displayName;

    /**
     * Конструктор для ініціалізації константи переліку з її відображуваним ім'ям.
     *
     * @param displayName Рядок, що представляє відображуване ім'я для статусу рейсу.
     */
    FlightStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Повертає відображуване ім'я статусу рейсу.
     * Це ім'я використовується для представлення статусу рейсу в інтерфейсі користувача.
     *
     * @return Рядок, що є відображуваним ім'ям статусу рейсу (наприклад, "Відкладений").
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Повертає рядкове представлення об'єкта, яке є його відображуваним ім'ям.
     * Перевизначений для зручності відображення та відлагодження.
     *
     * @return Відображуване ім'я статусу рейсу (наприклад, "Прибув").
     */
    @Override
    public String toString() {
        return displayName;
    }
}