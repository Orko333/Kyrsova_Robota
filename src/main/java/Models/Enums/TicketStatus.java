package Models.Enums;

/**
 * Перелік можливих статусів квитка.
 */
public enum TicketStatus {
    BOOKED("Заброньований"),
    SOLD("Проданий"),
    CANCELLED("Скасований"),
    USED("Використаний"); // Додано для історії поїздок

    private final String displayName;

    TicketStatus(String displayName) {
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
