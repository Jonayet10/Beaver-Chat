package beaverchat;

import edu.caltech.cs2.datastructures.*;
import edu.caltech.cs2.interfaces.IDeque;
import edu.caltech.cs2.interfaces.IDictionary;
import edu.caltech.cs2.types.Item;
import edu.caltech.cs2.types.NGram;
import wordcorrector.SpellingCorrector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public class Chat {

    private NGramMap[] suggestors;
    private SpellingCorrector corrector;
    private String username;
    private Set<String> otherUsers;
    private boolean loggedIn;
    private Map<String, List<Item<String, String>>> messageMap;
    private ServerConnection remote;

    public Chat() throws FileNotFoundException {
        this.loggedIn = false;
        this.otherUsers = new TreeSet<>();
        corrector = new SpellingCorrector("data/dictionary.txt");
        suggestors = new NGramMap[3];
        suggestors[0] = initNGramMap(new Scanner(new File("data/alice")), 1);
        suggestors[1] = initNGramMap(new Scanner(new File("data/alice")), 2);
        suggestors[2] = initNGramMap(new Scanner(new File("data/alice")), 3);
        this.messageMap = new HashMap<>();
    }

    public String autocomplete(String prefix) {
        return corrector.autocomplete(prefix);
    }

    public String getSuggestions(String context, int numSuggestions) {
        String[] suggestions = suggestors[context.split(" ").length - 1].getWordsAfter(
                new NGram(context), numSuggestions);
        String suggestionString = "[";
        for (String suggestion : suggestions) {
            suggestionString += suggestion + ", ";
        }
        suggestionString = suggestionString.substring(0, suggestionString.length() - 2);
        suggestionString += "]";
        return suggestionString;
    }

    public String getBestCorrection(String context, String misspelledWord) {
        NGramMap suggestor = null;
        String result = null;
        if (!context.equals("")) {
            suggestor = suggestors[context.split(" ").length - 1];
        }

        if (corrector.isMisspelled(misspelledWord)) {
            result = corrector.getBestCorrection(suggestor, context, misspelledWord, 5);
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    public Set<String> getOtherUsers() {
        return otherUsers;
    }

    public void addUsers(String[] users) {
        for (String user : users) {
            addUser(user);
        }
    }

    public void addUser(String user) {
        if (!user.equals(this.username)) {
            otherUsers.add(user);
        }
    }

    public void removeUser(String user) {
        otherUsers.remove(user);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void logIn() {
        this.loggedIn = true;
    }

    public void gotMessage(String from, String msg) {
        if (!messageMap.containsKey(from)) {
            messageMap.put(from, new ArrayList<>());
        }
        messageMap.get(from).add(new Item<>(from, msg));
    }

    public void setServerConnection(ServerConnection remote) {
        this.remote = remote;
    }

    public List<Item<String, String>> getMessages(String otherUser) {
        return messageMap.get(otherUser);
    }

    public void sendMessage(String otherUser, String msg) {
        if (!messageMap.containsKey(otherUser)) {
            messageMap.put(otherUser, new ArrayList<>());
        }
        messageMap.get(otherUser).add(new Item<>(this.username, msg));
        try {
            this.remote.m_channel(otherUser, msg);
        } catch (IOException e) {
            System.err.println("Failed to send message " + msg + " to " + otherUser);
        }
    }

    private NGramMap initNGramMap(Scanner in, int N) {
        Function<IDeque<String>, NGram> ngramCollector = (IDeque<String> x) -> new NGram(x);
        Function<IDeque<Character>, IterableString> stringCollector = (IDeque<Character> x) -> {
            char[] chars = new char[x.size()];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = x.peekFront();
                x.addBack(x.removeFront());
            }
            return new IterableString(new String(chars));
        };
        IDictionary<NGram, IDictionary<IterableString, Integer>> newOuter = new ChainingHashDictionary<>(MoveToFrontDictionary::new);
        Supplier<IDictionary<IterableString, Integer>> newInner = () -> new ChainingHashDictionary<>(MoveToFrontDictionary::new);
        return new NGramMap(in, N, newOuter, newInner);
    }
}
