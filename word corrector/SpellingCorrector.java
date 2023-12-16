package wordcorrector;

import edu.caltech.cs2.datastructures.ArrayDeque;
import edu.caltech.cs2.datastructures.ChainingHashDictionary;
import edu.caltech.cs2.datastructures.IterableString;
import edu.caltech.cs2.datastructures.MoveToFrontDictionary;
import edu.caltech.cs2.datastructures.NGramMap;
import edu.caltech.cs2.interfaces.ICollection;
import edu.caltech.cs2.interfaces.IDeque;
import edu.caltech.cs2.interfaces.IDictionary;
import edu.caltech.cs2.misc.CorrectionChoice;
import edu.caltech.cs2.types.NGram;

public class SpellingCorrector {
    private static final int MAX_EDITS = 2;
    private final AutoCompleteTrie dictionary;
    private static int[][] memotable;

    public SpellingCorrector(String dictFilename) {
        this.dictionary = new AutoCompleteTrie( (IDeque<Character> x) -> {
            char[] chars = new char[x.size()];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = x.peekFront();
                x.addBack(x.removeFront());
            }
            return new IterableString(new String(chars));
        }, dictFilename);
    }

    /**
     * Uses the dictionary to autocomplete keyString if it can find one guaranteed completion
     * @param keyString - the prefix to be completed
     * @return the autocompleted string if exactly one is found
     */
    public String autocomplete(String keyString) {
        IDeque<IterableString> possibilities = this.dictionary.autocomplete(keyString);
        if (possibilities.size() == 1) {
            return possibilities.removeFront().toString();
        }
        return null;
    }

    /**
     * Returns true if the given word is misspelled (i.e. it's not in the dictionary)
     * @param str - the potentially misspelled word
     * @return A boolean indicating the correctness of the spelling
     */
    public boolean isMisspelled(String str) {
        if (this.dictionary.containsKey(new IterableString(str))) {
            return false;
        }
        return true;
    }

    /**
     * Get all possible valid corrections for segment
     * @param segment - the string you want to get corrections for
     * @return A collection of correction choices for the given segment
     */
    public ICollection<CorrectionChoice> getPossibleCorrections(String segment) {
        // recursively obtain all possible corrections
        IDictionary<String, IDictionary<String, CorrectionChoice>> correctionMap =
                new ChainingHashDictionary<String, IDictionary<String, CorrectionChoice>>(MoveToFrontDictionary::new);
        getPossibleCorrections(segment, 1, correctionMap);

        // now collapse the possible corrections into one list of corrections using a hashset
        // additionally, we must ensure that our words can be found in the dictionary
        IDictionary<String, CorrectionChoice> allCorrections = new ChainingHashDictionary<String, CorrectionChoice>(MoveToFrontDictionary::new);
        for (IDictionary<String, CorrectionChoice> l : correctionMap.values()) {
            for (CorrectionChoice c : l.values()) {
                AutoCompleteTrie.Word dictionaryWord = dictionary.get(new IterableString(c.word));
                if (dictionaryWord != null) {
                    int dictPosition = dictionaryWord.dictionaryIndex;
                    if (allCorrections.containsKey(c.word)) {
                        CorrectionChoice existing = allCorrections.get(c.word);
                        if (existing.editDistance > c.editDistance) {
                            allCorrections.put(c.word, new CorrectionChoice(c.word, c.editDistance, dictPosition));
                        }
                    }
                    else {
                        allCorrections.put(c.word, new CorrectionChoice(c.word, c.editDistance, dictPosition));
                    }
                }
            }
        }
        return allCorrections.values();
    }

    /**
     * Recursively obtain all possible words that are within MAX_EDITS of segment.
     *
     * @param numEdits - the number of edits to consider
     * @param segment - the word fragment we wish to autocorrect
     * @param possibleCorrections - A dictionary that maps a string to a deque of correction choices that are an edit
     *                            of segment
     */
    private void getPossibleCorrections(String segment, int numEdits,
                                        IDictionary<String, IDictionary<String, CorrectionChoice>> possibleCorrections)
    {
        // if we've already maxed out our number of edits, or if we've already got corrections for this segment, return
        if (numEdits > MAX_EDITS || possibleCorrections.containsKey(segment)) {
            return;
        }

        // create the list of possible corrections for this segment
        possibleCorrections.put(segment, new ChainingHashDictionary<String, CorrectionChoice>(MoveToFrontDictionary::new));

        // get possible edits for the segment
        IDeque<String> edits = getPossibleEdits(segment);
        for (String edit : edits) {
            if (!possibleCorrections.get(segment).containsKey(edit)) {
                possibleCorrections.get(segment).put(edit, new CorrectionChoice(edit, numEdits, -1));
            }
            // call recursively to get next round of edits
            getPossibleCorrections(edit, numEdits + 1, possibleCorrections);
        }
    }

    /**
     * Get all possible strings that are within 1 edit away from segment. An edit is a transposition, deletion,
     * replacement, or insertion.
     *
     * @param segment - the string to find all possible edits for
     * @return An array deque of strings containing all possible edits
     */
    private IDeque<String> getPossibleEdits(String segment) {
        IDeque<String> edited = new ArrayDeque<>();
        for (int i = 0; i < segment.length(); i++) {
            String left = segment.substring(0, i);
            String right = segment.substring(i);

            // transposition
            if (right.length() >= 2) {
                edited.add(left + right.charAt(1) + right.charAt(0) + right.substring(2));
            }

            // deletion
            if (right.length() >= 1) {
                edited.add(left + right.substring(1));
            }

            IterableString alphabet = new IterableString("abcdefghijklmnopqrstuvwxyz");
            // replaces
            if (right.length() >= 1) {
                for (char letter : alphabet) {
                    edited.add(left + letter + right.substring(1));
                }
            }

            // insertions
            for (char letter : alphabet) {
                edited.add(left + letter + right);
            }
        }
        return edited;
    }

    /**
     * Computes the edit distance between strings a and b
     * @param a
     * @param b
     * @return the edit distance
     */
    public static int editDistance(String a, String b) {
        memotable = new int[a.length() + 1][b.length() + 1];
        for(int i = 0; i < a.length() + 1; i++){
            for(int j = 0; j < b.length() + 1; j++){
                memotable[i][j] = -1;
            }
        }
        return editDistanceHelper(a.length(),b.length(), a, b);
    }

    private static int mindis(int i, int j, int k){
        if(i < Math.min(j, k)){
            return i;
        }
        else if(j < Math.min(i, k)){
            return j;
        }
        else if(k < Math.min(i, j)){
            return k;
        }
        System.out.println("0 case");
        return 0;
    }

    private static int editDistanceHelper(int i, int j, String a, String b){
        if (memotable[i][j] != -1) {
            return memotable[i][j];
        }
        if (i == 0 && j == 0) {
            memotable[i][j] = 0;
            return 0;
        }
        if(i > 0 && j == 0){
            memotable[i][j] = i;
            return i;
        }
        if(j > 0 && i == 0){
            memotable[i][j] = j;
            return j;
        }

        if(b.charAt(j-1) == a.charAt(i - 1)){
            memotable[i][j] = editDistanceHelper(i - 1, j - 1, a, b);
            return memotable[i][j];
        }

        int ed1 = editDistanceHelper(i - 1, j, a, b) + 1;
        int ed2 = editDistanceHelper(i, j - 1, a, b) + 1;
        int ed3 = editDistanceHelper(i - 1, j -1 , a, b) + 1;
////
        int min = Math.min(Math.min(ed1 ,ed2), ed3);
        if(b.charAt(j - 1) != a.charAt(i - 1)){
            if(memotable[i][j] == min){
                return memotable[i][j];
            }
            else if(memotable[i][j] == -1){
                memotable[i][j] = min;
                return memotable[i][j];
            }
        }
        return -1;
    }
