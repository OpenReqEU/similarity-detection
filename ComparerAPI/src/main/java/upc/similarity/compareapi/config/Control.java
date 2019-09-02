package upc.similarity.compareapi.config;

import java.util.logging.*;

public class Control {

    private static Control instance = new Control();
    private final Logger log = Logger.getLogger("CompareComponent.Control");

    public static Control getInstance() {
        return instance;
    }

    private Control() {
        log.setLevel(Level.ALL);
    }

    public void showInfoMessage(String text) {
        log.log(Level.INFO, text);
    }

    public void showWarnMessage(String text) {
        log.log(Level.WARNING, text);
    }

    public void showErrorMessage(String text) {
        log.log(Level.SEVERE, text);
    }

}
