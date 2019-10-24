package upc.similarity.compareapi.preprocess;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreprocessPipelineDefault implements PreprocessPipeline {

    @Override
    public Map<String, List<String>> preprocessRequirements(boolean compare, List<Requirement> requirements) throws InternalErrorException {
        Map<String, List<String>> result = new HashMap<>();
        try {
            for (Requirement requirement : requirements) {
                String id = requirement.getId();
                String info = extractRequirementInfo(compare, requirement);
                List<String> tokens = englishAnalyze(info);
                result.put(id,tokens);
            }
        } catch (IOException e) {
            throw new InternalErrorException("Error while loading preprocess pipeline");
        }
        return result;
    }


    /*
    Private methods
     */

    private String extractRequirementInfo(boolean compare, Requirement requirement) {
        String result = "";
        if (requirement.getName() != null) result = result.concat(cleanText(requirement.getName()) + ". ");
        if (compare && (requirement.getText() != null)) result = result.concat(cleanText(requirement.getText()));
        return result;
    }

    private String cleanText(String text) {
        text = text.replaceAll("(\\{.*?})", " code ");
        text = text.replaceAll("[.$,;\\\"/:|!?=%,()><_0-9\\-\\[\\]{}']", " ");
        String[] aux2 = text.split(" ");
        String result = "";
        for (String a : aux2) {
            if (a.length() > 1) {
                result = result.concat(" " + a);
            }
        }
        return result;
    }
    private List<String> englishAnalyze(String text) throws IOException {
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("porterstem")
                .addTokenFilter("commongrams")
                .build();
        return analyze(text, analyzer);
    }

    private List<String> analyze(String text, Analyzer analyzer) throws IOException {
        List<String> result = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        return result;
    }

}
