package Models;

import Models.Enums.BenefitType;

import java.util.Objects;

/**
 * Клас, що представляє пасажира.
 * Містить персональні дані пасажира та інформацію про пільги.
 * Історія поїздок буде реалізована через запити до квитків.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class Passenger {
    /**
     * Унікальний ідентифікатор пасажира.
     */
    private long id;
    /**
     * Повне ім'я та прізвище пасажира (ПІБ).
     */
    private String fullName;
    /**
     * Номер документа, що посвідчує особу (наприклад, паспорт, студентський квиток).
     */
    private String documentNumber;
    /**
     * Тип документа, що посвідчує особу.
     */
    private String documentType;
    /**
     * Номер телефону пасажира.
     */
    private String phoneNumber;
    /**
     * Адреса електронної пошти пасажира (необов'язкове поле).
     */
    private String email;
    /**
     * Тип пільги, якою користується пасажир.
     * @see Models.Enums.BenefitType
     */
    private BenefitType benefitType;

    /**
     * Конструктор для створення об'єкта Пасажир з усіма необхідними даними.
     *
     * @param id унікальний ідентифікатор пасажира.
     * @param fullName повне ім'я та прізвище пасажира.
     * @param documentNumber номер документа, що посвідчує особу.
     * @param documentType тип документа, що посвідчує особу.
     * @param phoneNumber номер телефону пасажира.
     * @param email адреса електронної пошти пасажира (може бути {@code null}).
     * @param benefitType тип пільги, якою користується пасажир.
     */
    public Passenger(long id, String fullName, String documentNumber, String documentType,
                     String phoneNumber, String email, BenefitType benefitType) {
        this.id = id;
        this.fullName = fullName;
        this.documentNumber = documentNumber;
        this.documentType = documentType;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.benefitType = benefitType;
    }

    // Getters and Setters

    /**
     * Повертає унікальний ідентифікатор пасажира.
     * @return {@code long} значення ідентифікатора.
     */
    public long getId() {
        return id;
    }

    /**
     * Встановлює унікальний ідентифікатор пасажира.
     * @param id новий ідентифікатор пасажира.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Повертає повне ім'я та прізвище пасажира.
     * @return {@code String} ПІБ пасажира.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Встановлює повне ім'я та прізвище пасажира.
     * @param fullName нове ПІБ пасажира.
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Повертає номер документа пасажира.
     * @return {@code String} номер документа.
     */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Встановлює номер документа пасажира.
     * @param documentNumber новий номер документа.
     */
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    /**
     * Повертає тип документа пасажира.
     * @return {@code String} тип документа.
     */
    public String getDocumentType() {
        return documentType;
    }

    /**
     * Встановлює тип документа пасажира.
     * @param documentType новий тип документа.
     */
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    /**
     * Повертає номер телефону пасажира.
     * @return {@code String} номер телефону.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Встановлює номер телефону пасажира.
     * @param phoneNumber новий номер телефону.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Повертає адресу електронної пошти пасажира.
     * @return {@code String} адреса електронної пошти, або {@code null}, якщо не вказана.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Встановлює адресу електронної пошти пасажира.
     * @param email нова адреса електронної пошти (може бути {@code null}).
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Повертає тип пільги пасажира.
     * @return {@link BenefitType} тип пільги.
     */
    public BenefitType getBenefitType() {
        return benefitType;
    }

    /**
     * Встановлює тип пільги пасажира.
     * @param benefitType новий тип пільги.
     */
    public void setBenefitType(BenefitType benefitType) {
        this.benefitType = benefitType;
    }

    /**
     * Повертає рядкове представлення об'єкта {@code Passenger}.
     * Формат: "ПІБ (Док.: ТипДокумента НомерДокумента)".
     * @return {@code String} рядкове представлення пасажира.
     */
    @Override
    public String toString() {
        return fullName + " (Док.: " + documentType + " " + documentNumber + ")";
    }

    /**
     * Порівнює поточний об'єкт {@code Passenger} з іншим об'єктом.
     * Два пасажири вважаються рівними, якщо їхні ідентифікатори ({@code id}) однакові.
     *
     * @param o об'єкт для порівняння.
     * @return {@code true}, якщо об'єкти рівні (мають однаковий {@code id}),
     *         {@code false} в іншому випадку.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passenger passenger = (Passenger) o;
        return id == passenger.id;
    }

    /**
     * Повертає хеш-код для об'єкта {@code Passenger}.
     * Хеш-код базується на ідентифікаторі ({@code id}) пасажира.
     *
     * @return {@code int} хеш-код об'єкта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}