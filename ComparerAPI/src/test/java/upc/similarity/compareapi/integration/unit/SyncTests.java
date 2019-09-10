package upc.similarity.compareapi.integration.unit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.service.CompareServiceImpl;
import upc.similarity.compareapi.util.DatabaseOperations;
import upc.similarity.compareapi.util.Tfidf;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class SyncTests {

    private static int sleepTime;

    @BeforeClass
    public static void createTestDB() throws Exception {
        sleepTime = Constants.getInstance().getSleepTime();
        Constants.getInstance().setSleepTime(1);
        SQLiteDatabase.setDbPath("../testing/integration/test_database/");
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        Tfidf.getInstance().setCutOffDummy(true);
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        Constants.getInstance().setSleepTime(sleepTime);
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        File file = new File("../testing/integration/test_database/main.db");
        boolean result = file.delete();
    }

    @Test
    public void getAccessToUpdateOrganization() {
        try {
            CompareServiceImpl compareService = new CompareServiceImpl();
            class Control {
                public volatile boolean flag = true;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", null);
                    compareService.releaseAccessToUpdate("UPC", null);
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UB", null);
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            Thread thread3 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", null);
                    compareService.releaseAccessToUpdate("UPC", null);
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            thread1.start();
            thread2.start();
            thread3.start();

            thread1.join();
            thread2.join();
            thread3.join();

            assertTrue(control.flag);

            ConcurrentMap<String, Lock> concurrentMap = compareService.getConcurrentMap();
            assertTrue(concurrentMap.get("UPC").tryLock());
            assertFalse(concurrentMap.get("UB").tryLock());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void lockOrganization() {
        try {
            CompareServiceImpl compareService = new CompareServiceImpl();
            class Control {
                public volatile boolean flag1 = false;
                public volatile boolean flag2 = false;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", null);
                } catch (Exception e) {
                    control.flag1 = true;
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", null);
                } catch (InternalErrorException e) {
                    control.flag2 = false;
                }catch (NotFinishedException e) {
                    control.flag2 = true;
                }
            });

            thread1.start();
            Thread.sleep(1000);
            thread2.start();

            thread1.join();
            thread2.join();

            assertFalse(control.flag1);
            assertTrue(control.flag2);

            ConcurrentMap<String, Lock> concurrentMap = compareService.getConcurrentMap();
            assertFalse(concurrentMap.get("UPC").tryLock());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void removeOrganization() {
        try {
            CompareServiceImpl compareService = new CompareServiceImpl();
            class Control {
                public volatile boolean flag1 = false;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UB", null);
                    compareService.removeOrganizationLock("UB");
                    compareService.releaseAccessToUpdate("UB", null);
                } catch (InternalErrorException e) {
                    control.flag1 = true;
                }catch (NotFinishedException e) {
                    control.flag1 = false;
                }
            });

            thread1.start();

            thread1.join();

            assertTrue(control.flag1);

            ConcurrentMap<String, Lock> concurrentMap = compareService.getConcurrentMap();
            assertNull(concurrentMap.get("UB"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExceptions() {
        try {
            CompareServiceImpl compareService = new CompareServiceImpl();
            SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
            sqLiteDatabase.getAccessToMainDb();
            DatabaseOperations.getInstance().setDatabaseModel(sqLiteDatabase);
            class Control {
                public volatile boolean flag = false;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService.batchProcess("1234", "UPC", new Clusters());
                } catch (InternalErrorException e1) {
                    control.flag = true;
                } catch (Exception e2) {
                    control.flag = false;
                }
            });

            thread1.start();
            thread1.join();
            assertTrue(control.flag);
            control.flag = false;

            sqLiteDatabase.releaseAccessToMainDb();
            compareService.getAccessToUpdate("UPC", "12345");

            thread1 = new Thread(() -> {
                try {
                    compareService.batchProcess("123456", "UPC", new Clusters());
                } catch (NotFinishedException e1) {
                    control.flag = true;
                } catch (Exception e2) {
                    control.flag = false;
                }
            });

            thread1.start();
            thread1.join();
            assertTrue(control.flag);
            assertEquals("","{\"error\":\"Not finished\",\"message\":\"There is another computation in the same organization with write or update rights that has not finished yet\",\"status\":423}",compareService.getResponsePage("UPC", "123456"));
            control.flag = false;

            sqLiteDatabase = new SQliteNew();
            DatabaseOperations.getInstance().setDatabaseModel(sqLiteDatabase);

            thread1 = new Thread(() -> {
                try {
                    compareService.batchProcess("1234567", "UPC", new Clusters());
                } catch (InternalErrorException e1) {
                    //check no error messages are shown in console
                    control.flag = true;
                } catch (Exception e2) {
                    control.flag = false;
                }
            });

            thread1.start();
            thread1.join();
            assertTrue(control.flag);

            assertEquals("","{}",compareService.getResponsePage("UPC", "123456"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SQliteNew extends SQLiteDatabase {
        private String dbMainName = "main";
        public SQliteNew() throws ClassNotFoundException {
            Class.forName("org.sqlite.JDBC");
        }
        @Override
        public void saveResponse(String organizationId, String responseId, String methodName) throws SQLException, InternalErrorException {
            String sql = "INSERT INTO responses(organizationId, responseId, actualPage, maxPages, finished, startTime, finalTime, methodName) VALUES (?,?,?,?,?,?,?,?)";

            getAccessToMainDb();
            try (Connection conn =  DriverManager.getConnection("jdbc:sqlite:../testing/integration/test_database/main.db");
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,organizationId);
                ps.setString(2,responseId);
                ps.setInt(3,0);
                ps.setInt(4,0);
                ps.setInt(5,0);
                ps.setLong(6, 123);
                ps.setLong(7, 123);
                ps.setString(8, methodName);
                ps.execute();
            } finally {
                releaseAccessToMainDb();
            }

            /*
            New code
             */

            Thread thread1 = new Thread(() -> {
                try {
                    getAccessToMainDb();
                } catch (Exception e) {
                    Control.getInstance().showErrorMessage("Here " + e.getMessage());
                }
            });
            thread1.start();
        }
    }
}
