package upc.similarity.compareapi.integration.unit;

import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.preprocess.PreprocessPipelineWordnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DriverHypernyms {

    public static void main(String[] args) {
        try {
            PreprocessPipelineWordnet preprocessPipelineWordnet = new PreprocessPipelineWordnet();
            //String meh = preprocessPipelineWordnet.getHypernym("hello","(nn)");
            //System.out.println(meh);
            Requirement req1 = new Requirement();
            req1.setId("UPC-1");
            req1.setText("Thank you for your comments. The updated list of events, publications and videos is in the Google Drive (https://drive.google.com/file/d/19oTLc1k32HeBE_TteFzpQ9xzR6tcm6SN/view?usp=sharing)");
            List<Requirement> aux = new ArrayList<>();
            aux.add(req1);
            Map<String, List<String>> meh = preprocessPipelineWordnet.preprocessRequirements(true,aux);
            System.out.println("yes");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
