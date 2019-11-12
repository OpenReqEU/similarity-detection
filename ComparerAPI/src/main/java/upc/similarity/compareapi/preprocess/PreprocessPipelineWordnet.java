package upc.similarity.compareapi.preprocess;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import opennlp.tools.stemmer.PorterStemmer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.preprocess.adapter_tagger.AdapterTagger;
import upc.similarity.compareapi.preprocess.adapter_tagger.OpenNLPTagger;
import upc.similarity.compareapi.util.Logger;

import javax.naming.ldap.Control;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

public class PreprocessPipelineWordnet implements PreprocessPipeline {

    //http://wordnetcode.princeton.edu/wn3.1.dict.tar.gz
    private static final String dictionaryPath = "../dict";
    private IDictionary dictionary;
    private AdapterTagger adapterTagger;
    private PorterStemmer porterStemmer;
    private Map<String,Character> posTags;

    public PreprocessPipelineWordnet() {
        try {
            URL url = new URL("file", null, dictionaryPath);
            dictionary = new Dictionary(url);
            dictionary.open();
            adapterTagger = new OpenNLPTagger();
            porterStemmer = new PorterStemmer();
            loadInformation("../opennlp_tags_to_pos");
        } catch (IOException | InternalErrorException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            //throw new InternalErrorException("Error while loading the dictionary");
        }
    }

    @Override
    public Map<String, List<String>> preprocessRequirements(boolean compare, List<Requirement> requirements) throws InternalErrorException {
        Map<String, List<String>> result = new HashMap<>();
        try {
            for (Requirement requirement : requirements) {
                String id = requirement.getId();
                String info = extractRequirementInfo(compare, requirement);
                List<String> tokens = englishAnalyze(info);
                List<String> tokensWordnet = extractHypernyms(tokens);
                List<String> tokensStem = porterStem(tokensWordnet); //test with one good stemmer, that uses pos tags
                result.put(id,tokensStem);
            }
        } catch (IOException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error while loading preprocess pipeline");
        }
        return result;
    }


    /*
    Private methods
     */

    private List<String> porterStem(List<String> tokens) {
        List<String> result = new ArrayList<>();
        for (String token: tokens) {
            String stem = porterStemmer.stem(token);
            if (stem != null) result.add(stem);
            else result.add(token);
        }
        return result;
    }

    private String getHypernym(String token, String tag) {
        Character charTag = posTags.get(tag);
        if (charTag != null) {
            POS pos = POS.getPartOfSpeech(charTag);
            if (pos != null) {
                IIndexWord idxWord = dictionary.getIndexWord(token, pos);
                if (idxWord != null) {
                    List<IWordID> wordsIDs = idxWord.getWordIDs();
                    if (wordsIDs.size() > 0) {
                        IWordID wordID = wordsIDs.get(0); // 1st meaning
                        IWord word = dictionary.getWord(wordID);
                        ISynset synset = word.getSynset();

                        List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
                        if (hypernyms.size() > 0) {
                            List<IWord> words;
                            for (ISynsetID sid : hypernyms) {
                                words = dictionary.getSynset(sid).getWords();
                                Iterator<IWord> i = words.iterator();
                                if (i.hasNext()) return i.next().getLemma(); // 1st hypernym
                                else return null;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<String> extractHypernyms(List<String> tokens) {
        String[] aux = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); ++i) aux[i] = tokens.get(i);
        String[] postTags = adapterTagger.posTagger(aux);
        List<String> result = new ArrayList<>();
        int i = 0;
        for (String token: tokens) {
            String hypernym = getHypernym(token,postTags[i]);
            if (hypernym != null) result.add(hypernym);
            else result.add(token);
            ++i;
        }
        return result;
    }

    private void loadInformation(String path) throws InternalErrorException {
        posTags = new HashMap<>();
        try(FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split("->");
                if (parts.length != 2) throw new InternalErrorException("Error loading tags data. Syntax error around '->' symbol");
                String tag = parts[0].replaceAll(" ", "");
                String description = "";
                String[] auxDescription = parts[1].split(" ");
                boolean firstWord = true;
                for (String word: auxDescription) {
                    if (!word.equals("")) {
                        if (!firstWord) description = description.concat(" ");
                        firstWord = false;
                        description = description.concat(word);
                    }
                }
                posTags.put(tag,description.charAt(0));
            }
        } catch (IOException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error loading tags data. File error.");
        }
    }

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
                //.addTokenFilter("porterstem")
                //.addTokenFilter("commongrams")
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
