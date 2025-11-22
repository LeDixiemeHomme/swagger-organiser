package org.valle.present.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBoxList;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import lombok.extern.slf4j.Slf4j;
import org.valle.present.ShowEndpoints;
import org.valle.process.models.EndPoint;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ShowEndpointsViaLanterna implements ShowEndpoints {

    private final LanternaSingleton lanternaSingleton;

    public ShowEndpointsViaLanterna() {
        lanternaSingleton = LanternaSingleton.getInstance();
    }

    @Override
    public void display(Set<EndPoint> endPoints) {

        Screen screen;
        try {
            screen = new DefaultTerminalFactory().createScreen();
            screen.startScreen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        BasicWindow window = new BasicWindow("Formulaire (multi-sélection)");

        // Mise en page type “formulaire”
        Panel form = new Panel(new GridLayout(2));
        form.setPreferredSize(new TerminalSize(50, 15));

        // Label + liste de cases à cocher
        form.addComponent(new Label("Fonctionnalités :"));
        CheckBoxList<String> features = new CheckBoxList<>();
        endPoints.forEach(endPoint -> {
            features.addItem(endPoint.toString());
        });
        form.addComponent(features);

        // Label + champ texte simple (optionnel)
        form.addComponent(new Label("Nom du projet :"));
        TextBox projectName = new TextBox().setText("demo-lanterna");
        form.addComponent(projectName);

        // Boutons
        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        Button submit = new Button("Valider", () -> {
            List<String> selected = features.getCheckedItems();
            String chosen = selected.isEmpty()
                    ? "(aucune)"
                    : selected.stream().collect(Collectors.joining(", "));
            MessageDialog.showMessageDialog(
                    gui,
                    "Résumé",
                    "Projet : " + projectName.getText() + "\n" +
                            "Options : " + chosen
            );
        });
        Button close = new Button("Fermer", () -> window.close());
        buttons.addComponent(submit);
        buttons.addComponent(close);

        // Le panneau principal (vertical)
        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(form);
        root.addComponent(buttons);

        window.setComponent(root);
        gui.addWindowAndWait(window);
        try {
            screen.stopScreen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
