package upc.similarity.semilarapi.dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import semilar.data.DependencyStructure;
import semilar.data.Sentence;
import semilar.data.Word;
import upc.similarity.semilarapi.entity.Requirement;

import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class SQLiteDAO implements RequirementDAO {

    private static Connection c;

    private void createNewTable() {
        // SQL statement for creating a new table
        /*
        id -> primary key
        name -> values in range (0,1) 0-> name null 1-> name not null
        text -> values in range (0,1) 0-> text null 1-> text not null

         */
        String sql = "CREATE TABLE IF NOT EXISTS prepocessed (\n"
                + "	id varchar PRIMARY KEY,\n"
                + " created_at long, \n"
                + " name integer, \n"
                + " text integer, \n"
                + "	sentence_name text,\n"
                + "	sentence_text text\n"
                + ");";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:../semilar.db");
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
    public void savePreprocessed(Requirement r) throws SQLException {
        c = DriverManager.getConnection("jdbc:sqlite:../semilar.db");


        PreparedStatement ps;
        ps = c.prepareStatement("DELETE FROM prepocessed WHERE id = ?");
        ps.setString(1, r.getId());
        ps.execute();

        ps = c.prepareStatement ("INSERT INTO prepocessed (id, created_at, name, text, sentence_name, sentence_text) VALUES (?, ?, ?, ?, ?, ?)");
        ps.setString(1, r.getId());
        if (!(r.getCreated_at() == null)) ps.setLong(2,r.getCreated_at());
        if (r.getName() == null) {
            ps.setInt(3,0);
            ps.setString(5,"");
        } else {
            ps.setInt(3,1);
            ps.setString(5,sentence2JSON(r.getSentence_name()).toString());
        }
        if (r.getText() == null) {
            ps.setInt(4,0);
            ps.setString(6,"");
        } else {
            ps.setInt(4,1);
            ps.setString(6, sentence2JSON(r.getSentence_text()).toString());
        }
        ps.execute();
        c.close();
    }

    @Override
    public Requirement getRequirement(String id_aux) throws SQLException {
        c = DriverManager.getConnection("jdbc:sqlite:../semilar.db");
        PreparedStatement ps;
        ps = c.prepareStatement("SELECT id, created_at, name, text, sentence_name, sentence_text FROM prepocessed WHERE id = ?");
        ps.setString(1, id_aux);
        ps.execute();
        ResultSet rs = ps.getResultSet();

        if (rs.next()) {
            String id = rs.getString("id");
            Long created_at = rs.getLong("created_at");
            int name = rs.getInt("name");
            int text = rs.getInt("text");
            Sentence sentence_name = null;
            Sentence sentence_text = null;
            if (name == 1) sentence_name = JSON2Sentence(rs.getString("sentence_name"));
            if (text == 1) sentence_text = JSON2Sentence(rs.getString("sentence_text"));
            Requirement result = new Requirement(id,name,text,sentence_name,sentence_text,created_at);
            c.close();
            return result;
        }
        else {
            c.close();
            throw new SQLException("Requirement with id " + id_aux + " does not exist in DB");
        }
    }

    @Override
    public void clearDB() throws SQLException {
        c = DriverManager.getConnection("jdbc:sqlite:../semilar.db");
        PreparedStatement ps;
        ps = c.prepareStatement("DELETE FROM prepocessed");
        ps.execute();
        c.close();
    }

    // Conversion Sentence <=> JSON

    private JSONObject sentence2JSON(Sentence sentence) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("rawForm", sentence.getRawForm());
            jsonObject.put("words", words2JSON(sentence.getWords()));
            jsonObject.put("dependencies", dependencies2JSON(sentence.getDependencies()));
            jsonObject.put("syntacticTreeString", sentence.getSyntacticTreeString());
            jsonObject.put("dependencyTreeString", sentence.getDependencyTreeString());
            jsonObject.put("collocations", new JSONObject(sentence.getCollocations()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  jsonObject;
    }

    private Sentence JSON2Sentence(String sentence) {

        
        Sentence s = new Sentence();
        
        try {
			JSONObject jsonObject = new JSONObject(sentence);

			s.setRawForm(jsonObject.getString("rawForm"));
			s.setWords(JSON2Words(jsonObject.getJSONArray("words")));
			s.setDependencies(JSON2Dependencies(jsonObject.getJSONArray("dependencies")));
			s.setSyntacticTreeString(jsonObject.getString("syntacticTreeString"));
//			s.setDependencyTreeString(jsonObject.getString("dependencyTreeString"));
			s.setCollocations(JSON2Collocations(jsonObject.getJSONObject("collocations")));
		} catch (JSONException e) {
			e.printStackTrace();
		}

        return  s;
    }

    private Hashtable<Integer,Integer> JSON2Collocations(JSONObject collocations) {

        Hashtable<Integer, Integer> c = new Hashtable<>();

        JSONArray jsonArray = collocations.names();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
					int key = jsonArray.getInt(i);
					int value = collocations.getInt("" + key);
					c.put(key, value);
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
        }

        return c;
    }

    private JSONArray words2JSON(ArrayList<Word> words) {

        JSONArray jsonArray = new JSONArray();
        for (Word w : words) jsonArray.put(word2JSON(w));
        return  jsonArray;
    }

    private ArrayList<Word> JSON2Words(JSONArray words) {

        ArrayList<Word> ws = new ArrayList<>();
        for (int i = 0; i < words.length(); i++)
			try {
				ws.add(JSON2Word(words.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
        return ws;
    }

    private JSONObject word2JSON(Word w) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("rawForm", w.getRawForm());
            jsonObject.put("baseForm", w.getBaseForm());
            jsonObject.put("pos", w.getPos());
            jsonObject.put("sentenceIndex", w.getSentenceIndex());
            jsonObject.put("isStopWord", w.isIsStopWord());
            jsonObject.put("isPunctuaton", w.isIsPunctuaton());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    private Word JSON2Word(JSONObject jsonObject) {

        Word word = new Word();

        try {
			word.getBaseForm();
			word.setRawForm(jsonObject.getString("rawForm"));
			word.setBaseForm(jsonObject.getString("baseForm"));
			word.setPos(jsonObject.getString("pos"));
			word.setSentenceIndex(jsonObject.getInt("sentenceIndex"));
			word.setIsStopWord(jsonObject.getBoolean("isStopWord"));
			word.setIsPunctuaton(jsonObject.getBoolean("isPunctuaton"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

        return word;
    }

    private JSONArray dependencies2JSON(ArrayList<DependencyStructure> dependencies) {

        JSONArray jsonArray = new JSONArray();
//        for (DependencyStructure d : dependencies) jsonArray.put(dependency2JSON(d));
        return  jsonArray;
    }

    private ArrayList<DependencyStructure> JSON2Dependencies(JSONArray dependencies) {

        ArrayList<DependencyStructure> ds = new ArrayList<>();
        for (int i = 0; i < dependencies.length(); i++)
			try {
				ds.add(JSON2Dependency(dependencies.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
        return ds;
    }

    private JSONObject dependency2JSON(DependencyStructure d) {

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", d.getType());
            jsonObject.put("head", d.getHead());
            jsonObject.put("modifier", d.getModifier());
            jsonObject.put("strHead", d.getStrHead());
            jsonObject.put("strModifier", d.getStrModifier());
            jsonObject.put("strPoshead", d.getStrPoshead());
            jsonObject.put("strPosModifier", d.getStrPosModifier());
            jsonObject.put("depthInTree", d.getDepthInTree());
            jsonObject.put("sentenceIndex", d.getSentenceIndex());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  jsonObject;
    }

    private DependencyStructure JSON2Dependency(JSONObject jsonObject) {

        DependencyStructure d = new DependencyStructure();

        try {
			d.setType(jsonObject.getString("type"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			d.setHead(jsonObject.getInt("head"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			d.setModifier(jsonObject.getInt("modifier"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
            d.setStrHead(jsonObject.getString("strHead"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
            d.setStrModifier(jsonObject.getString("strModifier"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
            d.setStrPoshead(jsonObject.getString("strPoshead"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
            d.setStrPosModifier(jsonObject.getString("strPosModifier"));
        } catch (JSONException e) {
            //e.printStackTrace();
        }
        try {
			d.setDepthInTree(jsonObject.getInt("depthInTree"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			d.setSentenceIndex(jsonObject.getInt("sentenceIndex"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

        return d;
    }
}
