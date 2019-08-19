package upc.similarity.similaritydetectionapi.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;

public class Control {

    private static Control instance = new Control();
    private final Logger log = Logger.getLogger("MainComponent.Control");

    public static Control getInstance() {
        return instance;
    }

    private Control() {
        log.setLevel(Level.ALL);
    }

    public void showInfoMessage(String text) {
        log.log(Level.INFO, text);
    }

    public void showErrorMessage(String text) {
        log.log(Level.SEVERE, text);
    }

    public void showStackTrace(Exception e) {
        log.log(Level.SEVERE, getStackTrace(e));
    }

    private String getStackTrace(Exception e) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        e.printStackTrace(pWriter);
        return sWriter.toString();
    }




}
