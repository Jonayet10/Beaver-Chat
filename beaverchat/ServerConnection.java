package beaverchat;

import java.io.*;
import java.net.Socket;

public class ServerConnection extends Thread {
    private final static String SERVER = "beaverchat.countablethoughts.com";
    private final int PORT = 9001;
    private final static String SERVER_PWD = "vX3lnvQYym3ptOyh640iFggco2iNt1kGw";
    private final static String CHAT_CHANNEL = "#main";

    private Chat chat;

    private final BufferedWriter out;
    private final BufferedReader in;

    private final Socket socket;

    public ServerConnection(Chat chat) throws IOException {
        this.chat = chat;

        // connect to IRC server
        this.socket = new Socket(this.SERVER, this.PORT);
        this.out = new BufferedWriter(
                new OutputStreamWriter(this.socket.getOutputStream()));
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    public boolean go() throws IOException {
        // Log on to the server.
        write("PASS", this.SERVER_PWD);
        write("NICK", chat.getUsername());
        write("USER", chat.getUsername(), "-", "-", "-");

        // Read lines from the server until it tells us we have connected.
        String line = null;
        while ((line = this.in.readLine()) != null) {
            int code = Integer.parseInt(line.split(" ")[1]);
            switch (code) {
                case IRCCodes.RplMyInfo:
                    write("JOIN", this.CHAT_CHANNEL);
                    start();
                    return true;
                case IRCCodes.ErrNickNameInUse:
                    this.socket.close();
                    return false;
            }
        }
        return false;
    }

    public String getAccountName() {
        return chat.getUsername();
    }

    public void send(String cmd, String text) {
        try {
            cmd = cmd.trim();
            switch (cmd) {
                case "MAIN":
                    m_channel(this.CHAT_CHANNEL, text);
                    break;
                default:
                    String[] parts = text.split(" ", 2);
                    cmd = parts[0].trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(String... args) throws IOException {
        StringBuilder str = new StringBuilder();

        int i = 0;
        while (i < args.length - 1) {
            str.append(args[i] + " ");
            i++;
        }
        if (args.length > 0) {
            str.append(args[i]);
        }
        this.out.write(str.toString());
        this.out.write("\r\n");
        this.out.flush();
    }

    public void m_channel(String channel, String msg) throws IOException {
        write("PRIVMSG", channel, ":" + msg);
    }

    @Override
    public void run() {
        try {
            String line = null;
            // Keep reading lines from the server.
            while ((line = this.in.readLine()) != null) {
                if (line.startsWith("PING")) {
                    write("PONG", line.substring(5));
                }
                else if (line.startsWith(":beaverchat")) {
                    int code = Integer.parseInt(line.split(" ")[1]);
                    switch (code) {
                        // List of users...
                        case IRCCodes.RplNamReply:
                            String[] names = line.split(":")[2].split(" ");
                            chat.addUsers(names);
                            break;
                    }
                }
                else {
                    String cmd = line.split(" ")[1];
                    switch (cmd) {
                        case "PART":
                            chat.removeUser(line.split(":")[1].split("!")[0]);
                            break;
                        case "JOIN":
                            chat.addUser(line.split(":")[1].split("!")[0]);
                            break;
                        case "PRIVMSG":
                            if (!line.split(" ")[2].equals(chat.getUsername())) {
                                return;
                            }
                            String[] lineParts = line.split(":");
                            chat.gotMessage(lineParts[1].split("!")[0], lineParts[2]);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
