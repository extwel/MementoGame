
package com.mementogame.memento;

import com.mementogame.game.Game;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveManager {

    private static final int MAX_SLOTS = 3;

    private final Path saveDirectory =
            Path.of("saves");

    public SaveManager() {
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create save directory", e);
        }
    }

    public void save(Game game, int slot) throws IOException {

        validateSlot(slot);

        Memento memento = game.createMemento();

        Path file = getSlotPath(slot);

        try (ObjectOutputStream output =
                     new ObjectOutputStream(
                             Files.newOutputStream(file))) {

            output.writeObject(memento);
        }
    }

    public void load(Game game, int slot)
            throws IOException, ClassNotFoundException {

        validateSlot(slot);

        Path file = getSlotPath(slot);

        if (!Files.exists(file)) {
            throw new FileNotFoundException(
                    "Save slot " + slot + " is empty."
            );
        }

        try (ObjectInputStream input =
                     new ObjectInputStream(
                             Files.newInputStream(file))) {

            Memento memento = (Memento) input.readObject();

            game.restoreMemento(memento);
        }
    }

    public boolean hasSave(int slot) {
        validateSlot(slot);
        return Files.exists(getSlotPath(slot));
    }

    public void deleteSave(int slot) throws IOException {
        validateSlot(slot);
        Files.deleteIfExists(getSlotPath(slot));
    }

    private Path getSlotPath(int slot) {
        return saveDirectory.resolve("slot" + slot + ".dat");
    }

    private void validateSlot(int slot) {
        if (slot < 1 || slot > MAX_SLOTS) {
            throw new IllegalArgumentException(
                    "Slot must be between 1 and " + MAX_SLOTS
            );
        }
    }
}
