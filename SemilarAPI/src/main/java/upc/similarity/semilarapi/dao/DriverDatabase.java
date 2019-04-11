package upc.similarity.semilarapi.dao;

public class DriverDatabase {

    public static void main(String[] args) {
        try {
            SQLiteDAO db = new SQLiteDAO();
            db.createDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
