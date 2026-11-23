
package com.mementogame.ui;

import com.mementogame.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

public class GamePanel extends JPanel {

    private final Game game;
    private final Runnable onMainMenu;
    private final Timer gameTimer;
    private final KeyEventDispatcher shiftDispatcher;

    private static final int TILE = 40;
    private static final int MAP_X = 100;
    private static final int MAP_Y = 115;

    private static final long DEATH_ANIMATION_DURATION = 1100;

    private final Color background = new Color(13, 13, 20);
    private final Color stone = new Color(48, 43, 48);
    private final Color stoneDark = new Color(34, 30, 37);
    private final Color gold = new Color(218, 170, 75);

    // Death screen buttons
    private Rectangle restartButton;
    private Rectangle menuButton;

    // Victory screen buttons
    private Rectangle shopButton;
    private Rectangle nextLevelButton;
    private Rectangle victoryMenuButton;

    // Shop buttons
    private final Rectangle[] shopItemButtons = new Rectangle[5];
    private Rectangle shopBackButton;

    // Shop state
    private boolean shopOpen = false;
    private String shopMessage = "Choose an upgrade for your next battle.";

    private final String[] shopNames = {
            "HEALTH POTION",
            "STRONG POTION",
            "MAX HEALTH UPGRADE",
            "SWORD UPGRADE",
            "EXTRA POTION"
    };

    private final String[] shopDescriptions = {
            "Restore 30 HP immediately",
            "Restore 60 HP immediately",
            "+20 maximum health (maximum 200)",
            "Increase sword damage from 35 to 50",
            "Add one potion to your inventory"
    };

    private final int[] shopPrices = {
            30, 50, 100, 150, 40
    };

    // Player direction
    private int facingX = 1;
    private int facingY = 0;

    // Combat animations
    private long attackAnimationUntil = 0;
    private long blockFlashUntil = 0;
    private long enemyAttackAnimationUntil = 0;

    // Enemy death
    private long enemyDeathStartedAt = 0;
    private boolean levelCompleted = false;

    private String previousMessage = "";

    // =================================
    // CONSTRUCTOR
    // =================================

    public GamePanel(Game game, Runnable onMainMenu) {

        this.game = Objects.requireNonNull(game);
        this.onMainMenu = onMainMenu;

        setPreferredSize(new Dimension(1000, 720));
        setBackground(background);
        setFocusable(true);

        // Handle a previously defeated enemy after loading
        if (!game.isEnemyAlive() && !game.isGameOver()) {

            enemyDeathStartedAt =
                    System.currentTimeMillis()
                            - DEATH_ANIMATION_DURATION;

            levelCompleted = true;
        }

        setupControls();
        setupMouseControls();

        previousMessage = game.getMessage();

        // Register SHIFT listener
        shiftDispatcher = createShiftDispatcher();

        KeyboardFocusManager
                .getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(shiftDispatcher);

        // Game loop
        gameTimer = new Timer(30, e -> updateGame());

        gameTimer.start();
    }

    // =================================
    // SHIFT LISTENER
    // =================================

