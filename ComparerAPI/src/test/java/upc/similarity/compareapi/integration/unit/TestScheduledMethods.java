package upc.similarity.compareapi.integration.unit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.service.ScheduledTasks;
import upc.similarity.compareapi.util.Time;

import java.io.File;
import java.time.Clock;
import java.time.Duration;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class TestScheduledMethods {

    private static Constants constants = null;

    @BeforeClass
    public static void createTestDB() throws Exception {
        constants = Constants.getInstance();
        constants.setDatabasePath("../testing/integration/test_database/");
        constants.getDatabaseModel().clearDatabase();
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        constants.getDatabaseModel().clearDatabase();
        File file = new File("../testing/integration/test_database/main.db");
        boolean result = file.delete();
    }

    @Test
    public void deleteOldResponses() throws Exception {
        DatabaseModel databaseOperations = constants.getDatabaseModel();
        Time time = Time.getInstance();
        databaseOperations.saveResponse("UPC", "1234", "Test");
        generateResponsePage("UPC", "1234", new JSONArray(), "dependencies",databaseOperations);
        databaseOperations.finishComputation("UPC", "1234");
        time.setClock(Clock.offset(time.getClock(), Duration.ofDays(-8)));
        databaseOperations.saveResponse("UB", "4321", "Test");
        generateResponsePage("UB", "4321", new JSONArray(), "dependencies",databaseOperations);
        databaseOperations.finishComputation("UB", "4321");
        time.setClock(Clock.offset(time.getClock(), Duration.ofDays(8)));
        ScheduledTasks scheduledTasks = new ScheduledTasks();
        scheduledTasks.deleteOldResponses();
        databaseOperations.getResponsePage("UPC", "1234");
        boolean correct = false;
        try {
            databaseOperations.getResponsePage("UB", "4321");
        } catch (NotFoundException e) {
            correct = true;
        }
        assertTrue(correct);
    }

    private void generateResponsePage(String organizationId, String responseId, JSONArray array, String arrayName, DatabaseModel databaseModel) throws InternalErrorException {
        try {
            JSONObject json = new JSONObject();
            json.put("status", 200);
            json.put(arrayName, array);
            databaseModel.saveResponsePage(organizationId, responseId, json.toString());
        } catch (Exception e) {
            throw new InternalErrorException("Error while saving response");
        }
    }
}
