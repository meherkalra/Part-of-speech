/**
 * Part of Speech Predictor
 * @author Meher Kalra and Neo Cai
 * Dartmouth CS10, Spring 2022
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class POS {

    private static ArrayList<String[]> training;
    private static ArrayList<String[]> tag;
    private static HashMap<String, HashMap<String, Double>> transitionMap;
    private static HashMap<String, HashMap<String, Double>> observationMap;

    /**
     * Reading training data
     *
     * @param trainingTextFilePath
     * @param trainingTagsFilePath
     * @throws Exception
     */
    public void loadTrainingData(String trainingTextFilePath, String trainingTagsFilePath) throws Exception {
        try {
            training = new ArrayList<>();
            BufferedReader btrain = new BufferedReader(new FileReader(trainingTextFilePath));
            String line;

            while ((line = btrain.readLine()) != null) {
                line = "# " + line.toLowerCase();
                String[] word = line.split(" ");
                training.add(word);
            }
            btrain.close();

            tag = new ArrayList<>();
            BufferedReader bTag = new BufferedReader(new FileReader(trainingTagsFilePath));
            String tags;

            while ((tags = bTag.readLine()) != null) {
                tags = "# " + tags;
                String[] linetags = tags.split(" ");
                tag.add(linetags);
            }
            bTag.close();
        } catch (Exception e) {
            System.out.println("Error: Cannot read file.");
        }
    }

    /**
     * Training to create the transition and observation maps
     */
    public void OTMaps() {
        try {
            transitionMap = new HashMap<>();
            observationMap = new HashMap<>();
            if (!training.isEmpty() && training.size() == tag.size()) {
                // observations
                for (int i = 1; i < training.size(); i++) {
                    for (int j = 1; j < training.get(i).length; j++) {
                        String currentW = training.get(i)[j]; // current word
                        String currentT = tag.get(i)[j];      // current tags

                        // If map doesn't have tage, add tag with word, score map
                        if (!observationMap.containsKey(currentT)) {
                            HashMap<String, Double> innerMap = new HashMap<String, Double>();
                            innerMap.put(currentW, 0.0);
                            observationMap.put(currentT, innerMap);
                        }

                        // If map has tag but doesn't have word, add word
                        if (!observationMap.get(currentT).containsKey(currentW)) {
                            observationMap.get(currentT).put(currentW, 0.0);
                        }
                        // Increment current value by 1
                        observationMap.get(currentT).put(currentW, observationMap.get(currentT).get(currentW) + 1);
                    }
                }

                for (HashMap<String, Double> hash : observationMap.values()) {
                    double total = 0;
                    for (String state : hash.keySet()) {
                        total += hash.get(state);
                    }
                    for (String state : hash.keySet()) {
                        hash.put(state, Math.log(hash.get(state) / total));
                    }
                }
                // transitions
                for (int i = 0; i < training.size(); i++) {
                    for (int j = 0; j < training.get(i).length - 1; j++) {
                        String currentT = tag.get(i)[j]; // current tag
                        String nextT = tag.get(i)[j + 1];      // next tag

                        // If doesn't have current tag, put it in with an empty hashmap
                        if (!transitionMap.containsKey(currentT)) {
                            HashMap<String, Double> innerMap = new HashMap<>();
                            innerMap.put(nextT, 0.0);
                            transitionMap.put(currentT, innerMap);
                        }
                        // If has current tag but doesn't have next tag, add with score
                        if (!transitionMap.get(currentT).containsKey(nextT)) {
                            transitionMap.get(currentT).put(nextT, 0.0);
                        }
                        // Get the current tag, put the next tag (key) with value of the transition score from the current Tag
                        transitionMap.get(currentT).put(nextT, transitionMap.get(currentT).get(nextT) + 1);
                    }
                }
                // Loop through the values of the transition map
                for (HashMap<String, Double> hash : transitionMap.values()) {
                    double total = 0.0;

                    // Go through map in transition map and get the state
                    for (String state : hash.keySet()) {
                        // Total = state value + total
                        total += hash.get(state);
                    }

                    // Logarithmize the scores
                    for (String state : hash.keySet()) {
                        hash.put(state, Math.log(hash.get(state) / total));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error: Training data is not valid.");
        }
    }

    /**
     * Viterbi algorithm, given a string input determines the most likely path of states
     * @param input
     * @return
     */
    public ArrayList<String> viterbi(String input) {
        // Initialize BackTrace list to contain a map with current and previous sentence
        ArrayList<HashMap<String, String>> backTrace = new ArrayList<>();

        // Initialize the currentState and current score
        HashMap<String, Double> currScores = new HashMap<String, Double>();

        // Initialize the next Scores and next state
        HashMap<String, Double> nextScores;

        // Initial start and value as 0.0
        currScores.put("#", 0.0);

        // Convert input to list
        String[] words = input.toLowerCase().split(" ");

        // Loop through the list
        for (int i = 0; i < words.length; i++) {
            nextScores = new HashMap<>();

            HashMap<String, String> backTraceMap = new HashMap<>();

            for (String currState : currScores.keySet()) {
                if (transitionMap.containsKey(currState)) {
                    for (String nextState : transitionMap.get(currState).keySet()) {

                        double currScore = currScores.get(currState);
                        double transitionScore = transitionMap.get(currState).get(nextState);
                        double observationScore;

                        if (observationMap.get(nextState).containsKey(words[i])) {
                            observationScore = observationMap.get(nextState).get(words[i]);
                        } else observationScore = -100;

                        double nextScore = currScore + transitionScore + observationScore;

                        if (!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)) {
                            nextScores.put(nextState, nextScore);
                            backTraceMap.put(nextState, currState);
                            backTrace.add(i, backTraceMap);
                        }
                    }
                }
            }
            currScores = nextScores;
            // find max value at the end and backtrack from that max value
        }
        // Do backtrace
        double maxScore = Double.NEGATIVE_INFINITY;
        String maxState = "";

        // loop through curr scores
        for (String currState : currScores.keySet()) {

            // find highest score
            if (currScores.get(currState) > maxScore) {
                maxScore = currScores.get(currState);
                // get highest state
                maxState = currState;
            }
        }

        // loop through back trace array list backwards
        ArrayList<String> predictedStates = new ArrayList<String>();
        for (int i = words.length - 1; i >= 1; i--) {
            predictedStates.add(0, maxState);
            maxState = backTrace.get(i).get(maxState);
        }


        // return that list
        return predictedStates;
    }

    /**
     * Console testing
     * @throws Exception
     */
    public void consoleTest() throws Exception {
        Scanner input = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("Enter a line: ");
                String line = "# " + input.nextLine().toLowerCase();
                if (line.equals("# q")) {
                    System.out.println("quit");
                    break;
                }
                ArrayList<String> predictionArray = viterbi(line);
                String prediction = "";
                for (String state : predictionArray) {
                    prediction = prediction + state + " ";
                }
                System.out.println("Prediction: " + prediction);

            }
        } catch (Exception e) {
            System.out.println("Error: Input is invalid");
        }
    }

    /**
     * Testing with a large file
     * @param testSentencesFile
     * @param testTagsFile
     * @throws IOException
     */
    public void fileTest(String testSentencesFile, String testTagsFile) throws Exception {
        BufferedReader btest = new BufferedReader(new FileReader(testSentencesFile));
        BufferedReader btesttags = new BufferedReader(new FileReader(testTagsFile));

        String line = btest.readLine();
        String tagLine = btesttags.readLine();

        double correct = 0;
        int total = 0;

        try {
            while (line != null && tagLine != null) {
                line = "# " + line.toLowerCase();
                ArrayList<String> predictionArray = viterbi(line);
                String[] tags = tagLine.split(" ");

                for (int i = 0; i < predictionArray.size(); i++) {

                    if (tags[i].equals(predictionArray.get(i))) {
                        correct += 1;
                    }
                    total += 1;
                }
                line = btest.readLine();
                tagLine = btesttags.readLine();
            }
        } catch (IOException e){
            System.out.println("Error: Files could not be read.");
        }
        System.out.println("Correct: " + correct);
        System.out.println("Total: " + total);

        System.out.println((correct / total) * 100 + "% correct, " + (100 - (correct / total) * 100) + "% incorrect");
        btest.close();
        btesttags.close();
    }

    /**
     * Hard coded graph test
     */
    public void graphTest() {
        transitionMap = new HashMap<>();
        observationMap = new HashMap<>();

        String sentence = "# dog watch dog watch chase";
        transitionMap.put("#", new HashMap<>());
        transitionMap.put("NP", new HashMap<>());
        transitionMap.put("V", new HashMap<>());
        transitionMap.put("N", new HashMap<>());

        transitionMap.get("#").put("N", 0.0);
        transitionMap.get("N").put("V", 0.0);
        transitionMap.get("V").put("N", -0.301);
        transitionMap.get("V").put("NP", -0.301);
        transitionMap.get("NP").put("N", 0.0);

        observationMap.put("NP", new HashMap<>());
        observationMap.put("V", new HashMap<>());
        observationMap.put("N", new HashMap<>());

        observationMap.get("NP").put("chase", 0.0);
        observationMap.get("V").put("watch", 0.0);
        observationMap.get("N").put("dog", 0.0);

        System.out.println("Sentence: " + sentence);
        System.out.println("Transition Map: " + transitionMap);
        System.out.println("Observation Map: " + observationMap);

        System.out.println("Prediction:  " + viterbi(sentence));
    }

    /**
     * Main for all the tests
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String BrownTrainingSentences = "PS5/brown-train-sentences.txt";
        String BrownTrainingTags = "PS5/brown-train-tags.txt";
        String SimpleTrainingSentences = "PS5/simple-train-sentences.txt";
        String SimpleTrainingTags = "PS5/simple-train-tags.txt";

        POS manualTest = new POS();

        manualTest.graphTest();
        System.out.println(" ");

        POS simpleTest = new POS();
        simpleTest.loadTrainingData(SimpleTrainingSentences, SimpleTrainingTags);
        simpleTest.OTMaps();
        System.out.println(" ");
        System.out.println("File test Simple:");
        simpleTest.fileTest("PS5/simple-test-sentences.txt", "PS5/simple-test-tags.txt");
        System.out.println(" ");

        POS autoTest = new POS();
        autoTest.loadTrainingData(BrownTrainingSentences, BrownTrainingTags);
        autoTest.OTMaps();
        System.out.println("File test Brown:");
        autoTest.fileTest("PS5/brown-test-sentences.txt", "PS5/brown-test-tags.txt"  );
        System.out.println(" ");
        System.out.println("Console test:");
        autoTest.consoleTest();
    }
}
// handle the error where the tag file is different from teh text file