    private KeyEventDispatcher createShiftDispatcher() {

        return event -> {

            if (event.getKeyCode() != KeyEvent.VK_SHIFT) {
                return false;
            }

            if (event.getID() == KeyEvent.KEY_PRESSED) {

                if (isShowing() && canControl()) {

                    int dx = game.getEnemyX() - game.getPlayerX();
                    int dy = game.getEnemyY() - game.getPlayerY();

                    if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
                        facingX = Integer.signum(dx);
                        facingY = 0;
                    } else if (dy != 0) {
                        facingX = 0;
                        facingY = Integer.signum(dy);
                    }

                    game.setBlocking(true);
                    repaint();
                }

                return true;
            }

            if (event.getID() == KeyEvent.KEY_RELEASED) {

                game.setBlocking(false);
                repaint();

                return true;
            }

            return false;
        };
    }

    // =================================
    // GAME LOOP
    // =================================

    private void updateGame() {

        game.update();

        checkEnemyDefeated();

        String currentMessage = game.getMessage();

        if (!previousMessage.equals(currentMessage)) {

            if (currentMessage.startsWith("SHIELD BLOCKED")) {

                blockFlashUntil =
                        System.currentTimeMillis() + 350;
            }

            if (currentMessage.startsWith("SHADOW ATTACKS")) {

                enemyAttackAnimationUntil =
                        System.currentTimeMillis() + 250;
            }

            previousMessage = currentMessage;
        }

        if (game.isGameOver()) {

            game.setBlocking(false);
            gameTimer.stop();
        }

        repaint();
    }

    // =================================
    // DETECT ENEMY DEATH
    // =================================

    private void checkEnemyDefeated() {

        if (!game.isGameOver()
                && !game.isEnemyAlive()
                && enemyDeathStartedAt == 0) {

            enemyDeathStartedAt =
                    System.currentTimeMillis();

            levelCompleted = true;
            shopOpen = false;

            game.setBlocking(false);
        }
    }

    // =================================
    // GAME STATE CHECKS
    // =================================

    private boolean canControl() {

        return !game.isGameOver()
                && !levelCompleted;
    }

    private boolean isVictoryReady() {

        return levelCompleted
                && System.currentTimeMillis()
                - enemyDeathStartedAt
                >= DEATH_ANIMATION_DURATION;
    }

    // =================================
    // KEYBOARD CONTROLS
    // =================================

    private void setupControls() {

        bind("pressed W", "up",
                () -> movePlayer(0, -1));

        bind("pressed S", "down",
                () -> movePlayer(0, 1));

        bind("pressed A", "left",
                () -> movePlayer(-1, 0));

        bind("pressed D", "right",
                () -> movePlayer(1, 0));

        bind("pressed SPACE", "attack",
                this::performAttack);

        bind("pressed H", "potion", () -> {

            if (!canControl()) return;

            game.usePotion();
            repaint();
        });

        bind("pressed N", "nextRoom", () -> {

            if (!canControl()) return;

            game.nextRoom();
            repaint();
        });

        bind("pressed ENTER", "nextLevel", () -> {

            if (isVictoryReady() && !shopOpen) {
                advanceToNextLevel();
            }
        });

        bind("pressed R", "restart", () -> {

            if (game.isGameOver()) {
                restartGame();
            }
        });

        bind("pressed ESCAPE", "menu", () -> {

            if (shopOpen) {

                shopOpen = false;
                repaint();

            } else {

                goToMainMenu();
            }
        });
    }

    private void bind(
            String key,
            String name,
            Runnable action
    ) {

        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);

        getActionMap().put(name, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // =================================
    // PLAYER MOVEMENT
    // =================================

    private void movePlayer(int dx, int dy) {

        if (!canControl()) return;

        facingX = dx;
        facingY = dy;

        game.move(dx, dy);

        repaint();
    }

    // =================================
    // PLAYER ATTACK
    // =================================

    private void performAttack() {

        if (!canControl()) return;

        if (game.isBlocking()) {

            game.attack();
            repaint();
            return;
        }

        int dx = game.getEnemyX() - game.getPlayerX();
        int dy = game.getEnemyY() - game.getPlayerY();

        if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {

            facingX = Integer.signum(dx);
            facingY = 0;

        } else if (dy != 0) {

            facingX = 0;
            facingY = Integer.signum(dy);
        }

        attackAnimationUntil =
                System.currentTimeMillis() + 240;

        game.attack();

        checkEnemyDefeated();

        repaint();
    }

    // =================================
    // MOUSE CONTROLS
    // =================================

    private void setupMouseControls() {

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                Point point = e.getPoint();

                // Death screen
                if (game.isGameOver()) {

                    if (restartButton != null
                            && restartButton.contains(point)) {

                        restartGame();

                    } else if (menuButton != null
                            && menuButton.contains(point)) {

                        goToMainMenu();
                    }

                    return;
                }

                // Shop screen
                if (isVictoryReady() && shopOpen) {

                    handleShopClick(point);
                    return;
                }

                // Victory screen
                if (isVictoryReady()) {

                    if (shopButton != null
                            && shopButton.contains(point)) {

                        shopOpen = true;
                        shopMessage = "Choose an upgrade for your next battle.";

                        repaint();

                    } else if (nextLevelButton != null
                            && nextLevelButton.contains(point)) {

                        advanceToNextLevel();

                    } else if (victoryMenuButton != null
                            && victoryMenuButton.contains(point)) {

                        goToMainMenu();
                    }
                }
            }
        });
    }

    // =================================
    // SHOP: HANDLE PURCHASE CLICK
    // =================================

    private void handleShopClick(Point point) {

        if (shopBackButton != null
                && shopBackButton.contains(point)) {

            shopOpen = false;
            repaint();
            return;
        }

        for (int i = 0; i < shopItemButtons.length; i++) {

            Rectangle button = shopItemButtons[i];

            if (button != null && button.contains(point)) {

                boolean purchased = false;

                switch (i) {

                    case 0:
                        purchased = game.buyHealthPotion();
                        break;

                    case 1:
                        purchased = game.buyStrongPotion();
                        break;

                    case 2:
                        purchased = game.buyMaxHealthUpgrade();
                        break;

                    case 3:
                        purchased = game.buySwordUpgrade();
                        break;

                    case 4:
                        purchased = game.buyExtraPotion();
                        break;

                    default:
                        break;
                }

                shopMessage = game.getMessage();

                repaint();
                return;
            }
        }
    }

    // =================================
    // NEXT LEVEL
    // =================================

    private void advanceToNextLevel() {

        if (!isVictoryReady()) return;

        game.nextRoom();

        enemyDeathStartedAt = 0;
        levelCompleted = false;
        shopOpen = false;

        attackAnimationUntil = 0;
        blockFlashUntil = 0;
        enemyAttackAnimationUntil = 0;

        facingX = 1;
        facingY = 0;

        previousMessage = game.getMessage();

        gameTimer.start();

        repaint();
        requestFocusInWindow();
    }

    // =================================
    // RESTART
    // =================================

    private void restartGame() {

        game.restart();

        enemyDeathStartedAt = 0;
        levelCompleted = false;
        shopOpen = false;

        attackAnimationUntil = 0;
        blockFlashUntil = 0;
        enemyAttackAnimationUntil = 0;

        facingX = 1;
        facingY = 0;

        previousMessage = game.getMessage();

        gameTimer.start();

        repaint();
        requestFocusInWindow();
    }

    // =================================
    // MAIN MENU
    // =================================

    private void goToMainMenu() {

        gameTimer.stop();
        game.setBlocking(false);

        if (onMainMenu != null) {
            onMainMenu.run();
        }
    }

    // =================================
    // CLEANUP
    // =================================

    @Override
    public void removeNotify() {

        gameTimer.stop();

        KeyboardFocusManager
                .getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(shiftDispatcher);

        game.setBlocking(false);

        super.removeNotify();
    }

    // =================================
    // MAIN RENDERING
    // =================================

    @Override
    protected void paintComponent(Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
        );

        drawHeader(g);
        drawDungeon(g);
        drawEnemy(g);

        if (game.isGameOver()) {

            drawDeadPlayer(g);

        } else {

            drawPlayer(g);
        }

        // Enemy death animation
        if (enemyDeathStartedAt > 0
                && !game.isEnemyAlive()
                && !game.isGameOver()
                && !isVictoryReady()) {

            drawEnemyDeathAnimation(g);
        }

        drawFooter(g);

        if (game.isGameOver()) {

            drawDeathScreen(g);

        } else if (isVictoryReady() && shopOpen) {

            drawShopScreen(g);

        } else if (isVictoryReady()) {

            drawVictoryScreen(g);
        }

        g.dispose();
    }

    // =================================
    // HEADER
    // =================================

    private void drawHeader(Graphics2D g) {

        g.setColor(new Color(23, 20, 29));
        g.fillRect(0, 0, getWidth(), 95);

        g.setColor(gold);
        g.setFont(new Font("Monospaced", Font.BOLD, 27));

        g.drawString("MEMENTO", 35, 38);

        g.setColor(new Color(160, 145, 155));
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));

        g.drawString("THE LOST MEMORIES", 37, 57);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));

        g.drawString("ROOM " + game.getRoom(), 820, 32);

        g.setColor(new Color(180, 170, 180));
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));

        g.drawString("LEVEL " + game.getLevel(), 820, 52);

        g.setColor(gold);
        g.drawString("DMG " + game.getSwordDamage(), 820, 72);
    }

    // =================================
    // DUNGEON
    // =================================

    private void drawDungeon(Graphics2D g) {

        int rows = 12;
        int cols = 20;

        for (int y = 0; y < rows; y++) {

            for (int x = 0; x < cols; x++) {

                int px = MAP_X + x * TILE;
                int py = MAP_Y + y * TILE;

                boolean wall =
                        x == 0 || x == cols - 1 ||
                                y == 0 || y == rows - 1;

                g.setColor(wall ? stoneDark : stone);
                g.fillRect(px, py, TILE, TILE);

                g.setColor(new Color(70, 62, 68));
                g.drawRect(px, py, TILE, TILE);

                if (!wall && (x * 7 + y * 11) % 13 == 0) {

                    g.setColor(new Color(62, 54, 60));
                    g.fillRect(px + 12, py + 13, 5, 5);
                }

                if (wall && (x + y) % 3 == 0) {

                    g.setColor(new Color(65, 57, 64));
                    g.fillRect(px + 7, py + 7, 25, 4);
                }
            }
        }

        drawTorch(g, MAP_X + 2 * TILE, MAP_Y + TILE);
        drawTorch(g, MAP_X + 17 * TILE, MAP_Y + TILE);
        drawTorch(g, MAP_X + 2 * TILE, MAP_Y + 10 * TILE);
        drawTorch(g, MAP_X + 17 * TILE, MAP_Y + 10 * TILE);
    }

    // =================================
    // TORCHES
    // =================================

    private void drawTorch(Graphics2D g, int x, int y) {

        g.setColor(new Color(120, 70, 30));
        g.fillRect(x + 15, y + 13, 10, 19);

        g.setColor(new Color(255, 155, 50));
        g.fillRect(x + 16, y + 4, 8, 12);

        g.setColor(new Color(255, 220, 110));
        g.fillRect(x + 18, y + 1, 4, 7);
    }

    // =================================
    // PLAYER
    // =================================

    private void drawPlayer(Graphics2D g) {

        int x = MAP_X + game.getPlayerX() * TILE;
        int y = MAP_Y + game.getPlayerY() * TILE;

        g.setColor(new Color(10, 8, 12));
        g.fillRect(x + 7, y + 30, 27, 7);

        g.setColor(new Color(50, 45, 55));
        g.fillRect(x + 11, y + 25, 7, 11);
        g.fillRect(x + 23, y + 25, 7, 11);

        g.setColor(new Color(110, 120, 135));
        g.fillRect(x + 8, y + 13, 25, 16);

        g.setColor(new Color(170, 180, 190));
        g.fillRect(x + 12, y + 15, 16, 5);

        g.setColor(new Color(190, 160, 130));
        g.fillRect(x + 13, y + 4, 16, 13);

        g.setColor(new Color(70, 75, 95));
        g.fillRect(x + 11, y + 2, 20, 7);

        g.setColor(new Color(180, 40, 45));
        g.fillRect(x + 15, y + 10, 3, 2);
        g.fillRect(x + 24, y + 10, 3, 2);

        if (game.isBlocking()) {

            drawShield(g, x, y);

        } else {

            drawSword(g, x, y);
        }

        if (System.currentTimeMillis() < attackAnimationUntil) {

            drawAttackAnimation(g, x, y);
        }
    }

    // =================================
    // SWORD
    // =================================

    private void drawSword(Graphics2D g, int x, int y) {

        g.setColor(new Color(210, 210, 220));

        if (facingX == 1) {
            g.fillRect(x + 34, y + 12, 4, 18);

        } else if (facingX == -1) {
            g.fillRect(x + 2, y + 12, 4, 18);

        } else if (facingY == -1) {
            g.fillRect(x + 17, y - 2, 4, 18);

        } else {
            g.fillRect(x + 17, y + 27, 4, 18);
        }

        g.setColor(gold);

        if (facingX != 0) {

            g.fillRect(
                    x + (facingX > 0 ? 31 : 0),
                    y + 24,
                    10,
                    3
            );

        } else {

            g.fillRect(
                    x + 14,
                    y + (facingY > 0 ? 30 : -1),
                    10,
                    3
            );
        }
    }

    // =================================
    // SHIELD
    // =================================

    private void drawShield(Graphics2D g, int x, int y) {

        boolean flash =
                System.currentTimeMillis() < blockFlashUntil;

        Color shieldColor = flash
                ? new Color(220, 245, 255, 230)
                : new Color(65, 160, 220, 190);

        Color borderColor = flash
                ? Color.WHITE
                : new Color(130, 210, 255);

        int sx;
        int sy;
        int sw;
        int sh;

        if (facingX == 1) {
            sx = x + 34;
            sy = y + 8;
            sw = 11;
            sh = 25;

        } else if (facingX == -1) {
            sx = x - 5;
            sy = y + 8;
            sw = 11;
            sh = 25;

        } else if (facingY == -1) {
            sx = x + 8;
            sy = y - 6;
            sw = 25;
            sh = 11;

        } else {
            sx = x + 8;
            sy = y + 35;
            sw = 25;
            sh = 11;
        }

        g.setColor(new Color(55, 130, 200, 70));
        g.fillRoundRect(sx - 4, sy - 4, sw + 8, sh + 8, 10, 10);

        g.setColor(shieldColor);
        g.fillRoundRect(sx, sy, sw, sh, 7, 7);

        g.setColor(borderColor);
        g.setStroke(new BasicStroke(2));

        g.drawRoundRect(sx, sy, sw, sh, 7, 7);

        g.setColor(Color.WHITE);

        if (facingX != 0) {
            g.fillRect(sx + 4, sy + 5, 3, sh - 10);
        } else {
            g.fillRect(sx + 5, sy + 4, sw - 10, 3);
        }

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(140, 210, 255));

        g.drawString("BLOCK", x - 1, y - 8);
    }

    // =================================
    // ATTACK ANIMATION
    // =================================

    private void drawAttackAnimation(
            Graphics2D g,
            int x,
            int y
    ) {

        Stroke oldStroke = g.getStroke();

        g.setStroke(new BasicStroke(4));
        g.setColor(new Color(255, 195, 95));

        if (facingX == 1) {

            g.drawArc(x + 18, y - 8, 45, 55, 235, 120);
            g.drawLine(x + 33, y + 5, x + 49, y + 22);

        } else if (facingX == -1) {

            g.drawArc(x - 23, y - 8, 45, 55, 65, 120);
            g.drawLine(x + 7, y + 5, x - 9, y + 22);

        } else if (facingY == -1) {

            g.drawArc(x - 8, y - 23, 55, 45, 325, 120);
            g.drawLine(x + 5, y + 7, x + 22, y - 9);

        } else {

            g.drawArc(x - 8, y + 18, 55, 45, 145, 120);
            g.drawLine(x + 5, y + 33, x + 22, y + 49);
        }

        g.setStroke(new BasicStroke(2));
        g.setColor(Color.WHITE);

        if (facingX != 0) {

            int sign = facingX;

            g.drawLine(
                    x + 20 + sign * 14,
                    y + 12,
                    x + 20 + sign * 27,
                    y + 26
            );

        } else {

            int sign = facingY;

            g.drawLine(
                    x + 12,
                    y + 20 + sign * 14,
                    x + 26,
                    y + 20 + sign * 27
            );
        }

        g.setStroke(oldStroke);
    }

    // =================================
    // ENEMY
    // =================================

    private void drawEnemy(Graphics2D g) {

        if (!game.isEnemyAlive()) {
            return;
        }

        int x = MAP_X + game.getEnemyX() * TILE;
        int y = MAP_Y + game.getEnemyY() * TILE;

        g.setColor(new Color(40, 15, 20));
        g.fillRect(x - 4, y - 14, 48, 6);

        g.setColor(new Color(200, 45, 55));

        int hpWidth =
                48 * game.getEnemyHealth()
                        / game.getEnemyMaxHealth();

        g.fillRect(x - 4, y - 14, hpWidth, 6);

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(255, 100, 100));

        g.drawString("SHADOW", x - 5, y - 19);

        g.setColor(Color.BLACK);
        g.fillRect(x + 7, y + 31, 28, 7);

        g.setColor(new Color(85, 30, 40));
        g.fillRect(x + 7, y + 14, 27, 20);

        g.setColor(new Color(120, 105, 100));
        g.fillRect(x + 10, y + 3, 22, 19);

        g.setColor(new Color(190, 180, 165));

        g.fillRect(x + 7, y, 5, 10);
        g.fillRect(x + 30, y, 5, 10);

        g.setColor(new Color(255, 40, 30));

        g.fillRect(x + 14, y + 11, 5, 4);
        g.fillRect(x + 25, y + 11, 5, 4);

        if (System.currentTimeMillis() < enemyAttackAnimationUntil) {

            g.setStroke(new BasicStroke(4));

            g.setColor(new Color(255, 45, 45));

            g.drawLine(
                    x + 12,
                    y + 30,
                    x - 3,
                    y + 42
            );

            g.setStroke(new BasicStroke(1));
        }
    }

    // =================================
    // ENEMY DEATH ANIMATION
    // =================================

    private void drawEnemyDeathAnimation(Graphics2D g) {

        long elapsed =
                System.currentTimeMillis() - enemyDeathStartedAt;

        float progress = Math.min(
                1f,
                elapsed / (float) DEATH_ANIMATION_DURATION
        );

        float alpha = 1f - progress;

        int x = MAP_X + game.getEnemyX() * TILE;
        int y = MAP_Y + game.getEnemyY() * TILE;

        int fall = (int) (progress * 13);

        Graphics2D corpse = (Graphics2D) g.create();

        corpse.setComposite(
                AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        Math.max(0f, alpha)
                )
        );

        corpse.setColor(Color.BLACK);
        corpse.fillRect(x + 4, y + 29 + fall, 34, 8);

        corpse.setColor(new Color(75, 25, 38));
        corpse.fillRect(x + 8, y + 18 + fall, 29, 12);

        corpse.setColor(new Color(120, 105, 100));
        corpse.fillRect(x + 2, y + 17 + fall, 15, 12);

        corpse.setColor(new Color(190, 180, 165));
        corpse.fillRect(x + 2, y + 12 + fall, 4, 7);

        corpse.setColor(new Color(220, 55, 65));

        for (int i = 0; i < 7; i++) {

            int particleX = x + 5 + i * 5;
            int particleY = y + 5 + (i % 3) * 9 - fall;

            corpse.fillRect(particleX, particleY, 3, 3);
        }

        corpse.dispose();

        g.setFont(new Font("Monospaced", Font.BOLD, 13));

        g.setColor(new Color(
                240, 100, 100,
                Math.max(0, (int) (alpha * 255))
        ));

        drawCentered(
                g,
                "SHADOW DEFEATED!",
                x + TILE / 2,
                y - 20 - (int) (progress * 22)
        );
    }

    // =================================
    // DEAD PLAYER
    // =================================

    private void drawDeadPlayer(Graphics2D g) {

        int x = MAP_X + game.getPlayerX() * TILE;
        int y = MAP_Y + game.getPlayerY() * TILE;

        g.setColor(new Color(5, 4, 8));
        g.fillRect(x + 3, y + 23, 35, 10);

        g.setColor(new Color(85, 91, 105));
        g.fillRect(x + 10, y + 17, 24, 12);

        g.setColor(new Color(145, 150, 160));
        g.fillRect(x + 15, y + 19, 12, 4);

        g.setColor(new Color(65, 70, 85));
        g.fillRect(x + 1, y + 13, 16, 12);

        g.setColor(new Color(170, 130, 115));
        g.fillRect(x + 4, y + 18, 9, 5);

        g.setColor(new Color(45, 40, 50));
        g.fillRect(x + 28, y + 24, 10, 6);

        g.setColor(new Color(190, 190, 200));
        g.fillRect(x + 18, y + 34, 19, 3);

        g.setColor(gold);
        g.fillRect(x + 19, y + 31, 3, 9);
    }

    // =================================
    // FOOTER / HUD
    // =================================

    private void drawFooter(Graphics2D g) {

        int y = MAP_Y + 12 * TILE + 15;

        g.setFont(new Font("Monospaced", Font.BOLD, 12));

        // HP
        g.setColor(Color.WHITE);
        g.drawString("HP", 35, y + 12);

        g.setColor(new Color(70, 25, 35));
        g.fillRect(65, y, 150, 13);

        g.setColor(new Color(195, 45, 60));

        int hpWidth =
                150 * game.getHealth() / game.getMaxHealth();

        g.fillRect(65, y, hpWidth, 13);

        g.setColor(Color.WHITE);

        g.drawString(
                game.getHealth() + "/" + game.getMaxHealth(),
                225, y + 12
        );

        // XP
        g.setColor(new Color(70, 55, 25));
        g.fillRect(340, y, 120, 13);

        g.setColor(new Color(220, 170, 55));

        int xpWidth = 120 * game.getXp() / 100;

        g.fillRect(340, y, xpWidth, 13);

        g.setColor(Color.WHITE);

        g.drawString(
                "XP " + game.getXp() + "/100",
                470, y + 12
        );

        // Coins
        g.setColor(gold);

        g.drawString(
                "COINS: " + game.getCoins(),
                650, y + 12
        );

        // Controls
        g.setColor(new Color(180, 170, 180));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));

        g.drawString(
                "WASD: MOVE   SPACE: ATTACK   SHIFT: BLOCK   H: POTION",
                35, y + 40
        );

        // Statistics
        g.setColor(new Color(190, 175, 145));

        g.drawString(
                "ENEMIES DEFEATED: " + game.getDefeatedEnemies()
                        + "     POTIONS: " + game.getPotions(),
                35, y + 58
        );

        // Message
        g.setColor(gold);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));

        g.drawString(
                game.getMessage(),
                35, y + 82
        );

        // Sword damage
        g.setColor(new Color(205, 185, 145));

        g.drawString(
                "SWORD DAMAGE: " + game.getSwordDamage(),
                650, y + 40
        );
    }

    // =================================
    // VICTORY SCREEN
    // =================================

    private void drawVictoryScreen(Graphics2D g) {

        g.setColor(new Color(5, 3, 9, 225));
        g.fillRect(0, 0, getWidth(), getHeight());

        int boxWidth = 600;
        int boxHeight = 390;

        int boxX = (getWidth() - boxWidth) / 2;
        int boxY = (getHeight() - boxHeight) / 2;

        g.setColor(new Color(25, 20, 26));
        g.fillRect(boxX, boxY, boxWidth, boxHeight);

        g.setColor(new Color(190, 145, 75));
        g.setStroke(new BasicStroke(2));
        g.drawRect(boxX, boxY, boxWidth, boxHeight);

        // Title
        g.setFont(new Font("Monospaced", Font.BOLD, 46));
        g.setColor(new Color(235, 190, 100));

        drawCentered(
                g, "VICTORY!",
                getWidth() / 2,
                boxY + 70
        );

        // Subtitle
        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(new Color(185, 155, 145));

        drawCentered(
                g, "THE SHADOW HAS FALLEN",
                getWidth() / 2,
                boxY + 98
        );

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(Color.WHITE);

        drawCentered(
                g,
                "DUNGEON ROOM " + game.getRoom() + " CLEARED",
                getWidth() / 2,
                boxY + 155
        );

        // Reward
        g.setColor(new Color(230, 185, 95));

        drawCentered(
                g,
                "REWARD: +60 COINS   +30 XP",
                getWidth() / 2,
                boxY + 190
        );

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(170, 160, 165));

        drawCentered(
                g,
                "TOTAL ENEMIES DEFEATED: "
                        + game.getDefeatedEnemies(),
                getWidth() / 2,
                boxY + 225
        );

        // Three buttons
        int buttonWidth = 164;
        int buttonHeight = 48;
        int gap = 12;

        int totalWidth = buttonWidth * 3 + gap * 2;
        int startX = (getWidth() - totalWidth) / 2;

        int buttonY = boxY + 270;

        shopButton = new Rectangle(
                startX,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        nextLevelButton = new Rectangle(
                startX + buttonWidth + gap,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        victoryMenuButton = new Rectangle(
                startX + (buttonWidth + gap) * 2,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        drawVictoryButton(g, shopButton, "SHOP", true);
        drawVictoryButton(g, nextLevelButton, "NEXT LEVEL", true);
        drawVictoryButton(g, victoryMenuButton, "MAIN MENU", false);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(135, 120, 130));

        drawCentered(
                g,
                "VISIT THE SHOP TO PREPARE FOR YOUR NEXT BATTLE",
                getWidth() / 2,
                boxY + 350
        );
    }

    // =================================
    // SHOP SCREEN
    // =================================

    private void drawShopScreen(Graphics2D g) {

        // Dark overlay
        g.setColor(new Color(5, 3, 9, 240));
        g.fillRect(0, 0, getWidth(), getHeight());

        int boxWidth = 720;
        int boxHeight = 610;

        int boxX = (getWidth() - boxWidth) / 2;
        int boxY = (getHeight() - boxHeight) / 2;

        // Main shop panel
        g.setColor(new Color(25, 20, 28));
        g.fillRect(boxX, boxY, boxWidth, boxHeight);

        g.setColor(new Color(190, 145, 75));
        g.setStroke(new BasicStroke(2));
        g.drawRect(boxX, boxY, boxWidth, boxHeight);

        // Shop title
        g.setFont(new Font("Monospaced", Font.BOLD, 30));
        g.setColor(new Color(230, 185, 100));

        drawCentered(
                g,
                "DUNGEON SHOP",
                getWidth() / 2,
                boxY + 55
        );

        // Balance
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(gold);

        drawCentered(
                g,
                "YOUR COINS: " + game.getCoins(),
                getWidth() / 2,
                boxY + 88
        );

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(155, 140, 145));

        drawCentered(
                g,
                "BUY SUPPLIES AND UPGRADES FOR YOUR NEXT ADVENTURE",
                getWidth() / 2,
                boxY + 112
        );

        // Item cards
        int cardX = boxX + 45;
        int cardWidth = boxWidth - 90;
        int cardHeight = 63;
        int startY = boxY + 135;
        int gap = 11;

        for (int i = 0; i < shopNames.length; i++) {

            int cardY = startY + i * (cardHeight + gap);

            shopItemButtons[i] = new Rectangle(
                    cardX,
                    cardY,
                    cardWidth,
                    cardHeight
            );

            boolean affordable =
                    game.getCoins() >= shopPrices[i];

            // Card background
            g.setColor(new Color(38, 31, 39));

            g.fillRect(
                    cardX,
                    cardY,
                    cardWidth,
                    cardHeight
            );

            // Border
            g.setColor(
                    affordable
                            ? new Color(155, 115, 65)
                            : new Color(75, 65, 70)
            );

            g.drawRect(
                    cardX,
                    cardY,
                    cardWidth,
                    cardHeight
            );

            // Item name
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(230, 195, 145));

            g.drawString(
                    shopNames[i],
                    cardX + 17,
                    cardY + 25
            );

            // Description
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(160, 150, 155));

            g.drawString(
                    shopDescriptions[i],
                    cardX + 17,
                    cardY + 45
            );

            // Price
            g.setFont(new Font("Monospaced", Font.BOLD, 12));

            g.setColor(gold);

            g.drawString(
                    shopPrices[i] + " COINS",
                    cardX + cardWidth - 180,
                    cardY + 25
            );

            // Buy status
            g.setFont(new Font("Monospaced", Font.BOLD, 10));

            g.setColor(
                    affordable
                            ? new Color(130, 210, 140)
                            : new Color(145, 110, 110)
            );

            g.drawString(
                    affordable ? "AVAILABLE" : "NEED COINS",
                    cardX + cardWidth - 180,
                    cardY + 45
            );
        }

        // Current feedback
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(220, 185, 120));

        drawCentered(
                g,
                shopMessage,
                getWidth() / 2,
                boxY + 555
        );

        // Back button
        shopBackButton = new Rectangle(
                getWidth() / 2 - 110,
                boxY + 565,
                220,
                32
        );

        g.setColor(new Color(48, 35, 42));

        g.fillRect(
                shopBackButton.x,
                shopBackButton.y,
                shopBackButton.width,
                shopBackButton.height
        );

        g.setColor(new Color(165, 125, 85));

        g.drawRect(
                shopBackButton.x,
                shopBackButton.y,
                shopBackButton.width,
                shopBackButton.height
        );

        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(220, 190, 145));

        drawCentered(
                g,
                "BACK TO VICTORY",
                getWidth() / 2,
                shopBackButton.y + 21
        );
    }

    // =================================
    // VICTORY BUTTON DESIGN
    // =================================

    private void drawVictoryButton(
            Graphics2D g,
            Rectangle bounds,
            String text,
            boolean primary
    ) {

        g.setColor(
                primary
                        ? new Color(78, 52, 30)
                        : new Color(42, 31, 38)
        );

        g.fillRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );

        g.setColor(
                primary
                        ? new Color(220, 175, 90)
                        : new Color(130, 90, 75)
        );

        g.drawRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );

        g.setFont(new Font("Monospaced", Font.BOLD, 12));

        g.setColor(
                primary
                        ? new Color(255, 220, 150)
                        : new Color(205, 175, 145)
        );

        drawCentered(
                g,
                text,
                bounds.x + bounds.width / 2,
                bounds.y + 29
        );
    }

    // =================================
    // GAME OVER
    // =================================

    private void drawDeathScreen(Graphics2D g) {

        g.setColor(new Color(5, 3, 9, 220));
        g.fillRect(0, 0, getWidth(), getHeight());

        int boxWidth = 500;
        int boxHeight = 315;

        int boxX = (getWidth() - boxWidth) / 2;
        int boxY = (getHeight() - boxHeight) / 2;

        g.setColor(new Color(24, 17, 25));
        g.fillRect(boxX, boxY, boxWidth, boxHeight);

        g.setColor(new Color(135, 75, 65));
        g.setStroke(new BasicStroke(2));
        g.drawRect(boxX, boxY, boxWidth, boxHeight);

        g.setFont(new Font("Monospaced", Font.BOLD, 42));
        g.setColor(new Color(190, 55, 65));

        drawCentered(g, "YOU DIED", getWidth() / 2, boxY + 80);

        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(new Color(180, 150, 150));

        drawCentered(
                g,
                "YOUR MEMORIES HAVE FADED",
                getWidth() / 2,
                boxY + 117
        );

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(gold);

        drawCentered(
                g,
                "ROOM REACHED: " + game.getRoom(),
                getWidth() / 2,
                boxY + 155
        );

        drawCentered(
                g,
                "ENEMIES DEFEATED: " + game.getDefeatedEnemies(),
                getWidth() / 2,
                boxY + 180
        );

        int buttonWidth = 180;
        int buttonHeight = 43;

        restartButton = new Rectangle(
                getWidth() / 2 - buttonWidth - 10,
                boxY + 225,
                buttonWidth,
                buttonHeight
        );

        menuButton = new Rectangle(
                getWidth() / 2 + 10,
                boxY + 225,
                buttonWidth,
                buttonHeight
        );

        drawDeathButton(g, restartButton, "RESTART");
        drawDeathButton(g, menuButton, "MAIN MENU");

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(135, 120, 130));

        drawCentered(
                g,
                "PRESS R TO BEGIN A NEW JOURNEY",
                getWidth() / 2,
                boxY + 290
        );
    }

    // =================================
    // DEATH BUTTON DESIGN
    // =================================

    private void drawDeathButton(
            Graphics2D g,
            Rectangle bounds,
            String text
    ) {

        g.setColor(new Color(48, 29, 38));

        g.fillRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );

        g.setColor(new Color(165, 105, 75));

        g.drawRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(230, 185, 125));

        drawCentered(
                g,
                text,
                bounds.x + bounds.width / 2,
                bounds.y + 27
        );
    }

    // =================================
    // CENTER TEXT
    // =================================

    private void drawCentered(
            Graphics2D g,
            String text,
            int centerX,
            int y
    ) {

        FontMetrics metrics = g.getFontMetrics();

        int x = centerX - metrics.stringWidth(text) / 2;

        g.drawString(text, x, y);
    }
}
