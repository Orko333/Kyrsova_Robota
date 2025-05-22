package Models;

import java.util.List;
import java.util.Objects;

/**
 * Клас, що представляє маршрут рейсу.
 * Маршрут складається з пункту відправлення, пункту призначення та списку проміжних зупинок.
 *
 * @author [Ваше ім'я або назва команди] // Додайте автора, якщо потрібно
 * @version 1.0 // Додайте версію, якщо потрібно
 */
public class Route {
    /**
     * Унікальний ідентифікатор маршруту.
     */
    private long id;
    /**
     * Зупинка, що є пунктом відправлення маршруту.
     * @see Stop
     */
    private Stop departureStop;
    /**
     * Зупинка, що є пунктом призначення маршруту.
     * @see Stop
     */
    private Stop destinationStop;
    /**
     * Список проміжних зупинок на маршруті. Може бути порожнім, якщо проміжних зупинок немає.
     * @see Stop
     */
    private List<Stop> intermediateStops;

    /**
     * Конструктор для створення об'єкта Маршрут.
     *
     * @param id унікальний ідентифікатор маршруту.
     * @param departureStop зупинка відправлення.
     * @param destinationStop зупинка призначення.
     * @param intermediateStops список проміжних зупинок (може бути {@code null} або порожнім).
     */
    public Route(long id, Stop departureStop, Stop destinationStop, List<Stop> intermediateStops) {
        this.id = id;
        this.departureStop = departureStop;
        this.destinationStop = destinationStop;
        this.intermediateStops = intermediateStops;
    }

    // Getters and Setters

    /**
     * Повертає унікальний ідентифікатор маршруту.
     * @return {@code long} значення ідентифікатора.
     */
    public long getId() {
        return id;
    }

    /**
     * Встановлює унікальний ідентифікатор маршруту.
     * @param id новий ідентифікатор маршруту.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Повертає зупинку відправлення маршруту.
     * @return {@link Stop} об'єкт зупинки відправлення.
     */
    public Stop getDepartureStop() {
        return departureStop;
    }

    /**
     * Встановлює зупинку відправлення маршруту.
     * @param departureStop нова зупинка відправлення.
     */
    public void setDepartureStop(Stop departureStop) {
        this.departureStop = departureStop;
    }

    /**
     * Повертає зупинку призначення маршруту.
     * @return {@link Stop} об'єкт зупинки призначення.
     */
    public Stop getDestinationStop() {
        return destinationStop;
    }

    /**
     * Встановлює зупинку призначення маршруту.
     * @param destinationStop нова зупинка призначення.
     */
    public void setDestinationStop(Stop destinationStop) {
        this.destinationStop = destinationStop;
    }

    /**
     * Повертає список проміжних зупинок на маршруті.
     * @return {@code List<Stop>} список проміжних зупинок. Може бути порожнім.
     */
    public List<Stop> getIntermediateStops() {
        return intermediateStops;
    }

    /**
     * Встановлює список проміжних зупинок на маршруті.
     * @param intermediateStops новий список проміжних зупинок (може бути {@code null} або порожнім).
     */
    public void setIntermediateStops(List<Stop> intermediateStops) {
        this.intermediateStops = intermediateStops;
    }

    /**
     * Повертає повний опис маршруту у вигляді рядка.
     * Формат: "МістоВідправлення -> МістоПроміжноїЗупинки1 -> ... -> МістоПризначення".
     *
     * @return {@code String} рядок, що описує маршрут.
     */
    public String getFullRouteDescription() {
        StringBuilder sb = new StringBuilder();
        // Передбачається, що клас Stop має метод getCity()
        sb.append(departureStop.getCity()).append(" -> ");
        if (intermediateStops != null && !intermediateStops.isEmpty()) {
            for (Stop stop : intermediateStops) {
                sb.append(stop.getCity()).append(" -> ");
            }
        }
        sb.append(destinationStop.getCity());
        return sb.toString();
    }

    /**
     * Повертає рядкове представлення об'єкта {@code Route}.
     * Формат: "Маршрут [id]: [Повний опис маршруту]".
     * @return {@code String} рядкове представлення маршруту.
     */
    @Override
    public String toString() {
        return "Маршрут " + id + ": " + getFullRouteDescription();
    }

    /**
     * Порівнює поточний об'єкт {@code Route} з іншим об'єктом.
     * Два маршрути вважаються рівними, якщо їхні ідентифікатори ({@code id}) однакові.
     *
     * @param o об'єкт для порівняння.
     * @return {@code true}, якщо об'єкти рівні (мають однаковий {@code id}),
     *         {@code false} в іншому випадку.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return id == route.id;
    }

    /**
     * Повертає хеш-код для об'єкта {@code Route}.
     * Хеш-код базується на ідентифікаторі ({@code id}) маршруту.
     *
     * @return {@code int} хеш-код об'єкта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}