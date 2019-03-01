package upc.similarity.semilarapi.dao;

import upc.similarity.semilarapi.entity.Requirement;

import java.sql.SQLException;
import java.util.List;

public interface RequirementDAO {

    public void savePreprocessed(Requirement r) throws SQLException;

    public Requirement getRequirement(String id) throws SQLException;

    void clearDB() throws SQLException;
}
