package upc.similarity.semilarapi.dao;

import upc.similarity.semilarapi.entity.Model;
import upc.similarity.semilarapi.entity.Requirement;

import java.sql.SQLException;
import java.util.List;

public interface modelDAO {

    public void saveModel(String organization, Model model) throws SQLException;

    public Model getModel(String organization) throws SQLException;

    public void clearDB() throws SQLException;
}
