package Models;

import Models.Enums.BenefitType;

import java.util.Objects;

/**
 * Клас, що представляє пасажира.
 * Містить персональні дані пасажира та інформацію про пільги.
 * Історія поїздок буде реалізована через запити до квитків.
 */
public class Passenger {
    private long id;
    private String fullName; // ПІБ
    private String documentNumber; // Номер документа (паспорт, студентський тощо)
    private String documentType; // Тип документа
    private String phoneNumber;
    private String email; // Опціонально
    private BenefitType benefitType;

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
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BenefitType getBenefitType() {
        return benefitType;
    }

    public void setBenefitType(BenefitType benefitType) {
        this.benefitType = benefitType;
    }

    @Override
    public String toString() {
        return fullName + " (Док.: " + documentType + " " + documentNumber + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passenger passenger = (Passenger) o;
        return id == passenger.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
