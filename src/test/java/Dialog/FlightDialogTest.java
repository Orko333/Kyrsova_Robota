package Dialog; // Або ваш актуальний пакет

import DAO.FlightDAO;
import DAO.RouteDAO;
import Models.Enums.FlightStatus;
import Models.Flight;
import Models.Route;
import Models.Stop;

import UI.Dialog.FlightDialog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightDialogTest {

    @Mock
    private FlightDAO mockFlightDAO;
    @Mock
    private RouteDAO mockRouteDAO;

    private JFrame testOwnerFrame;
    private FlightDialog flightDialog;
    private Flight flightToEdit;
    private List<Route> availableRoutes;

    @Captor
    private ArgumentCaptor<Flight> flightCaptor;

    private static final DateTimeFormatter INPUT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @BeforeEach
    void setUp() throws SQLException { // Додано throws SQLException для мокінгу DAO
        FlightDialog.setSuppressMessagesForTesting(true); // Придушуємо JOptionPane

        testOwnerFrame = new JFrame();

        // Створюємо тестові маршрути
        Stop stopA = new Stop(1, "Місто А", "Вокзал А");
        Stop stopB = new Stop(2, "Місто Б", "Вокзал Б");
        Stop stopC = new Stop(3, "Місто В", "Вокзал В");
        Route route1 = new Route(101, stopA, stopB, Collections.emptyList());
        Route route2 = new Route(102, stopB, stopC, Collections.singletonList(stopA));
        availableRoutes = new ArrayList<>(List.of(route1, route2));

        // Мокуємо завантаження маршрутів
        when(mockRouteDAO.getAllRoutes()).thenReturn(availableRoutes);

        // Створюємо тестовий рейс для редагування (опціонально, для деяких тестів)
        flightToEdit = new Flight(
                1L, route1,
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 1, 12, 0),
                50, FlightStatus.PLANNED, "TestBus 123", new BigDecimal("100.00")
        );

        flightDialog = null; // Скидаємо перед кожним тестом
    }

    @AfterEach
    void tearDown() {
        FlightDialog.setSuppressMessagesForTesting(false); // Відновлюємо

        if (flightDialog != null) {
            final FlightDialog currentDialog = flightDialog;
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (currentDialog.isDisplayable()) {
                        currentDialog.dispose();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error disposing flightDialog in tearDown: " + e.getMessage());
            }
            flightDialog = null;
        }
        if (testOwnerFrame != null) {
            testOwnerFrame.dispose();
            testOwnerFrame = null;
        }
    }

    private void initializeDialog(Flight flight) { // Приймає flightToEdit або null
        try {
            SwingUtilities.invokeAndWait(() -> {
                flightDialog = new FlightDialog(testOwnerFrame, "Test Flight Dialog", mockFlightDAO, mockRouteDAO, flight);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("Failed to initialize FlightDialog: " + cause.getMessage(), cause);
        }
    }

    @Test
    @DisplayName("Конструктор: успішна ініціалізація для нового рейсу")
    void constructor_newFlight_successfulInitialization() {
        initializeDialog(null); // Новий рейс
        assertNotNull(flightDialog);
        assertEquals("Test Flight Dialog", flightDialog.getTitle());
        assertEquals(availableRoutes.size(), flightDialog.getCmbRoute().getItemCount());
        assertEquals(FlightStatus.PLANNED, flightDialog.getCmbStatus().getSelectedItem()); // Перевірка статусу за замовчуванням
        assertTrue(flightDialog.getCmbRoute().isEnabled());
    }

    @Test
    @DisplayName("Конструктор: успішна ініціалізація для редагування рейсу")
    void constructor_editFlight_successfulInitialization() {
        initializeDialog(flightToEdit); // Редагування
        assertNotNull(flightDialog);

        // Перевірка заповнення полів
        Route selectedInCombo = (Route) flightDialog.getCmbRoute().getSelectedItem();
        assertNotNull(selectedInCombo);
        assertEquals(flightToEdit.getRoute().getId(), selectedInCombo.getId());

        assertEquals(flightToEdit.getDepartureDateTime().format(INPUT_DATE_TIME_FORMATTER), flightDialog.getTxtDepartureDateTime().getText());
        assertEquals(flightToEdit.getArrivalDateTime().format(INPUT_DATE_TIME_FORMATTER), flightDialog.getTxtArrivalDateTime().getText());
        assertEquals(String.valueOf(flightToEdit.getTotalSeats()), flightDialog.getTxtTotalSeats().getText());
        assertEquals(flightToEdit.getBusModel(), flightDialog.getTxtBusModel().getText());
        assertEquals(flightToEdit.getPricePerSeat().toString(), flightDialog.getTxtPricePerSeat().getText());
        assertEquals(flightToEdit.getStatus(), flightDialog.getCmbStatus().getSelectedItem());
    }

    @Test
    @DisplayName("Завантаження маршрутів: порожній список")
    void loadRoutesIntoComboBox_emptyList() throws SQLException {
        when(mockRouteDAO.getAllRoutes()).thenReturn(new ArrayList<>());
        initializeDialog(null);
        // JOptionPane буде придушено, але ми можемо перевірити стан cmbRoute
        assertFalse(flightDialog.getCmbRoute().isEnabled());
        assertEquals(1, flightDialog.getCmbRoute().getItemCount()); // Має бути один null елемент
        assertNull(flightDialog.getCmbRoute().getItemAt(0));
    }

    @Test
    @DisplayName("Завантаження маршрутів: помилка SQLException")
    void loadRoutesIntoComboBox_sqlException() throws SQLException {
        when(mockRouteDAO.getAllRoutes()).thenThrow(new SQLException("DB error loading routes"));
        initializeDialog(null);
        assertFalse(flightDialog.getCmbRoute().isEnabled());
    }

    // --- Тести для saveFlightAction ---

    private void fillValidNewFlightData() {
        flightDialog.getCmbRoute().setSelectedItem(availableRoutes.get(0));
        flightDialog.getTxtDepartureDateTime().setText("2025-02-01 10:00");
        flightDialog.getTxtArrivalDateTime().setText("2025-02-01 12:00");
        flightDialog.getTxtTotalSeats().setText("30");
        flightDialog.getTxtBusModel().setText("New Bus");
        flightDialog.getTxtPricePerSeat().setText("150.50");
        flightDialog.getCmbStatus().setSelectedItem(FlightStatus.PLANNED);
    }

    private void fillValidEditFlightData() {
        flightDialog.getCmbRoute().setSelectedItem(availableRoutes.get(1)); // Змінюємо маршрут
        flightDialog.getTxtDepartureDateTime().setText("2025-03-01 14:00");
        flightDialog.getTxtArrivalDateTime().setText("2025-03-01 16:00");
        flightDialog.getTxtTotalSeats().setText("40");
        flightDialog.getTxtBusModel().setText("Updated Bus");
        flightDialog.getTxtPricePerSeat().setText("200.00");
        flightDialog.getCmbStatus().setSelectedItem(FlightStatus.DELAYED);
    }

    private void clickSaveButton() {
        // Отримуємо кнопку "Зберегти" - припускаємо, що вона друга на панелі кнопок
        // Краще було б мати прямий доступ через геттер або шукати за текстом/іменем
        JPanel buttonPanel = (JPanel) flightDialog.getContentPane().getComponent(1); // Index 1 for SOUTH panel
        JButton btnSave = null;
        for(Component comp : buttonPanel.getComponents()){
            if(comp instanceof JButton && "Зберегти".equals(((JButton) comp).getText())){
                btnSave = (JButton) comp;
                break;
            }
        }
        assertNotNull(btnSave, "Кнопка 'Зберегти' не знайдена");
        final JButton finalBtnSave = btnSave;
        try {
            SwingUtilities.invokeAndWait(() -> {
                finalBtnSave.getActionListeners()[0].actionPerformed(new ActionEvent(finalBtnSave, ActionEvent.ACTION_PERFORMED, "save"));
            });
        } catch (Exception e) {
            fail("Помилка при симуляції натискання кнопки 'Зберегти': " + e.getMessage(), e);
        }
    }


    @Test
    @DisplayName("Збереження: успішне створення нового рейсу")
    void saveFlightAction_newFlight_success() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        when(mockFlightDAO.addFlight(any(Flight.class))).thenAnswer(invocation -> {
            Flight f = invocation.getArgument(0);
            f.setId(999L); // Симулюємо встановлення ID DAO
            return true;
        });

        clickSaveButton();

        assertTrue(flightDialog.isSaved());
        assertFalse(flightDialog.isDisplayable()); // Діалог має закритися
        verify(mockFlightDAO).addFlight(flightCaptor.capture());
        Flight captured = flightCaptor.getValue();
        assertEquals(availableRoutes.get(0).getId(), captured.getRoute().getId());
        assertEquals(LocalDateTime.of(2025,2,1,10,0), captured.getDepartureDateTime());
        assertEquals(30, captured.getTotalSeats());
        assertEquals(new BigDecimal("150.50"), captured.getPricePerSeat());
    }

    @Test
    @DisplayName("Збереження: успішне оновлення існуючого рейсу")
    void saveFlightAction_editFlight_success() throws SQLException {
        initializeDialog(flightToEdit);
        fillValidEditFlightData();
        when(mockFlightDAO.updateFlight(any(Flight.class))).thenReturn(true);

        clickSaveButton();

        assertTrue(flightDialog.isSaved());
        assertFalse(flightDialog.isDisplayable());
        verify(mockFlightDAO).updateFlight(flightCaptor.capture());
        Flight captured = flightCaptor.getValue();
        assertEquals(flightToEdit.getId(), captured.getId()); // ID має бути той самий
        assertEquals(availableRoutes.get(1).getId(), captured.getRoute().getId()); // Маршрут оновлено
        assertEquals(LocalDateTime.of(2025,3,1,14,0), captured.getDepartureDateTime());
        assertEquals(40, captured.getTotalSeats());
        assertEquals(FlightStatus.DELAYED, captured.getStatus());
    }

    @Test
    @DisplayName("Збереження: помилка валідації - не обрано маршрут")
    void saveFlightAction_validationError_noRouteSelected() throws SQLException {
        initializeDialog(null);
        // Не обираємо маршрут, інші поля заповнені
        flightDialog.getTxtDepartureDateTime().setText("2025-02-01 10:00");
        flightDialog.getTxtArrivalDateTime().setText("2025-02-01 12:00");
        flightDialog.getTxtTotalSeats().setText("30");
        flightDialog.getTxtPricePerSeat().setText("150.00");

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable()); // Діалог не має закриватися
    }

    @Test
    @DisplayName("Збереження: помилка валідації - неправильний формат дати")
    void saveFlightAction_validationError_invalidDateFormat() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        flightDialog.getTxtDepartureDateTime().setText("неправильна дата");

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO, never()).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("Збереження: помилка валідації - прибуття раніше відправлення")
    void saveFlightAction_validationError_arrivalBeforeDeparture() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        flightDialog.getTxtArrivalDateTime().setText("2025-02-01 09:00"); // Раніше ніж 10:00

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO, never()).addFlight(any(Flight.class));
    }


    @Test
    @DisplayName("Збереження: помилка валідації - непозитивна кількість місць")
    void saveFlightAction_validationError_nonPositiveSeats() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        flightDialog.getTxtTotalSeats().setText("0");

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO, never()).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("Збереження: помилка валідації - неправильний формат ціни")
    void saveFlightAction_validationError_invalidPriceFormat() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        flightDialog.getTxtPricePerSeat().setText("сто гривень");

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO, never()).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("Збереження: помилка DAO при додаванні (addFlight повертає false)")
    void saveFlightAction_newFlight_daoAddFails() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        when(mockFlightDAO.addFlight(any(Flight.class))).thenReturn(false);

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("Збереження: помилка DAO при оновленні (updateFlight повертає false)")
    void saveFlightAction_editFlight_daoUpdateFails() throws SQLException {
        initializeDialog(flightToEdit);
        fillValidEditFlightData();
        when(mockFlightDAO.updateFlight(any(Flight.class))).thenReturn(false);

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO).updateFlight(any(Flight.class));
    }


    @Test
    @DisplayName("Збереження: помилка SQLException при додаванні")
    void saveFlightAction_newFlight_sqlExceptionOnAdd() throws SQLException {
        initializeDialog(null);
        fillValidNewFlightData();
        when(mockFlightDAO.addFlight(any(Flight.class))).thenThrow(new SQLException("DB add error"));

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("Збереження: помилка SQLException при оновленні")
    void saveFlightAction_editFlight_sqlExceptionOnUpdate() throws SQLException {
        initializeDialog(flightToEdit);
        fillValidEditFlightData();
        when(mockFlightDAO.updateFlight(any(Flight.class))).thenThrow(new SQLException("DB update error"));

        clickSaveButton();

        assertFalse(flightDialog.isSaved());
        assertTrue(flightDialog.isDisplayable());
        verify(mockFlightDAO).updateFlight(any(Flight.class));
    }

    @Test
    @DisplayName("Натискання кнопки 'Скасувати'")
    void cancelButton_action() {
        initializeDialog(null);
        // Знаходимо кнопку "Скасувати"
        JPanel buttonPanel = (JPanel) flightDialog.getContentPane().getComponent(1);
        JButton btnCancel = null;
        for(Component comp : buttonPanel.getComponents()){
            if(comp instanceof JButton && "Скасувати".equals(((JButton) comp).getText())){
                btnCancel = (JButton) comp;
                break;
            }
        }
        assertNotNull(btnCancel, "Кнопка 'Скасувати' не знайдена");
        final JButton finalBtnCancel = btnCancel;

        try {
            SwingUtilities.invokeAndWait(() -> {
                finalBtnCancel.getActionListeners()[0].actionPerformed(new ActionEvent(finalBtnCancel, ActionEvent.ACTION_PERFORMED, "cancel"));
            });
        } catch (Exception e) {
            fail("Помилка при симуляції натискання кнопки 'Скасувати': " + e.getMessage(), e);
        }

        assertFalse(flightDialog.isSaved());
        assertFalse(flightDialog.isDisplayable());
    }
}