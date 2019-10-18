package upc.similarity.compareapi.util;

import java.util.logging.*;

public class Logger {

    private static Logger instance = new Logger();
    private final java.util.logging.Logger log = java.util.logging.Logger.getLogger("CompareComponent.Control");

    public static Logger getInstance() {
        return instance;
    }

    private Logger() {
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
