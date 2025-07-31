package org.valle.display.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;

public class LanternaSingleton {

    private static LanternaSingleton instance;

    private final Terminal terminal;

    private LanternaSingleton() {
        try {
            terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(80, 24))
                .createTerminal();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'initialisation du terminal Lanterna", e);
        }
    }

    public static synchronized LanternaSingleton getInstance() {
        if (instance == null) {
            instance = new LanternaSingleton();
        }
        return instance;
    }

    public void write(String text) {
        try {
            for (char c : text.toCharArray()) {
                terminal.putCharacter(c);
            }
            terminal.flush();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'écriture dans le terminal", e);
        }
    }

    public void clear() {
        try {
            terminal.clearScreen();
            terminal.flush();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'effacement du terminal", e);
        }
    }

    public void close() {
        try {
            terminal.close();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la fermeture du terminal", e);
        }
    }
}