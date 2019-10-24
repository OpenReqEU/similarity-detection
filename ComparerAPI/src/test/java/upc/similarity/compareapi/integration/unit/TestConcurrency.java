package upc.similarity.compareapi.integration.unit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import upc.similarity.compareapi.clusters_algorithm.ClustersAlgorithm;
import upc.similarity.compareapi.clusters_algorithm.max_graph.ClustersAlgorithmMaxGraph;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.ClustersModelDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.max_graph.ClustersModelDatabaseMaxGraph;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.tf_idf.SimilarityModelDatabaseTfIdf;
import upc.similarity.compareapi.entity.exception.LockedOrganizationException;
import upc.similarity.compareapi.preprocess.PreprocessPipeline;
import upc.similarity.compareapi.preprocess.PreprocessPipelineDefault;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.util.Logger;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.service.CompareServiceImpl;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class TestConcurrency {

    private static int sleepTime;
    private static Constants constants = null;
    private static PreprocessPipeline preprocessPipeline;
    private static SimilarityAlgorithm similarityAlgorithm;
    private static SimilarityModelDatabase similarityModelDatabase;
    private static ClustersAlgorithm clustersAlgorithm;
    private static ClustersModelDatabase clustersModelDatabase;
    private static DatabaseModel databaseModel;

    @BeforeClass
    public static void createTestDB() throws Exception {
        constants = Constants.getInstance();
        preprocessPipeline = new PreprocessPipelineDefault();
        similarityAlgorithm = constants.getSimilarityAlgorithm();
        similarityModelDatabase = constants.getSimilarityModelDatabase();
        clustersAlgorithm = constants.getClustersAlgorithm();
        clustersModelDatabase = constants.getClustersModelDatabase();
        databaseModel = constants.getDatabaseModel();
        sleepTime = constants.getMaxWaitingTime();

        DatabaseModel databaseModel = new SQLiteDatabase("../testing/integration/test_database/",1,similarityModelDatabase, clustersModelDatabase);
        constants.setDatabaseModel(databaseModel);
        constants.setMaxWaitingTime(1);
        constants.getDatabaseModel().clearDatabase();
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        constants.setDatabaseModel(databaseModel);
        constants.setMaxWaitingTime(sleepTime);
        constants.getDatabaseModel().clearDatabase();
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
                    compareService.getAccessToUpdate("UPC");
                    compareService.releaseAccessToUpdate("UPC");
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UB");
                } catch (Exception e) {
                    control.flag = false;
                }
            });

            Thread thread3 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC");
                    compareService.releaseAccessToUpdate("UPC");
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
                    compareService.getAccessToUpdate("UPC");
                } catch (Exception e) {
                    control.flag1 = true;
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    compareService.getAccessToUpdate("UPC");
                } catch (InternalErrorException e) {
                    control.flag2 = false;
                }catch (LockedOrganizationException e) {
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
                    compareService.getAccessToUpdate("UB");
                    compareService.removeOrganizationLock("UB");
                    compareService.releaseAccessToUpdate("UB");
                } catch (InternalErrorException e) {
                    control.flag1 = true;
                } catch (LockedOrganizationException e) {
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

    private CompareServiceImpl compareService1;

    @Test
    public void testExceptions() {
        try {
            SQLiteDatabase sqLiteDatabase = new SQLiteDatabase("../testing/integration/test_database/",1,similarityModelDatabase,clustersModelDatabase);
            sqLiteDatabase.getAccessToMainDb();
            sqLiteDatabase.clearDatabase();
            constants.setDatabaseModel(sqLiteDatabase);
            compareService1 = new CompareServiceImpl();
            class Control {
                public volatile boolean flag = false;
            }
            final Control control = new Control();

            Thread thread1 = new Thread(() -> {
                try {
                    compareService1.batchProcess("1234", "UPC", new Clusters());
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
            compareService1.getAccessToUpdate("UPC");

            thread1 = new Thread(() -> {
                try {
                    compareService1.batchProcess("123456", "UPC", new Clusters());
                } catch (LockedOrganizationException e1) {
                    control.flag = true;
                } catch (Exception e2) {
                    control.flag = false;
                }
            });

            thread1.start();
            thread1.join();
            assertTrue(control.flag);
            assertEquals("","{\"error\":\"Locked\",\"message\":\"There is another computation in the same organization with write or update rights that has not finished yet\",\"status\":423}",compareService1.getResponsePage("UPC", "123456"));
            control.flag = false;

            constants.setDatabaseModel(new SQliteNew("../testing/integration/test_database/",1,similarityModelDatabase,clustersModelDatabase));
            compareService1 = new CompareServiceImpl();

            thread1 = new Thread(() -> {
                try {
                    compareService1.batchProcess("1234567", "UPC", new Clusters());
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

            assertEquals("","{}",compareService1.getResponsePage("UPC", "123456"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SQliteNew extends SQLiteDatabase {
        private String dbMainName = "main";
        public SQliteNew(String dbPath, int sleepTime, SimilarityModelDatabase similarityModelDatabase, ClustersModelDatabase clustersModelDatabase) throws ClassNotFoundException {
            super(dbPath,sleepTime,similarityModelDatabase,clustersModelDatabase);
        }
        @Override
        public void saveResponse(String organizationId, String responseId, String methodName) throws InternalErrorException {
            String sql1 = "INSERT INTO responses(organizationId, responseId, actualPage, maxPages, finished, startTime, finalTime, methodName) VALUES (?,?,?,?,?,?,?,?)";

            getAccessToMainDb();
            try (Connection conn =  DriverManager.getConnection("jdbc:sqlite:../testing/integration/test_database/main.db");
                 PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1,organizationId);
                ps.setString(2,responseId);
                ps.setInt(3,0);
                ps.setInt(4,0);
                ps.setInt(5,0);
                ps.setLong(6, 123);
                ps.setLong(7, 123);
                ps.setString(8, methodName);
                ps.execute();
            } catch (SQLException sql) {
                throw new InternalErrorException(sql.getMessage());
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
                    Logger.getInstance().showErrorMessage("Here " + e.getMessage());
                }
            });
            thread1.start();
        }
    }
}
