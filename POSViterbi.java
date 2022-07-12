import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class POSViterbi {

    HashMap<String, HashMap<String, Double>> transitions;
    HashMap<String, HashMap<String, Double>> observations;

    public POSViterbi() {
        transitions = new HashMap<>();
        observations = new HashMap<>();
    }

    public void trainFile(String textFilename, String tagFilename) throws IOException {
        BufferedReader textFile = new BufferedReader(new FileReader(textFilename));
        BufferedReader tagFile = new BufferedReader(new FileReader(tagFilename));

        HashMap<String, Integer> totalTag = new HashMap<>();
        HashMap<String, Integer> totalWord = new HashMap<>();
        String textLine = textFile.readLine();
        String tagLine = tagFile.readLine();
        while (textLine != null && tagLine != null) {

            String[] words = textLine.split(" ");
            String[] tags = tagLine.split(" ");
            String backTag = "#";
            for (int i = 0; i < words.length; i++) {
                String word = words[i].toLowerCase();
                String tag = tags[i].toLowerCase();
                if (!transitions.containsKey(backTag)) {
                    transitions.put(tag, new HashMap<>());
                    totalTag.put(backTag, 0);
                }
                HashMap<String, Double> subTransMap = transitions.get(backTag);
                if (!subTransMap.containsKey(tag)) {
                    subTransMap.put(tag, 0.0);
                }
                subTransMap.put(tag, subTransMap.get(tag)+1);
                totalTag.put(tag, totalTag.get(tag)+1);
                transitions.put(backTag, subTransMap);

                if (!observations.containsKey(tag)) {
                    observations.put(tag, new HashMap<>());
                    totalWord.put(tag, 0);
                }
                HashMap<String, Double> subObsMap = observations.get(backTag);
                if (!subObsMap.containsKey(tag)) {
                    subObsMap.put(tag, 0.0);
                }
                subObsMap.put(tag, subObsMap.get(tag)+1);
                totalTag.put(tag, totalTag.get(tag)+1);
                transitions.put(backTag, subObsMap);
                backTag = tag;
            }

            textLine = tagFile.readLine();
            tagLine = tagFile.readLine();
        }
    }

}
