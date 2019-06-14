package upc.similarity.similaritydetectionapi.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;

public class Control {

    private static Control instance = new Control();
    private final Logger LOG = Logger.getLogger("MainComponent.Control");

    public static Control getInstance() {
        return instance;
    }

    private Control() {
        Handler consoleHandler = new ConsoleHandler();
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        consoleHandler.setFormatter(simpleFormatter);
        consoleHandler.setLevel(Level.ALL);
        LOG.addHandler(consoleHandler);
    }

    public void showInfoMessage(String text) {
        LOG.log(Level.INFO, text);
    }

    public void showErrorMessage(String text) {
        LOG.log(Level.SEVERE, text);
    }

    public void showStackTrace(Exception e) {
        LOG.log(Level.SEVERE, getStackTrace(e));
    }

    private String getStackTrace(Exception e) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        e.printStackTrace(pWriter);
        return sWriter.toString();
    }




}
