package upc.similarity.semilarapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import semilar.data.Sentence;
import semilar.tools.preprocessing.SentencePreprocessor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Class use to represent requirements
public class Requirement implements Serializable {

    private static final SentencePreprocessor sentencePreprocessor = new SentencePreprocessor(
            SentencePreprocessor.TokenizerType.OPENNLP,
            SentencePreprocessor.TaggerType.OPENNLP_ENTROPY,
            SentencePreprocessor.StemmerType.WORDNET,
            SentencePreprocessor.ParserType.OPENNLP);

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="name")
    private String name;
    @JsonProperty(value="text")
    private String text;
    @JsonProperty(value="created_at")
    private Long created_at;

    private Sentence sentence_name;
    private Sentence sentence_text;

    public Requirement() {}

    public Requirement(String id, int name, int text, Sentence sentence_name, Sentence sentence_text, long created_at) {
        this.id = id;
        this.created_at = created_at;
        if (name == 0) {
            this.name = null;
            this.sentence_name = null;
        } else {
            this.name = sentence_name.getRawForm();
            this.sentence_name = sentence_name;
        }
        if (text == 0) {
            this.text = null;
            this.sentence_text = null;
        } else {
            this.text = sentence_text.getRawForm();
            this.sentence_text = sentence_text;
        }
    }

    class Info {
        public boolean completed;
    }

    public void compute_sentence() {
        //Some reqs take too long to preprocess and consume all the memory, to avoid this we set 100 seconds as maximum preprocess time for a requirement
        Object wait_timeout = new Object();
        Info info = new Info();
        info.completed = false;
        Thread t = new Thread(
                new Runnable() {
                    // run method
                    @Override
                    public void run() {
                        try {
                            if (name != null) sentence_name = sentencePreprocessor.preprocessSentence(check_lengh(name, 1));
                            if (text != null) sentence_text = sentencePreprocessor.preprocessSentence(check_lengh(text, 2));
                            synchronized (wait_timeout) {
                                info.completed = true;
                                wait_timeout.notify();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
        );
        t.start();
        try {
            synchronized(wait_timeout) {
                wait_timeout.wait(100000);
                if (!info.completed) {
                    t.stop();
                    while(t.isAlive()) {}
                    this.name = null;
                    this.text = null;
                    System.out.println("Requirement ignored. To much time spent.");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private String check_lengh(String req, int clean) {
        req = clean_text(req,clean);
        String result = "";
        String[] sentences = req.split("\\.");
        for (int i = 0; i < sentences.length; ++i) {
            if ((sentences[i].split("\\s+").length < 60) && (sentences[i].length() < 800)) result = result.concat(sentences[i] + ".");
            else System.out.println("A requirement part was ignored. With the id " + id + " was ignored the next text: " + sentences[i]);
        }
        return result;
    }

    private String clean_text(String text, int clean) {
        if (clean == 0) return text;
        else if (clean == 1) {

            String[] aux = text.split(" ");
            text = "";
            for (String word: aux) {
                if (word.equals(word.toUpperCase())) word = word.toLowerCase();
                text = text.concat(" " + word);
            }

            //text = text.replace("n't", "");

            text = text.replace("..", ".");
            text = text.replace(":", " ");
            text = text.replace("\"", "");
            text = text.replace("\\r", ".");
            text = text.replace("..", ".");
            text = text.replace("\\n", ".");
            text = text.replace("..", ".");
            text = text.replace("!", ".");
            text = text.replace("..", ".");
            text = text.replace("?", ".");
            text = text.replace("..", ".");
            text = text.replace("=", " ");
            text = text.replace(">", " ");
            text = text.replace("<", " ");
            text = text.replace("%", " ");
            text = text.replace("#", " ");
            text = text.replace(",", " ");
            text = text.replace("(", " ");
            text = text.replace(")", " ");
            text = text.replace("{", " ");
            text = text.replace("}", " ");
            text = text.replace("-", " ");

            String result = "";
            String[] r = text.split("(?=\\p{Upper})");
            for (String word: r) result = result.concat(" " + word);


            return result;
        }
        else {
            text = text.replaceAll("(\\{code.*?\\{code)","");

            String[] aux = text.split(" ");
            text = "";
            for (String word: aux) {
                if (word.equals(word.toUpperCase())) word = word.toLowerCase();
                text = text.concat(" " + word);
            }

            text = text.replace("..", ".");
            text = text.replace("\"", "");
            text = text.replace(":", " ");
            text = text.replace("\\r", ".");
            text = text.replace("..", ".");
            text = text.replace("\\n", ".");
            text = text.replace("..", ".");
            text = text.replace("!", ".");
            text = text.replace("..", ".");
            text = text.replace("?", ".");
            text = text.replace("..", ".");
            text = text.replace("=", " ");
            text = text.replace("%", " ");
            text = text.replace("#", " ");
            text = text.replace(",", " ");
            text = text.replace("(", " ");
            text = text.replace(")", " ");
            text = text.replace("{", " ");
            text = text.replace("}", " ");
            text = text.replace("-", " ");
            text = text.replace(">", " ");
            text = text.replace("<", " ");

            String result = "";
            String[] r = text.split("(?=\\p{Upper})");
            for (String word: r) result = result.concat(" " + word);

            text = "";
            for (String word: result.split(" ")) {
                if (word.length() < 20) text = text.concat(" " + word);
            }
            return result;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public Long getCreated_at() {
        return created_at;
    }

    public Sentence getSentence_name() {
        return sentence_name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Sentence getSentence_text() {
        return sentence_text;
    }

    public boolean isOK() {
        if (id == null) return false;
        else return true;
    }

    @Override
    public String toString() {
        return "Requirement with id: " + id + ".";
    }
}