package org.valle.present.lanterna;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import lombok.Getter;

public class LanternaSingleton {

    private static LanternaSingleton instance;

    @Getter
    private final DefaultTerminalFactory terminal;

    private LanternaSingleton() {
        terminal = new DefaultTerminalFactory();
    }

    public static synchronized LanternaSingleton getInstance() {
        if (instance == null) {
            instance = new LanternaSingleton();
        }
        return instance;
    }
}