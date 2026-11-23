
package com.mementogame.game;

import com.mementogame.memento.Memento;
import com.mementogame.model.GameState;
import com.mementogame.model.Enemy;

public class Game {

    private int health = 100;
    private int maxHealth = 100;
    private int level = 1;
    private int xp = 0;
    private int coins = 0;
    private int room = 1;

    private int playerX = 5;
    private int playerY = 5;

    private int potions = 3;
    private int defeatedEnemies = 0;

    private int swordDamage = 35;

    private Enemy enemy = new Enemy(14, 5, 80, 80);

    private String message = "Explore the dungeon...";

    private boolean blocking = false;

    private long nextEnemyActionAt =
            System.currentTimeMillis() + 700;

    private static final long ENEMY_MOVE_DELAY = 650;
    private static final long ENEMY_ATTACK_DELAY = 950;

    // =================================
    // MEMENTO: CREATE SNAPSHOT
    // =================================

    public Memento createMemento() {

        GameState state = new GameState(
                health,
                maxHealth,
                level,
                xp,
                coins,
                room,
                playerX,
                playerY,
                potions,
                defeatedEnemies,
                enemy.getX(),
                enemy.getY(),
                enemy.getHealth(),
                enemy.getMaxHealth(),
                swordDamage
        );

        return new Memento(state);
    }

    // =================================
    // MEMENTO: RESTORE SNAPSHOT
    // =================================

    public void restoreMemento(Memento memento) {

        GameState state = memento.getSnapshot();

        health = state.getHealth();
        maxHealth = state.getMaxHealth();

        level = state.getLevel();
        xp = state.getXp();
        coins = state.getCoins();

        room = state.getRoom();

        playerX = state.getPlayerX();
        playerY = state.getPlayerY();

        potions = state.getPotions();
        defeatedEnemies = state.getDefeatedEnemies();

        swordDamage = state.getSwordDamage();

        enemy = new Enemy(
                state.getEnemyX(),
                state.getEnemyY(),
                state.getEnemyHealth(),
                state.getEnemyMaxHealth()
        );

        blocking = false;

        nextEnemyActionAt =
                System.currentTimeMillis() + 700;

        message = isGameOver()
                ? "YOU DIED! Start a new journey."
                : "Memory successfully restored!";
    }

    // =================================
    // PLAYER MOVEMENT
    // =================================

    public void move(int dx, int dy) {

        if (isGameOver()) return;

        int nextX = playerX + dx;
        int nextY = playerY + dy;

        if (nextX < 1 || nextX > 18 ||
                nextY < 1 || nextY > 10) {

            message = "You cannot move there!";
            return;
        }

        if (enemy.isAlive()
                && nextX == enemy.getX()
                && nextY == enemy.getY()) {

            message = "The Shadow blocks your path!";
            return;
        }

        playerX = nextX;
        playerY = nextY;

        message = "You move through the dungeon.";
    }

    // =================================
    // PLAYER ATTACK
    // =================================

    public void attack() {

        if (isGameOver()) return;

        if (blocking) {
            message = "Release SHIFT to attack!";
            return;
        }

        if (!enemy.isAlive()) {
            message = "The room is clear!";
            return;
        }

        int distance =
                Math.abs(playerX - enemy.getX())
                        + Math.abs(playerY - enemy.getY());

        if (distance > 1) {

            message = "Too far! Move closer to the Shadow.";
            return;
        }

        enemy.takeDamage(swordDamage);

        if (!enemy.isAlive()) {

            defeatedEnemies++;

            // Boss reward
            coins += 60;
            xp += 30;

            message = "SHADOW DEFEATED! +60 COINS, +30 XP";

            if (xp >= 100) {

                level++;
                xp -= 100;

                maxHealth += 10;
                health = maxHealth;

                message = "LEVEL UP! You reached level " + level;
            }

        } else {

            message = "You hit the Shadow for "
                    + swordDamage + " damage!";
        }
    }

    // =================================
    // ENEMY AI
    // =================================

    public void update() {

        if (isGameOver() || !enemy.isAlive()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now < nextEnemyActionAt) {
            return;
        }

        int distance =
                Math.abs(playerX - enemy.getX())
                        + Math.abs(playerY - enemy.getY());

        if (distance <= 1) {

            if (blocking) {

                message = "SHIELD BLOCKED THE ATTACK!";

            } else {

                health = Math.max(0, health - 10);

                message = isGameOver()
                        ? "YOU DIED! Your memories have faded."
                        : "SHADOW ATTACKS! -10 HP";
            }

            nextEnemyActionAt = now + ENEMY_ATTACK_DELAY;

        } else {

            enemy.moveTowards(playerX, playerY);

            message = "The Shadow is approaching...";

            nextEnemyActionAt = now + ENEMY_MOVE_DELAY;
        }
    }

    // =================================
    // DEFENSE
    // =================================

