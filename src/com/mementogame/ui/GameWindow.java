
package com.mementogame.ui;

import com.mementogame.game.Game;
import com.mementogame.memento.SaveManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GameWindow extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel container = new JPanel(cardLayout);

    private final SaveManager saveManager = new SaveManager();

    private Game game;

    private static final String MENU = "MENU";
    private static final String GAME = "GAME";

    // =================================
    // CONSTRUCTOR
    // =================================

    public GameWindow() {

        setTitle("MEMENTO - The Lost Memories");

        setSize(1050, 850);
        setMinimumSize(new Dimension(1050, 850));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);
        setResizable(false);

        add(container);

        showMainMenu();

        setVisible(true);
    }

    // =================================
    // MAIN MENU
    // =================================

    private void showMainMenu() {

        container.removeAll();

        MainMenu menu = new MainMenu(
                this::startNewGame,
                this::continueGame,
                this::exitGame,
                saveManager
        );

        container.add(menu, MENU);

        cardLayout.show(container, MENU);

        container.revalidate();
        container.repaint();
    }

    // =================================
    // NEW GAME
    // =================================

    private void startNewGame() {

        game = new Game();

        showGame();
    }

    // =================================
    // CONTINUE GAME
    // =================================

    private void continueGame() {

        Integer slot = chooseSlot();

        if (slot == null) {
            return;
        }

        if (!saveManager.hasSave(slot)) {

            JOptionPane.showMessageDialog(
                    this,
                    "This save slot is empty!",
                    "MEMENTO",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        game = new Game();

        try {

            saveManager.load(game, slot);

            showGame();

        } catch (IOException | ClassNotFoundException e) {

            showError(
                    "Could not load game: " + e.getMessage()
            );
        }
    }

    // =================================
    // GAME SCREEN
    // =================================

    private void showGame() {

        container.removeAll();

        JPanel gameScreen = new JPanel(new BorderLayout());

        // Game panel with main menu callback
        GamePanel gamePanel = new GamePanel(
                game,
                this::returnToMenu
        );

        // Bottom controls
        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 20, 12)
        );

        controls.setBackground(new Color(23, 20, 29));

        JButton saveButton = createControlButton("SAVE GAME");
        JButton loadButton = createControlButton("LOAD GAME");
        JButton menuButton = createControlButton("MAIN MENU");

        saveButton.addActionListener(e -> saveGame());

        loadButton.addActionListener(e -> loadGame());

        menuButton.addActionListener(e -> returnToMenu());

        controls.add(saveButton);
        controls.add(loadButton);
        controls.add(menuButton);

        gameScreen.add(gamePanel, BorderLayout.CENTER);
        gameScreen.add(controls, BorderLayout.SOUTH);

        container.add(gameScreen, GAME);

        cardLayout.show(container, GAME);

        container.revalidate();
        container.repaint();

        gamePanel.requestFocusInWindow();
    }

    // =================================
    // CONTROL BUTTON DESIGN
    // =================================

    private JButton createControlButton(String text) {

        JButton button = new JButton(text);

        button.setFont(
                new Font("Monospaced", Font.BOLD, 12)
        );

        button.setForeground(
                new Color(225, 190, 130)
        );

        button.setBackground(
                new Color(45, 35, 45)
        );

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createLineBorder(
                        new Color(130, 95, 60),
                        1
                )
        );

        button.setPreferredSize(
                new Dimension(180, 40)
        );

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        return button;
    }

    // =================================
    // SAVE GAME
    // =================================

    private void saveGame() {

        Integer slot = chooseSlot();

        if (slot == null) {
            return;
        }

        try {

            saveManager.save(game, slot);

            JOptionPane.showMessageDialog(
                    this,
                    "Game saved successfully!\n\nSave slot: " + slot,
                    "SAVE COMPLETE",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException e) {

            showError(
                    "Could not save game: " + e.getMessage()
            );
        }
    }

    // =================================
    // LOAD GAME
    // =================================

    private void loadGame() {

        Integer slot = chooseSlot();

        if (slot == null) {
            return;
        }

        if (!saveManager.hasSave(slot)) {

            JOptionPane.showMessageDialog(
                    this,
                    "This save slot is empty!",
                    "EMPTY SLOT",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            saveManager.load(game, slot);

            showGame();

            if (game.isGameOver()) {

                JOptionPane.showMessageDialog(
                        this,
                        "This saved game contains a defeated character.",
                        "MEMORY RESTORED",
                        JOptionPane.WARNING_MESSAGE
                );
            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Previous game state restored!",
                        "LOAD COMPLETE",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (IOException | ClassNotFoundException e) {

            showError(
                    "Could not load game: " + e.getMessage()
            );
        }
    }

    // =================================
    // SAVE SLOT SELECTION
    // =================================

    private Integer chooseSlot() {

        String[] slots = new String[3];

        for (int i = 0; i < 3; i++) {

            int slotNumber = i + 1;

            slots[i] = saveManager.hasSave(slotNumber)
                    ? "SLOT " + slotNumber + "  [SAVED]"
                    : "SLOT " + slotNumber + "  [EMPTY]";
        }

        int selected = JOptionPane.showOptionDialog(
                this,
                "Choose a save slot:",
                "MEMENTO SAVE SYSTEM",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                slots,
                slots[0]
        );

        if (selected == -1) {
            return null;
        }

        return selected + 1;
    }

    // =================================
    // RETURN TO MAIN MENU
    // =================================

    private void returnToMenu() {

        showMainMenu();
    }

    // =================================
    // EXIT GAME
    // =================================

    private void exitGame() {

        int answer = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to leave the game?",
                "EXIT MEMENTO",
                JOptionPane.YES_NO_OPTION
        );

        if (answer == JOptionPane.YES_OPTION) {

            System.exit(0);
        }
    }

    // =================================
    // ERROR MESSAGE
    // =================================

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "MEMENTO ERROR",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
