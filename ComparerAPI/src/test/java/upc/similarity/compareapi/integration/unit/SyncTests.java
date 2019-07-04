package upc.similarity.compareapi.integration.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.service.CompareServiceImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SyncTests {

    @LocalServerPort
    int port = 9405;

    @Test
    public void getAccessToUpdate() {
        try {
            CompareServiceImpl compareService = new CompareServiceImpl();
            class Control {
                public volatile boolean flag = true;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", "1234");
                    compareService.releaseAccessToUpdate("UPC", "1234");
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UB", "1234");
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            Thread thread3 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", "1234");
                    compareService.releaseAccessToUpdate("UPC", "1234");
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

            ConcurrentHashMap<String, AtomicBoolean> concurrentMap = compareService.getConcurrentMap();
            assertFalse(concurrentMap.get("UPC").get());
            assertTrue(concurrentMap.get("UB").get());
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
                public volatile boolean flag2 = false;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC", "1234");
                    compareService.getAccessToUpdate("UPC", "1234");
                } catch (InternalErrorException e) {
                    control.flag1 = true;
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UB", "1234");
                    compareService.removeOrganizationLock("UB");
                    compareService.releaseAccessToUpdate("UB", "1234");
                } catch (Exception e) {
                    control.flag2 = true;
                }
            });

            thread1.start();
            thread2.start();

            thread1.join();
            thread2.join();

            assertTrue(control.flag1);
            assertTrue(control.flag1);

            ConcurrentHashMap<String, AtomicBoolean> concurrentMap = compareService.getConcurrentMap();
            assertTrue(concurrentMap.get("UPC").get());
            assertNull(concurrentMap.get("UB"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
