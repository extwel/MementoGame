
package com.mementogame.model;

import java.io.Serializable;

public class GameState implements Serializable {

    private static final long serialVersionUID = 3L;

    private final int health;
    private final int maxHealth;
    private final int level;
    private final int xp;
    private final int coins;
    private final int room;

    private final int playerX;
    private final int playerY;

    private final int potions;
    private final int defeatedEnemies;

    private final int enemyX;
    private final int enemyY;
    private final int enemyHealth;
    private final int enemyMaxHealth;

    private final int swordDamage;

    public GameState(
            int health,
            int maxHealth,
            int level,
            int xp,
            int coins,
            int room,
            int playerX,
            int playerY,
            int potions,
            int defeatedEnemies,
            int enemyX,
            int enemyY,
            int enemyHealth,
            int enemyMaxHealth,
            int swordDamage
    ) {

        this.health = health;
        this.maxHealth = maxHealth;
        this.level = level;
        this.xp = xp;
        this.coins = coins;
        this.room = room;

        this.playerX = playerX;
        this.playerY = playerY;

        this.potions = potions;
        this.defeatedEnemies = defeatedEnemies;

        this.enemyX = enemyX;
        this.enemyY = enemyY;
        this.enemyHealth = enemyHealth;
        this.enemyMaxHealth = enemyMaxHealth;

        this.swordDamage = swordDamage;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getCoins() { return coins; }
    public int getRoom() { return room; }

    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }

    public int getPotions() { return potions; }
    public int getDefeatedEnemies() { return defeatedEnemies; }

    public int getEnemyX() { return enemyX; }
    public int getEnemyY() { return enemyY; }
    public int getEnemyHealth() { return enemyHealth; }
    public int getEnemyMaxHealth() { return enemyMaxHealth; }

    public int getSwordDamage() { return swordDamage; }
}
