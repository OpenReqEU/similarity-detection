package upc.similarity.compareapi.preprocess.adapter_tagger;

import java.util.List;

public interface AdapterTagger {

    List<String> getPosTags();

    List<String> getSentenceTags();

    String[] tokenizer(String sentence);

    String[] posTagger(String[] tokens);

    String[] chunker(String[] tokens, String[] tokensTagged);

    String getTagDescription(String tag);
}
