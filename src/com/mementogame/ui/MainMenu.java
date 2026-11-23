
package com.mementogame.ui;

import com.mementogame.memento.SaveManager;

import javax.swing.*;
import java.awt.*;

public class MainMenu extends JPanel {

    public MainMenu(Runnable newGame,
                    Runnable continueGame,
                    Runnable exitGame,
                    SaveManager saveManager) {

        setLayout(new GridBagLayout());
        setBackground(new Color(13, 11, 18));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("MEMENTO");
        title.setFont(new Font("Monospaced", Font.BOLD, 64));
        title.setForeground(new Color(220, 174, 91));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("THE LOST MEMORIES");
        subtitle.setFont(new Font("Monospaced", Font.PLAIN, 15));
        subtitle.setForeground(new Color(150, 130, 140));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel description = new JLabel(
                "A JOURNEY THROUGH THE DARKNESS"
        );
        description.setFont(new Font("Monospaced", Font.PLAIN, 11));
        description.setForeground(new Color(100, 90, 105));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(12));
        content.add(description);
        content.add(Box.createVerticalStrut(65));

        JButton start = createButton("NEW GAME");
        JButton continueButton = createButton("CONTINUE");
        JButton exit = createButton("EXIT");

        boolean hasSave = false;

        for (int i = 1; i <= 3; i++) {
            if (saveManager.hasSave(i)) {
                hasSave = true;
                break;
            }
        }

        continueButton.setEnabled(hasSave);

        start.addActionListener(e -> newGame.run());
        continueButton.addActionListener(e -> continueGame.run());
        exit.addActionListener(e -> exitGame.run());

        content.add(start);
        content.add(Box.createVerticalStrut(15));
        content.add(continueButton);
        content.add(Box.createVerticalStrut(15));
        content.add(exit);

        content.add(Box.createVerticalStrut(65));

        JLabel footer = new JLabel("VERSION 1.0  |  JAVA EDITION");
        footer.setFont(new Font("Monospaced", Font.PLAIN, 10));
        footer.setForeground(new Color(85, 75, 90));
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(footer);

        add(content);
    }

    private JButton createButton(String text) {

        JButton button = new JButton(text);

        button.setFont(new Font("Monospaced", Font.BOLD, 16));
        button.setForeground(new Color(220, 185, 125));
        button.setBackground(new Color(35, 28, 39));

        button.setFocusPainted(false);
        button.setBorder(
                BorderFactory.createLineBorder(
                        new Color(130, 95, 60), 2
                )
        );

        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(280, 52));
        button.setPreferredSize(new Dimension(280, 52));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }
}