    public void setBlocking(boolean value) {

        if (isGameOver()) {
            blocking = false;
            return;
        }

        blocking = value;

        if (blocking) {
            message = "SHIELD RAISED! DEFENDING...";
        } else {
            message = "Shield lowered.";
        }
    }

    public boolean isBlocking() {
        return blocking;
    }

    // =================================
    // POTION
    // =================================

    public void usePotion() {

        if (isGameOver()) return;

        if (potions <= 0) {
            message = "You have no potions!";
            return;
        }

        if (health >= maxHealth) {
            message = "Your health is already full!";
            return;
        }

        health = Math.min(maxHealth, health + 30);
        potions--;

        message = "Potion used! +30 HP";
    }

    // =================================
    // SHOP: HEALTH POTION
    // =================================

    public boolean buyHealthPotion() {

        int price = 30;

        if (!canSpend(price)) return false;

        if (health >= maxHealth) {

            message = "Your health is already full!";
            return false;
        }

        coins -= price;
        health = Math.min(maxHealth, health + 30);

        message = "Health Potion purchased! +30 HP";

        return true;
    }

    // =================================
    // SHOP: STRONG POTION
    // =================================

    public boolean buyStrongPotion() {

        int price = 50;

        if (!canSpend(price)) return false;

        if (health >= maxHealth) {

            message = "Your health is already full!";
            return false;
        }

        coins -= price;
        health = Math.min(maxHealth, health + 60);

        message = "Strong Potion purchased! +60 HP";

        return true;
    }

    // =================================
    // SHOP: MAX HEALTH UPGRADE
    // =================================

    public boolean buyMaxHealthUpgrade() {

        int price = 100;

        if (!canSpend(price)) return false;

        if (maxHealth >= 200) {

            message = "Maximum health upgrade reached!";
            return false;
        }

        coins -= price;

        maxHealth = Math.min(200, maxHealth + 20);
        health = Math.min(maxHealth, health + 20);

        message = "MAX HEALTH UPGRADED! +20 MAX HP";

        return true;
    }

    // =================================
    // SHOP: SWORD UPGRADE
    // =================================

    public boolean buySwordUpgrade() {

        int price = 150;

        if (!canSpend(price)) return false;

        if (swordDamage >= 50) {

            message = "Your sword is already upgraded!";
            return false;
        }

        coins -= price;
        swordDamage = 50;

        message = "SWORD UPGRADED! DAMAGE: 50";

        return true;
    }

    // =================================
    // SHOP: EXTRA POTION
    // =================================

    public boolean buyExtraPotion() {

        int price = 40;

        if (!canSpend(price)) return false;

        coins -= price;
        potions++;

        message = "Extra Potion added to inventory!";

        return true;
    }

    // =================================
    // CHECK MONEY
    // =================================

    private boolean canSpend(int price) {

        if (coins < price) {

            message = "Not enough coins!";

            return false;
        }

        return true;
    }

    // =================================
    // NEXT ROOM
    // =================================

    public void nextRoom() {

        if (isGameOver()) return;

        if (enemy.isAlive()) {

            message = "Defeat the Shadow before leaving!";
            return;
        }

        room++;

        playerX = 5;
        playerY = 5;

        int enemyHealth = 80 + (room - 1) * 20;

        enemy = new Enemy(
                14,
                5,
                enemyHealth,
                enemyHealth
        );

        blocking = false;

        nextEnemyActionAt =
                System.currentTimeMillis() + 700;

        message = "ROOM " + room
                + " - A stronger enemy appears!";
    }

    // =================================
    // RESTART
    // =================================

    public void restart() {

        health = 100;
        maxHealth = 100;

        level = 1;
        xp = 0;
        coins = 0;

        room = 1;

        playerX = 5;
        playerY = 5;

        potions = 3;
        defeatedEnemies = 0;

        swordDamage = 35;

        enemy = new Enemy(14, 5, 80, 80);

        blocking = false;

        nextEnemyActionAt =
                System.currentTimeMillis() + 700;

        message = "A new journey begins...";
    }

    // =================================
    // GAME OVER
    // =================================

    public boolean isGameOver() {
        return health <= 0;
    }

    // =================================
    // GETTERS
    // =================================

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public int getLevel() { return level; }
    public int getXp() { return xp; }

    public int getCoins() { return coins; }
    public int getRoom() { return room; }

    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }

    public int getPotions() { return potions; }

    public int getDefeatedEnemies() {
        return defeatedEnemies;
    }

    public int getSwordDamage() {
        return swordDamage;
    }

    public int getEnemyX() { return enemy.getX(); }
    public int getEnemyY() { return enemy.getY(); }

    public int getEnemyHealth() {
        return enemy.getHealth();
    }

    public int getEnemyMaxHealth() {
        return enemy.getMaxHealth();
    }

    public boolean isEnemyAlive() {
        return enemy.isAlive();
    }

    public String getMessage() {
        return message;
    }
}
