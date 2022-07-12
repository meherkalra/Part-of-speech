import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class markov {

    Map<String, Map<String, Double>> transitionMatrix; // matrix that keeps track of how often certain POS tags transition to others
    Map<String, Map<String, Double>> observationMatrix; // matrix that keeps track of what words from the training file can be each tag and how often they appear as that tag
    Double unseenPenalty = -100.0; // score for the state of a word when it has not been observed as that POS tag

    public markov() { // constructor
        transitionMatrix = new HashMap<>(); // initialize transitionMatrix
        observationMatrix = new HashMap<>(); // initialize observationMatrix
    }

    public void train(String textFile, String tagFile) throws IOException { // writes transitionMatrix and observationMatrix to function as a POS tagger
        BufferedReader textFileR = new BufferedReader(new FileReader(textFile)); // input file with sentences
        BufferedReader tagsFileR = new BufferedReader(new FileReader(tagFile)); // input file with corresponding POS tags
        String lineText = textFileR.readLine(); // create string of first line of sentences file
        String lineTag = tagsFileR.readLine(); // create string of first line of tags file
        Map<String, Integer> tagTotals = new HashMap<>(); // map of tags
        Map<String, Integer> wordTotals = new HashMap<>(); // map of words
        while (lineText != null && lineTag != null){ // while both lines are not null
            String[] splitText = lineText.split(" "); // split sentence line into list of words
            String[] splitTag = lineTag.split(" "); // split tag line into list of tags
            String prevTag = "#"; // set previous tag equal to # to represent start
            for (int i = 0; i < splitTag.length; i++) { // for i from 0 to less than length of list of words
                String word = splitText[i]; // word equals first word from word list
                word = word.toLowerCase(); // make word lowercase
                if (word.equals(".")) continue;
                String tag = splitTag[i]; // tag equals first tag from tag list
                updateMatrix(tagTotals, tag, prevTag, transitionMatrix); // update transition matrix to include tag
                updateMatrix(wordTotals, word, tag, observationMatrix); // update observation matrix to include word
                prevTag = tag; // update previous tag to be current tag
            }
            lineText = textFileR.readLine(); // read next line of sentences file
            lineTag = tagsFileR.readLine(); // read next line of tags file

        }
        logProbality(tagTotals, transitionMatrix); // convert transitionMatrix to use log probabilities
        logProbality(wordTotals, observationMatrix); // convert observationMatrix to use log probabilities
    }

    private void updateMatrix(Map<String, Integer> wordTotals, String word, String tag, Map<String, Map<String, Double>> observationMatrix) { // add new word with tag to matrix
        if (!observationMatrix.containsKey(tag)) { // if matrix does not contain given tag as key
            observationMatrix.put(tag, new HashMap<>()); // put tag as key and new map as value into matrix
            wordTotals.put(tag, 0); // put tag and 0 into wordTotals map
        }
        Map<String, Double> obsMap = observationMatrix.get(tag); // get map associated with tag from observationMatrix
        if (!obsMap.containsKey(word)) { // if retrieved map does not contain word as key
            obsMap.put(word, 0.0); // put word as key and 0 as value
        }
        obsMap.put(word, obsMap.get(word)+1); // increment value associated with word key by 1
        observationMatrix.put(tag, obsMap); // put tag as key and obsMap as value into matrix
        wordTotals.put(tag, wordTotals.get(tag)+1); // increment value associated with tag key by 1
    }

    public void logProbality(Map<String, Integer> totals, Map<String, Map<String, Double>> matrix) { // set probabilities of matrices to log probabilities
        for (String tag: totals.keySet()) { // for tag in keys of totals map
            Map<String, Double> subMatrix = matrix.get(tag); // get inner map from matrix that is value of key tag
            subMatrix.replaceAll((t, v) -> Math.log(subMatrix.get(t) / totals.get(tag))); // replace all values of inner map with log probability of those values
        }
    }

    public void testFileAccuracy(String textFile, String tagFile) throws IOException { // check how accurate viterbi algorithm is with text file and file with corresponding tags
        // number of correct and incorrect tags
        int correct = 0;
        int wrong = 0;

        // input files and read first lines
        BufferedReader textFileR = new BufferedReader(new FileReader(textFile));
        BufferedReader tagFileR = new BufferedReader(new FileReader(tagFile));
        String lineText = textFileR.readLine();
        String lineTag = tagFileR.readLine();

        // while loop that goes through files and increments number of correct and wrong tags
        while (lineText != null && lineTag != null) {

            String[] words = lineText.split(" ");
            String[] testTags = lineTag.split(" ");
            ArrayList<String> predTags = predict(words);
            for (int i = 0; i < words.length-1; i++) { // loops through all the words

                if (predTags.get(i).equals(testTags[i])) {
                    correct += 1; // if tag equals tag from tag file increment correct
                } else { // else increment wrong
                    wrong += 1;
                }
            }
            correct += 1; // account for period

            // read next lines of files
            lineText = textFileR.readLine();
            lineTag = tagFileR.readLine();
        }
        System.out.println(correct + " correct vs " + wrong + " wrong"); // print number of correct tags and number of wrong tags
    }

    public ArrayList<String> predict(String[] words) { // takes a list of words and returns list of tags associated with the words

        ArrayList<Map<String, String>> backPath = new ArrayList<>(); // create backpath to keep track of paths to current states
        Map<String, Double> toCheck = new HashMap<>(); // new map of tags to check as keys and the current scores as values
        toCheck.put("#", 0.0); // add start to toCheck

        for (int i = 0; i < words.length; i++) { // this loop goes through the words
            if (words[i].equals(".")) continue;
            Map<String, Double> nextCheck = new HashMap<>();
            String word = words[i];
            word = word.toLowerCase();
            HashMap<String, String> back = new HashMap<>();
            for (String checkTag: toCheck.keySet()) { // this loop goes through the tags in toCheck

                if (!transitionMatrix.containsKey(checkTag)) continue; // if transition matrix does not contain tag
                for (String nextTag: transitionMatrix.get(checkTag).keySet()) { // go through tags that can be next tag from transition matrix

                    double score = transitionMatrix.get(checkTag).get(nextTag) + toCheck.get(checkTag); // score is transition score of next tag plus current score
                    if (!observationMatrix.get(nextTag).containsKey(word)) { // if word has not been observed then observed score is unseenPenalty
                        score += unseenPenalty;
                    } else { // else score equals observed score of word plus score
                        score += observationMatrix.get(nextTag).get(word);
                    }
                    if (!nextCheck.containsKey(nextTag) || score > nextCheck.get(nextTag)) { // if tag isn't already in nextCheck or score is greater than current score of that tag
                        back.put(nextTag, checkTag); // put next tag with current tag in back map
                        nextCheck.put(nextTag, score); // update nextCheck
                    }
                }
            }
            backPath.add(back); // add back map to backPath
            toCheck = nextCheck; // update toCheck


        }
        // finds highest score once we reach the last word
        double maxScore = 0;
        String maxTag = "";
        for (String tag: toCheck.keySet()) {
            if (maxScore == 0  || toCheck.get(tag) > maxScore) {
                maxScore = toCheck.get(tag);
                maxTag = tag;
            }
        }

        // uses backpath to create list of tags associated with given list of words
        ArrayList<String> path = new ArrayList<>();
        path.add(maxTag);
        for (int i = backPath.size() - 1; i > 0; i--) {

            Map<String, String> back = backPath.get(i);
            path.add(0, back.get(maxTag));
            maxTag = back.get(maxTag);
        }
        // for (String word: words) System.out.print(word + " ");
        return path; // return path of tags
    }

    public void predictConsole() { // allows someone to write words into console and receive their POS predictions
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter text for predictions");
        String line = scanner.nextLine();
        String[] words = line.split(" ");
        System.out.println("Predictions:");
        System.out.println(predict(words));
    }

    public static void main(String[] args) throws Exception { // main method to train and test class
        // training files
        String trainTextFile = "texts/brown-train-sentences.txt";
        String trainTagFile = "texts/brown-train-tags.txt";

        // testing files
        String testTextFile = "texts/brown-test-sentences.txt";
        String testTagFile = "texts/brown-test-tags.txt";

        // create object and train and test it
        markov pos = new markov();
        pos.train(trainTextFile, trainTagFile);
        pos.testFileAccuracy(testTextFile, testTagFile);
        pos.predictConsole();
    }
}