package UI.Dialog; // Or your appropriate UI package

import DAO.StopDAO;
import Models.Route;
import Models.Stop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RouteCreationDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger("insurance.log");

    private JComboBox<Stop> cmbDepartureStop;
    private JComboBox<Stop> cmbDestinationStop;
    private JList<Stop> lstAvailableStops;
    private JList<Stop> lstSelectedIntermediateStops;
    private DefaultListModel<Stop> availableStopsModel;
    private DefaultListModel<Stop> selectedStopsModel;

    private JButton btnAddStop, btnRemoveStop, btnMoveUp, btnMoveDown, btnSave, btnCancel;

    private StopDAO stopDAO;
    private Route createdRoute = null;
    private boolean saved = false;

    public RouteCreationDialog(Frame owner, StopDAO stopDAO) {
        super(owner, "Створення нового маршруту", true);
        this.stopDAO = stopDAO;
        logger.info("Ініціалізація RouteCreationDialog.");
        initComponents();
        loadStops(); // Load stops after components are initialized
        pack();
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(600, 450)); // Ensure dialog is reasonably sized
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Stops Selection Panel (Departure and Destination) ---
        JPanel mainStopsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.1;
        mainStopsPanel.add(new JLabel("Зупинка відправлення:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.9;
        cmbDepartureStop = new JComboBox<>();
        mainStopsPanel.add(cmbDepartureStop, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.1;
        mainStopsPanel.add(new JLabel("Зупинка призначення:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.9;
        cmbDestinationStop = new JComboBox<>();
        mainStopsPanel.add(cmbDestinationStop, gbc);

        // --- Intermediate Stops Panel ---
        JPanel intermediateStopsOuterPanel = new JPanel(new BorderLayout(0, 5));
        intermediateStopsOuterPanel.setBorder(BorderFactory.createTitledBorder("Проміжні зупинки"));

        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // Panel for two JLists

        JPanel availablePanel = new JPanel(new BorderLayout(0, 5));
        availablePanel.add(new JLabel("Доступні зупинки:", SwingConstants.CENTER), BorderLayout.NORTH);
        availableStopsModel = new DefaultListModel<>();
        lstAvailableStops = new JList<>(availableStopsModel);
        JScrollPane availableScrollPane = new JScrollPane(lstAvailableStops);
        availableScrollPane.setPreferredSize(new Dimension(200, 150));
        availablePanel.add(availableScrollPane, BorderLayout.CENTER);

        JPanel selectedPanel = new JPanel(new BorderLayout(0, 5));
        selectedPanel.add(new JLabel("Обрані проміжні зупинки:", SwingConstants.CENTER), BorderLayout.NORTH);
        selectedStopsModel = new DefaultListModel<>();
        lstSelectedIntermediateStops = new JList<>(selectedStopsModel);
        JScrollPane selectedScrollPane = new JScrollPane(lstSelectedIntermediateStops);
        selectedScrollPane.setPreferredSize(new Dimension(200, 150));
        selectedPanel.add(selectedScrollPane, BorderLayout.CENTER);

        listsPanel.add(availablePanel);
        listsPanel.add(selectedPanel);

        JPanel intermediateControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        btnAddStop = new JButton("Додати >>");
        btnRemoveStop = new JButton("<< Видалити");
        btnMoveUp = new JButton("Вгору");
        btnMoveDown = new JButton("Вниз");

        btnAddStop.addActionListener(this::addIntermediateStopAction);
        btnRemoveStop.addActionListener(this::removeIntermediateStopAction);
        btnMoveUp.addActionListener(this::moveStopUpAction);
        btnMoveDown.addActionListener(this::moveStopDownAction);

        intermediateControlsPanel.add(btnAddStop);
        intermediateControlsPanel.add(btnRemoveStop);
        intermediateControlsPanel.add(Box.createHorizontalStrut(20)); // Spacer
        intermediateControlsPanel.add(btnMoveUp);
        intermediateControlsPanel.add(btnMoveDown);

        intermediateStopsOuterPanel.add(listsPanel, BorderLayout.CENTER);
        intermediateStopsOuterPanel.add(intermediateControlsPanel, BorderLayout.SOUTH);

        // --- Main Layout ---
        add(mainStopsPanel, BorderLayout.NORTH);
        add(intermediateStopsOuterPanel, BorderLayout.CENTER);

        // --- Control Buttons Panel (Save, Cancel) ---
        JPanel controlButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSave = new JButton("Зберегти маршрут");
        btnCancel = new JButton("Скасувати");

        btnSave.addActionListener(this::saveRouteAction);
        btnCancel.addActionListener(e -> {
            logger.debug("Створення маршруту скасовано користувачем.");
            saved = false;
            setVisible(false);
            dispose();
        });

        controlButtonsPanel.add(btnSave);
        controlButtonsPanel.add(btnCancel);
        add(controlButtonsPanel, BorderLayout.SOUTH);

        ItemListener stopChangeListener = e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                updateAvailableIntermediateStops();
            }
        };
        cmbDepartureStop.addItemListener(stopChangeListener);
        cmbDestinationStop.addItemListener(stopChangeListener);
        logger.debug("Компоненти RouteCreationDialog ініціалізовані.");
    }

    private void loadStops() {
        logger.debug("Завантаження зупинок для RouteCreationDialog.");
        try {
            List<Stop> allStops = stopDAO.getAllStops(); // Assumes StopDAO.getAllStops() exists
            if (allStops == null || allStops.isEmpty()) {
                logger.warn("Список всіх зупинок порожній або null. Неможливо створити маршрут.");
                JOptionPane.showMessageDialog(this, "Список доступних зупинок порожній. Додайте зупинки перед створенням маршруту.", "Помилка даних", JOptionPane.ERROR_MESSAGE);
                btnSave.setEnabled(false);
                cmbDepartureStop.setEnabled(false);
                cmbDestinationStop.setEnabled(false);
                return;
            }

            cmbDepartureStop.removeAllItems();
            cmbDestinationStop.removeAllItems();

            cmbDepartureStop.addItem(null); // Placeholder
            cmbDestinationStop.addItem(null); // Placeholder

            for (Stop stop : allStops) {
                cmbDepartureStop.addItem(stop);
                cmbDestinationStop.addItem(stop);
            }
            updateAvailableIntermediateStops(); // Initial population of available stops
            logger.info("Успішно завантажено {} зупинок.", allStops.size());

        } catch (SQLException e) {
            logger.error("Помилка SQL при завантаженні зупинок для RouteCreationDialog.", e);
            JOptionPane.showMessageDialog(this, "Не вдалося завантажити список зупинок: " + e.getMessage(), "Помилка бази даних", JOptionPane.ERROR_MESSAGE);
            btnSave.setEnabled(false);
        } catch (Exception e) {
            logger.error("Непередбачена помилка при завантаженні зупинок.", e);
            JOptionPane.showMessageDialog(this, "Непередбачена помилка при завантаженні зупинок: " + e.getMessage(), "Помилка програми", JOptionPane.ERROR_MESSAGE);
            btnSave.setEnabled(false);
        }
    }

    private void updateAvailableIntermediateStops() {
        logger.trace("Оновлення списку доступних проміжних зупинок.");
        Stop departure = (Stop) cmbDepartureStop.getSelectedItem();
        Stop destination = (Stop) cmbDestinationStop.getSelectedItem();

        availableStopsModel.clear();
        try {
            List<Stop> allStops = stopDAO.getAllStops();
            if (allStops == null) allStops = new ArrayList<>(); // Avoid NPE

            List<Stop> currentSelectedIntermediate = Collections.list(selectedStopsModel.elements());

            for (Stop stop : allStops) {
                boolean isDeparture = (departure != null && stop.equals(departure));
                boolean isDestination = (destination != null && stop.equals(destination));
                boolean isAlreadySelectedIntermediate = currentSelectedIntermediate.contains(stop);

                if (!isDeparture && !isDestination && !isAlreadySelectedIntermediate) {
                    availableStopsModel.addElement(stop);
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка SQL при оновленні списку доступних проміжних зупинок.", e);
        } catch (Exception e) {
            logger.error("Непередбачена помилка при оновленні списку доступних проміжних зупинок.", e);
        }
    }

    private void addIntermediateStopAction(ActionEvent e) {
        List<Stop> selectedFromAvailable = lstAvailableStops.getSelectedValuesList();
        if (!selectedFromAvailable.isEmpty()) {
            logger.debug("Додавання проміжних зупинок: {}", selectedFromAvailable.stream().map(Stop::getName).collect(Collectors.joining(", ")));
            for (Stop stop : selectedFromAvailable) {
                selectedStopsModel.addElement(stop);
                availableStopsModel.removeElement(stop); // Important: remove from available
            }
        } else {
            logger.trace("Спроба додати проміжну зупинку, але нічого не вибрано в доступних.");
        }
    }

    private void removeIntermediateStopAction(ActionEvent e) {
        List<Stop> selectedForRemoval = lstSelectedIntermediateStops.getSelectedValuesList();
        if (!selectedForRemoval.isEmpty()) {
            logger.debug("Видалення проміжних зупинок: {}", selectedForRemoval.stream().map(Stop::getName).collect(Collectors.joining(", ")));
            for (Stop stop : selectedForRemoval) {
                selectedStopsModel.removeElement(stop);
                // Add back to available list, only if it's not current departure/destination
                Stop departure = (Stop) cmbDepartureStop.getSelectedItem();
                Stop destination = (Stop) cmbDestinationStop.getSelectedItem();
                boolean isDeparture = (departure != null && stop.equals(departure));
                boolean isDestination = (destination != null && stop.equals(destination));
                if (!isDeparture && !isDestination && !availableStopsModel.contains(stop)) {
                    availableStopsModel.addElement(stop);
                }
            }
        } else {
            logger.trace("Спроба видалити проміжну зупинку, але нічого не вибрано в обраних.");
        }
    }

    private void moveStopUpAction(ActionEvent e) {
        int selectedIndex = lstSelectedIntermediateStops.getSelectedIndex();
        if (selectedIndex > 0) { // Can move up if not the first element
            logger.debug("Переміщення проміжної зупинки вгору: індекс {}", selectedIndex);
            Stop stopToMove = selectedStopsModel.getElementAt(selectedIndex);
            selectedStopsModel.remove(selectedIndex);
            selectedStopsModel.insertElementAt(stopToMove, selectedIndex - 1);
            lstSelectedIntermediateStops.setSelectedIndex(selectedIndex - 1);
        } else {
            logger.trace("Спроба перемістити зупинку вгору, але вона вже перша або нічого не вибрано.");
        }
    }

    private void moveStopDownAction(ActionEvent e) {
        int selectedIndex = lstSelectedIntermediateStops.getSelectedIndex();
        // Can move down if not the last element and something is selected
        if (selectedIndex != -1 && selectedIndex < selectedStopsModel.getSize() - 1) {
            logger.debug("Переміщення проміжної зупинки вниз: індекс {}", selectedIndex);
            Stop stopToMove = selectedStopsModel.getElementAt(selectedIndex);
            selectedStopsModel.remove(selectedIndex);
            selectedStopsModel.insertElementAt(stopToMove, selectedIndex + 1);
            lstSelectedIntermediateStops.setSelectedIndex(selectedIndex + 1);
        } else {
            logger.trace("Спроба перемістити зупинку вниз, але вона вже остання або нічого не вибрано.");
        }
    }

    private void saveRouteAction(ActionEvent e) {
        logger.info("Спроба зберегти новий маршрут.");
        Stop departureStop = (Stop) cmbDepartureStop.getSelectedItem();
        Stop destinationStop = (Stop) cmbDestinationStop.getSelectedItem();

        if (departureStop == null) {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть зупинку відправлення.", "Валідація", JOptionPane.WARNING_MESSAGE);
            logger.warn("Валідація не пройдена: зупинка відправлення не вибрана.");
            return;
        }
        if (destinationStop == null) {
            JOptionPane.showMessageDialog(this, "Будь ласка, виберіть зупинку призначення.", "Валідація", JOptionPane.WARNING_MESSAGE);
            logger.warn("Валідація не пройдена: зупинка призначення не вибрана.");
            return;
        }
        if (departureStop.equals(destinationStop)) {
            JOptionPane.showMessageDialog(this, "Зупинка відправлення та призначення не можуть бути однаковими.", "Валідація", JOptionPane.WARNING_MESSAGE);
            logger.warn("Валідація не пройдена: зупинка відправлення та призначення однакові.");
            return;
        }

        List<Stop> intermediateStops = new ArrayList<>();
        for (int i = 0; i < selectedStopsModel.getSize(); i++) {
            Stop intermediate = selectedStopsModel.getElementAt(i);
            if (intermediate.equals(departureStop) || intermediate.equals(destinationStop)) {
                JOptionPane.showMessageDialog(this,
                        "Проміжна зупинка '" + intermediate.getName() + "' не може бути такою ж, як зупинка відправлення або призначення.",
                        "Валідація", JOptionPane.WARNING_MESSAGE);
                logger.warn("Валідація не пройдена: проміжна зупинка {} збігається з відправленням/призначенням.", intermediate.getName());
                return;
            }
            intermediateStops.add(intermediate);
        }

        createdRoute = new Route(0, departureStop, destinationStop, intermediateStops); // ID is 0, will be set by DAO
        saved = true;
        logger.info("Новий маршрут підготовлено до збереження: {}", createdRoute.getFullRouteDescription());
        setVisible(false);
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    public Route getCreatedRoute() {
        return createdRoute;
    }
}