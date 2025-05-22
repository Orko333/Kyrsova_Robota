package Models.Enums;

/**
 * Перелік можливих статусів рейсу.
 */
public enum FlightStatus {
    PLANNED("Запланований"),
    DELAYED("Відкладений"),
    CANCELLED("Скасований"),
    DEPARTED("Відправлений"),
    ARRIVED("Прибув");

    private final String displayName;

    FlightStatus(String displayName) {
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
