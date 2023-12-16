package wordcorrector;

import edu.caltech.cs2.datastructures.IterableString;
import edu.caltech.cs2.datastructures.LinkedDeque;
import edu.caltech.cs2.datastructures.TrieMap;
import edu.caltech.cs2.interfaces.IDeque;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.function.Function;

public class AutoCompleteTrie extends TrieMap<Character, IterableString, AutoCompleteTrie.Word> {
    public static class Word {
        public final String word;
        public final int dictionaryIndex;

        public Word(String word, int dictionaryIndex) {
            this.word = word;
            this.dictionaryIndex = dictionaryIndex;
        }
    }

    /**
     * Initialize an AutoCompleteTrie and populate it using words from the dictFilename
     * @param collector - the function that collapses letters to keys
     * @param dictFilename - the filename containing the dictionary of words
     */
    public AutoCompleteTrie(Function<IDeque<Character>, IterableString> collector, String dictFilename) {
        super(collector);

        try (Scanner dict = new Scanner(new File(dictFilename))) {
            int counter = 0;
            while (dict.hasNext()) {
                String nextWord = dict.next();
                this.put(new IterableString(nextWord), new Word(nextWord, counter));
                counter++;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find dictionary for AutoCompleteTrie");
        }
    }

    /**
     * Retrieves keys that have the given key as a prefix
     */
    public IDeque<IterableString> autocomplete(String keyString) {
        IDeque<Word> completions = this.getCompletions(new IterableString(keyString));
        IDeque<IterableString> suggestions = new LinkedDeque<>();
        for (Word completion : completions) {
            suggestions.add(new IterableString(completion.word));
        }
        return suggestions;
    }
}
