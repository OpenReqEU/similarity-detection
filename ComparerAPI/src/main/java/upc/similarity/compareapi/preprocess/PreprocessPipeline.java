package upc.similarity.compareapi.preprocess;

import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.util.List;
import java.util.Map;

public interface PreprocessPipeline {

    /**
     * @param compare whether to use the text attribute or not
     * @param requirements the requirements to preprocess, all of them must have an id attribute
     * @return A map with each requirement id as key and the preprocessed tokens as value
     */
    Map<String, List<String>> preprocessRequirements(boolean compare, List<Requirement> requirements) throws InternalErrorException;
}
