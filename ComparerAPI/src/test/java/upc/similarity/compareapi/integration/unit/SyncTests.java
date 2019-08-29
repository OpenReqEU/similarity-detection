package upc.similarity.compareapi.integration.unit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.service.CompareServiceImpl;
import upc.similarity.compareapi.util.Tfidf;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class SyncTests {

    @BeforeClass
    public static void createTestDB() throws Exception {
        SQLiteDatabase.setDbPath("../testing/integration/test_database/");
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        Tfidf.setCutOffDummy(true);
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
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
}
