package wordsuggestor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import edu.caltech.cs2.datastructures.ArrayDeque;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.caltech.cs2.interfaces.IQueue;

public final class ParseFBMessages {
    private ParseFBMessages() {
        /* should not be instantiated */ }

    // INSTRUCTIONS:
    //
    // <Your FB Name> may be either:
    //  1) Your name on Messenger (e.g. "Danny Allen")
    //  2) Your username on facebook, which can be found by looking at the URL on your profile
    // It's typically 1), but for whatever reason Facebook sometimes labels them
    // with 2) (sorry!). You can check which one your messages are labeled with by
    // opening up one of the message files and taking a look.
    //
    // <Your FB Archive> is the directory on your computer where the archive is stored.
    // (e.g. "/Users/Me/Downloads/MyArchiveName" or "C:\Users\Me\Downloads\MyArchiveName")
    // You may be able to use a relative path like "./MyArchiveName", but results can
    // vary from machine to machine.
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("USAGE: ParseFBMessages <Your FB Name> <Your FB Archive>");
            System.exit(1);
        }

        // Note: you can replace these with your FB Name and Archive instead of
        // using the command line if you'd like.
        String name = args[0];
        String archive = args[1];

        IQueue<String> messages = new ArrayDeque<String>();
        File[] listOfFiles = new File(archive + File.separator + "messages").listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isFile()) {
              Document doc = Jsoup
                      .parse(listOfFiles[i], "UTF-8");
              Elements messagesElements = doc.getElementsByTag("p");
              for (Element content : messagesElements) {
                  if (content.previousElementSibling().getElementsByClass("user").text()
                          .equals(name)) {
                      messages.enqueue(content.text());
                  }
              }
          }
        }

        PrintWriter out = new PrintWriter("data/messages.txt", "UTF-8");

        Iterator<String> iter = messages.iterator();
        while (iter.hasNext()) {
            out.println(iter.next());
        }

        out.close();
    }
}
