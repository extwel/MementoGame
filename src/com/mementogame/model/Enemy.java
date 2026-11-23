
package com.mementogame.model;

public class Enemy {

    private int x;
    private int y;
    private int health;
    private final int maxHealth;

    public Enemy(int x, int y, int health, int maxHealth) {
        this.x = x;
        this.y = y;
        this.maxHealth = maxHealth;
        this.health = Math.max(0, Math.min(health, maxHealth));
    }

    public void moveTowards(int targetX, int targetY) {

        if (x != targetX) {
            x += Integer.compare(targetX, x);
        } else if (y != targetY) {
            y += Integer.compare(targetY, y);
        }
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public boolean isAlive() {
        return health > 0;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }
}
