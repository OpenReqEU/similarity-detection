package upc.similarity.semilarapi.dao;

import upc.similarity.semilarapi.entity.Model;
import upc.similarity.semilarapi.exception.BadRequestException;

import java.sql.*;

public class SQLiteDAO implements modelDAO {

    private static Connection c;
    private static String db_url = "jdbc:sqlite:../models.db";

    private void createDatabase() {

        String sql = "CREATE TABLE IF NOT EXISTS models (\n"
                + "	organization varchar PRIMARY KEY,\n"
                + " model text, \n"
                + " corpusF text \n"
                + ");";

        try (Connection conn = DriverManager.getConnection(db_url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public SQLiteDAO() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    @Override
    public void saveModel(String organization, Model model) throws SQLException {
        PreparedStatement ps = null;
        try {
            c = DriverManager.getConnection(db_url);
            ps = c.prepareStatement("DELETE FROM models WHERE organization = ?");
            ps.setString(1, organization);
            ps.execute();

            ps = c.prepareStatement("INSERT INTO models (organization, model, corpusF) VALUES (?, ?, ?)");
            ps.setString(1, organization);
            ps.setString(2, model.model_toJSON());
            ps.setString(3, model.corpusFrequency_toJSON());

            ps.execute();
        } finally {
            c.close();
            //if (ps != null) ps.close();
        }
    }

    @Override
    public Model getModel(String organization) throws SQLException, BadRequestException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = DriverManager.getConnection(db_url);
            ps = c.prepareStatement("SELECT model, corpusF FROM models WHERE organization = ?");
            ps.setString(1,organization);
            ps.execute();
            rs = ps.getResultSet();

            if (rs.next()) {
                String model = rs.getString("model");
                String corpusF = rs.getString("corpusF");
                return new Model(model,corpusF);
            } else {
                throw new BadRequestException("The organization " + organization + " does not have any model in the database");
            }
        } finally {
            c.close();
            //if (ps != null) ps.close();
            //if (rs != null) rs.close();
        }
    }



    @Override
    public void clearDB(String organization) throws SQLException {
        PreparedStatement ps = null;
        try {
            c = DriverManager.getConnection(db_url);
            ps = c.prepareStatement("DELETE FROM models WHERE organization = ?");
            ps.setString(1, organization);
            ps.execute();
        } finally {
            c.close();
            if (ps != null) ps.close();
        }
    }
}
