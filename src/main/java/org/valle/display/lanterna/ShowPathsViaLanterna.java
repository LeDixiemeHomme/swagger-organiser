package org.valle.display.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.valle.display.ShowPaths;

import java.util.List;

public class ShowPathsViaLanterna implements ShowPaths {

    private final LanternaSingleton lanternaSingleton;

    public ShowPathsViaLanterna() {
        lanternaSingleton = LanternaSingleton.getInstance();
    }

    @Override
    public void display(List<String> paths) {
        try {
            lanternaSingleton.write(paths.getFirst());
            // Création de l'UI Lanterna
            DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
            Screen screen = terminalFactory.createScreen();
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            BasicWindow window = new BasicWindow("Endpoints Swagger");
            Panel panel = new Panel();
            panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
            TextBox textBox = new TextBox(paths.getFirst())
                    .setReadOnly(true)
                    .setVerticalFocusSwitching(false)
                    .setPreferredSize(new TerminalSize(80, 20));
            panel.addComponent(textBox);
            panel.addComponent(new Button("Quitter", window::close));
            window.setComponent(panel);
            gui.addWindowAndWait(window);
            screen.stopScreen();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage Lanterna : " + e.getMessage());
        }
    }
}
