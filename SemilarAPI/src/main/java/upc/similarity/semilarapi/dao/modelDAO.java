package upc.similarity.semilarapi.dao;

import upc.similarity.semilarapi.entity.Model;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;

import java.sql.SQLException;
import java.util.List;

public interface modelDAO {

    public void saveModel(String organization, Model model) throws SQLException;

    public Model getModel(String organization) throws SQLException, BadRequestException;

    public void clearDB(String organization) throws SQLException;
}
