package upc.similarity.compareapi.integration.unit;

import org.json.JSONArray;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.service.ScheduledTasks;
import upc.similarity.compareapi.util.DatabaseOperations;
import upc.similarity.compareapi.util.Tfidf;
import upc.similarity.compareapi.util.Time;

import java.io.File;
import java.time.Clock;
import java.time.Duration;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class ScheduledTests {

    @BeforeClass
    public static void createTestDB() throws Exception {
        SQLiteDatabase.setDbPath("../testing/integration/test_database/");
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        Tfidf.getInstance().setCutOffDummy(true);
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        File file = new File("../testing/integration/test_database/main.db");
        boolean result = file.delete();
    }

    @Test
    public void deleteOldResponses() throws Exception {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Time time = Time.getInstance();
        databaseOperations.generateResponse("UPC", "1234", "Test");
        databaseOperations.generateResponsePage("1234", "UPC", new JSONArray(), "dependencies");
        databaseOperations.finishComputation("UPC", "1234");
        time.setClock(Clock.offset(time.getClock(), Duration.ofDays(-8)));
        databaseOperations.generateResponse("UB", "4321", "Test");
        databaseOperations.generateResponsePage("4321", "UB", new JSONArray(), "dependencies");
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
}
