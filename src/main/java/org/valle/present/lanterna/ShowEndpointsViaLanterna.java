package org.valle.present.lanterna;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import lombok.extern.slf4j.Slf4j;
import org.valle.present.ShowEndpoints;
import org.valle.process.models.EndPoint;

import java.util.Set;

@Slf4j
public class ShowEndpointsViaLanterna implements ShowEndpoints {

    private final LanternaSingleton lanternaSingleton;

    public ShowEndpointsViaLanterna() {
        lanternaSingleton = LanternaSingleton.getInstance();
    }

    @Override
    public void display(Set<EndPoint> endPoints) {
        try {
            // Création de l'UI Lanterna
            DefaultTerminalFactory terminalFactory = lanternaSingleton.getTerminal();
            Screen screen = terminalFactory.createScreen();
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
            BasicWindow window = new BasicWindow("Menu des Endpoints Swagger");
            Panel panel = new Panel();
            panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

            Label titre = new Label("Sélectionnez un endpoint :");
            panel.addComponent(titre);

            // Remplacement de ListBox par une liste de boutons
            final String[] selected = {null};
            Panel endpointsPanel = new Panel();
            endpointsPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
            for (EndPoint path : endPoints) {
                Button endpointButton = new Button(path.toString(), () -> selected[0] = path.toString());
                endpointsPanel.addComponent(endpointButton);
            }
            panel.addComponent(endpointsPanel);

            Panel boutons = new Panel(new LinearLayout(Direction.HORIZONTAL));
            Button valider = new Button("Valider", () -> {
                MessageDialog.showMessageDialog(gui, "Sélection", selected[0] != null ? selected[0] : "Aucune sélection");
            });
            Button quitter = new Button("Quitter", window::close);
            boutons.addComponent(valider);
            boutons.addComponent(quitter);
            panel.addComponent(boutons);

            window.setComponent(panel);
            gui.addWindowAndWait(window);
            screen.stopScreen();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage Lanterna : " + e.getMessage());
        }
    }
}
