/**
 *
 *  @author Szczęsny Piotr S24092
 *
 */

package zad1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private String host;
    private int port;
    private String id;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private StringBuilder chatView;

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
        this.chatView = new StringBuilder();
    }
    public void login() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(id);
            Thread receiverThread = new Thread(new Receiver());
            receiverThread.start();
            chatView.append("Connected to server: " + host + ":" + port + "\n");
            chatView.append(id + " logged in" + "\n");
            System.out.println(chatView);
        } catch (IOException e) {
            chatView.append("Error occurred while connecting to server: " + e.getMessage() + "\n");
        }
    }

    public void logout() {
        try {
            if (socket != null && socket.isConnected()) {
                out.println("/logout");
                socket.close();
                chatView.append(id+" logged out\n");;
                System.out.println(chatView);
            }
        } catch (IOException e) {
            chatView.append("Error occurred while logging out: " + e.getMessage() + "\n");
        }
    }

    public void send(String message) {
        try {
            out.println(message);
            chatView.append(message+"\n");
            System.out.println(chatView);
        } catch (Exception e) {
            chatView.append("Error occurred while sending message: " + e.getMessage() + "\n");
        }
    }

    public String getChatView() {
        System.out.println("=== "+id+" chat view");
        return chatView.toString();
    }

    private class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    synchronized(chatView) {
                        chatView.append(line + "\n");
                    }
                }
            } catch (IOException e) {
                chatView.append("Error occurred while receiving message: " + e.getMessage() + "\n");
            }
        }
    }
}


