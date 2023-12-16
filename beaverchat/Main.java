package beaverchat;

import java.io.*;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.caltech.cs2.types.Item;

import java.awt.Desktop;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

  private static String chatPage = "src/beaverchat/html/chat.html";
  private static String loginPage = "src/beaverchat/html/login.html";
  private static HttpServer server;
  private static ServerConnection remote;
  public static Chat chat;

  public static void main(String[] args) throws Exception {
    // create a chat instance
    chat = new Chat();

    // create server and all paths
    server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/", new LocalFile(loginPage));
    server.createContext("/js/chat.js", new LocalFile("src/beaverchat/js/chat.js"));
    server.createContext("/css/chat.css", new LocalFile("src/beaverchat/css/chat.css"));
    server.createContext("/js/login.js", new LocalFile("src/beaverchat/js/login.js"));
    server.createContext("/css/login.css", new LocalFile("src/beaverchat/css/login.css"));
    server.createContext("/messages", new GetMessages());
    server.createContext("/suggestions", new GetSuggestions());
    server.createContext("/corrections", new GetCorrections());
    server.createContext("/autocomplete", new AutoComplete());
    server.createContext("/login", new LoginHandler());
    server.createContext("/sendMessage", new MsgSendHandler());
    server.createContext("/getContacts", new GetContactsHandler());
    server.createContext("/getMessages", new GetMessagesHandler());
    server.setExecutor(null); // creates a default executor
    server.start();

    if (Desktop.isDesktopSupported()) {
      Desktop.getDesktop().browse(new URI("http://localhost:8000/"));
    }
  }

  static class GetMessagesHandler implements  HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String otherUser = t.getRequestURI().getQuery();
      String response = "";
      for (Item<String, String> elem : chat.getMessages(otherUser)) {
        response += elem.key.strip() + "&" + elem.value.strip() + ":";
      }
      response = response.substring(0, response.length() - 1);
      t.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class GetContactsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      Set<String> otherUsers = chat.getOtherUsers();
      String response = "";
      for (String otherUser : otherUsers) {
        response += otherUser + ",";
      }
      response = response.substring(0, response.length() - 1);
      t.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class MsgSendHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      BufferedReader msgReader = new BufferedReader(new InputStreamReader(t.getRequestBody()));
      int b;
      StringBuilder buf = new StringBuilder(512);
      while ((b = msgReader.read()) != -1) {
        buf.append((char) b);
      }
      msgReader.close();
      String body = buf.toString();
      int splitIdx = body.indexOf('&');
      String otherUser = body.substring(0, splitIdx);
      String message = body.substring(splitIdx + 1);
      chat.sendMessage(otherUser, message);
      t.sendResponseHeaders(200, 0);
    }
  }

  static class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String username = t.getRequestURI().getQuery();

      // ordering here is important: the remote expects the chat to have a username
      // so that must be done first
      chat.setUsername(username);
      remote = new ServerConnection(chat);
      chat.setServerConnection(remote);
      String response = "";
      int code = 200;
      if (!remote.go()) {
        response = "Username in use! Pick a different username.";
        code = 400;
      }
      else {
        chat.logIn();
        server.removeContext("/");
        server.createContext("/", new LocalFile(chatPage));
      }
      t.sendResponseHeaders(code, response.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class AutoComplete implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String prefix = t.getRequestURI().getQuery();
      String response = chat.autocomplete(prefix);
      if (response == null) {
        response = "";
      }
      t.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class GetSuggestions implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String[] query = t.getRequestURI().getQuery().split("&");
      String context = query[0];
      Integer numSuggestions = Integer.parseInt(query[1]);

      String suggestionString = chat.getSuggestions(context, numSuggestions);

      t.sendResponseHeaders(200, suggestionString.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(suggestionString.getBytes());
      os.close();
    }

  }

  static class GetCorrections implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String[] query = t.getRequestURI().getQuery().split("&");

      String misspelledWord = query[1].trim();
      String context = query[0].trim();

      String bestCorrection = chat.getBestCorrection(context, misspelledWord);

      t.sendResponseHeaders(200, bestCorrection.length());
      OutputStream os = t.getResponseBody();
      os.write(bestCorrection.getBytes());
      os.close();
    }
  }

  static class GetMessages implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      System.out.println(t.getRequestURI());
      String response = "{}";
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class LocalFile implements HttpHandler {
    private String name;

    public LocalFile(String name) {
      this.name = name;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
      String response = new String(Files.readAllBytes(Paths.get(this.name)), StandardCharsets.UTF_8);
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

}
