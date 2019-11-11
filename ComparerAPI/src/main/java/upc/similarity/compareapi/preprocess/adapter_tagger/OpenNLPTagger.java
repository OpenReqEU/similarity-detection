package upc.similarity.compareapi.preprocess.adapter_tagger;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.util.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenNLPTagger implements AdapterTagger {

    public OpenNLPTagger() throws InternalErrorException {
        posTags = new ArrayList<>();
        sentenceTags = new ArrayList<>();
        hashDescriptions = new HashMap<>();
        loadInformation("../opennlp_tags");
        loadModels();
    }

    private List<String> posTags;
    private List<String> sentenceTags;
    private HashMap<String,String> hashDescriptions;
    private Tokenizer tokenizer;
    private POSTaggerME tagger;
    private ChunkerME chunker;

    private void loadInformation(String path) throws InternalErrorException {
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
                if (tag.contains("<")) sentenceTags.add(tag);
                else if (tag.contains("(")) posTags.add(tag);
                else throw new InternalErrorException("Error loading tags data. The next tag is not well written: " + tag);
                hashDescriptions.put(tag,description);
            }

        } catch (IOException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error loading tags data. File error.");
        }
    }

    private void loadModels() throws InternalErrorException {
        tokenizer = WhitespaceTokenizer.INSTANCE;

        POSModel model1;
        try (InputStream modelIn = new FileInputStream("en-pos-maxent.bin")) {
            model1 = new POSModel(modelIn);
        } catch (IOException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error loading POSTAG model.");
        }
        tagger = new POSTaggerME(model1);

        /*ChunkerModel model2;
        try {
            try (InputStream modelIn = new FileInputStream("en-chunker.bin")) {
                model2 = new ChunkerModel(modelIn);
            }
        } catch (IOException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error loading Chunker model.");
        }
        chunker = new ChunkerME(model2);*/
    }

    @Override
    public List<String> getPosTags() {
        return posTags;
    }

    @Override
    public List<String> getSentenceTags() {
        return sentenceTags;
    }


    @Override
    public String[] tokenizer(String sentence) {
        return tokenizer.tokenize(sentence);
    }


    @Override
    public String[] posTagger(String[] tokens) {
        String[] tags = tagger.tag(tokens);
        for (int i = 0; i < tags.length; ++i) {
            tags[i] = "("+tags[i].toLowerCase()+")";
        }

        return tags;
    }

    @Override
    public String[] chunker(String[] tokens, String[] tokensTagged) {
        String[] chunks = chunker.chunk(tokens,tokensTagged);
        for (int i = 0; i < chunks.length; ++i) {
            chunks[i] = chunks[i].toLowerCase();
            if (chunks[i].contains("vp")) chunks[i] = "<vp>";
            else chunks[i] = "<np>";
        }
        return chunks;
    }

    @Override
    public String getTagDescription(String tag) {
        return hashDescriptions.get(tag);
    }
}
