
package com.mementogame.memento;

import com.mementogame.model.GameState;

import java.io.Serializable;

public class Memento implements Serializable {

    private static final long serialVersionUID = 1L;

    private final GameState snapshot;

    public Memento(GameState snapshot) {
        this.snapshot = snapshot;
    }

    public GameState getSnapshot() {
        return snapshot;
    }
}
