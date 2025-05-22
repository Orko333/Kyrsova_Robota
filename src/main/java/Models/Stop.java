package Models;

import java.util.Objects;

/**
 * Клас, що представляє зупинку в маршруті.
 * Кожна зупинка характеризується унікальним ідентифікатором, назвою (наприклад, назва автостанції або конкретного місця зупинки)
 * та містом, в якому вона розташована.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class Stop {
    /**
     * Унікальний ідентифікатор зупинки.
     */
    private long id;
    /**
     * Назва зупинки (наприклад, "Автовокзал Центральний", "Зупинка Площа Ринок").
     */
    private String name;
    /**
     * Місто, в якому знаходиться зупинка (наприклад, "Київ", "Львів").
     */
    private String city;

    /**
     * Конструктор для створення об'єкта Зупинка.
     *
     * @param id унікальний ідентифікатор зупинки.
     * @param name назва зупинки.
     * @param city місто, в якому розташована зупинка.
     */
    public Stop(long id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    // Getters and Setters

    /**
     * Повертає унікальний ідентифікатор зупинки.
     * @return {@code long} значення ідентифікатора.
     */
    public long getId() {
        return id;
    }

    /**
     * Встановлює унікальний ідентифікатор зупинки.
     * @param id новий ідентифікатор зупинки.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Повертає назву зупинки.
     * @return {@code String} назва зупинки.
     */
    public String getName() {
        return name;
    }

    /**
     * Встановлює назву зупинки.
     * @param name нова назва зупинки.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Повертає місто, в якому знаходиться зупинка.
     * @return {@code String} назва міста.
     */
    public String getCity() {
        return city;
    }

    /**
     * Встановлює місто, в якому знаходиться зупинка.
     * @param city нова назва міста.
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Повертає рядкове представлення об'єкта {@code Stop}.
     * Формат: "НазваЗупинки (Місто)".
     * @return {@code String} рядкове представлення зупинки.
     */
    @Override
    public String toString() {
        return name + " (" + city + ")";
    }

    /**
     * Порівнює поточний об'єкт {@code Stop} з іншим об'єктом.
     * Дві зупинки вважаються рівними, якщо їхні ідентифікатори ({@code id}) однакові.
     *
     * @param o об'єкт для порівняння.
     * @return {@code true}, якщо об'єкти рівні (мають однаковий {@code id}),
     *         {@code false} в іншому випадку.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return id == stop.id;
    }

    /**
     * Повертає хеш-код для об'єкта {@code Stop}.
     * Хеш-код базується на ідентифікаторі ({@code id}) зупинки.
     *
     * @return {@code int} хеш-код об'єкта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}