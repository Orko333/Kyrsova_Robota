package Models.Enums;

/**
 * Перелік можливих типів пільг для пасажирів.
 */
public enum BenefitType {
    NONE("Без пільг"),
    STUDENT("Студент"),
    PENSIONER("Пенсіонер"),
    COMBATANT("Учасник бойових дій");

    private final String displayName;

    BenefitType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