//
    /**
     * Determines the best possible spelling correction
     * @param ngrams the map of ngrams to use when getting suggested words
     * @param text - the context (the n words preceding the misspelled word)
     * @param segment - the misspelled word
     * @param numConsidered - the number of options to consider when retrieving suggestions
     * @return the best spelling correction
     */
    public String getBestCorrection(NGramMap ngrams, String text, String segment, int numConsidered) {
        // Throw error if the text given isn't a proper ngram for the given ngrams map

        CorrectionChoice best = null;
        if (ngrams != null) {
            // get suggestions that are at most MAX_EDITS away
            String[] suggestions = ngrams.getWordsAfter(new NGram(text), numConsidered);
            for (String s : suggestions) {
                int distance = editDistance(s, segment);
                if (distance <= MAX_EDITS) {
                    if (best == null || distance < best.editDistance) {
                        int dictionaryPos = dictionary.get(new IterableString(s)).dictionaryIndex;
                        best = new CorrectionChoice(s, distance, dictionaryPos);
                    }
                }
            }
        }

        // If the number of suggestions that are at most MAX_EDITS away is 0, then we look at all possible corrections
        // and select the one with the closest edit distance
        if (best == null) {
            ICollection<CorrectionChoice> corrections = getPossibleCorrections(segment);
            for (CorrectionChoice correction : corrections) {
                if (best == null) {
                    best = correction;
                }
                else {
                    if (correction.editDistance < best.editDistance) {
                        best = correction;
                    }
                    else if (correction.editDistance == best.editDistance) {
                        if (correction.dictionaryPosition < best.dictionaryPosition) {
                            best = correction;
                        }
                    }
                }
            }
        }
        if (best == null) {
            return null;
        }
        return best.word;
    }
}
