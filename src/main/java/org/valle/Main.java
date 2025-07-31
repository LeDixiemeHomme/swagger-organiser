package org.valle;

import org.valle.display.lanterna.ShowPathsViaLanterna;
import org.valle.process.GetAndShowPathsImpl;
import org.valle.provide.jackson.GetAllPathsFromJackson;

public class Main {
    public static void main(String[] args) {
        GetAndShowPathsImpl process = new GetAndShowPathsImpl(
                new GetAllPathsFromJackson(),
                new ShowPathsViaLanterna()
        );
        process.execute();
    }
}