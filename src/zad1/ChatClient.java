/**
 *
 *  @author Szczęsny Piotr S24092
 *
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class ChatClient extends Thread {
    private SocketChannel channel;
    private final StringBuilder clientChatView;
    private final String clientId;

    public ChatClient(String host, int port, String clientId) {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));                                      // opening channel, connecting to the address
            channel.configureBlocking(false);                                                                     // making it none-blocking
        } catch (IOException ignored) { }
        this.clientChatView = new StringBuilder("=== " + clientId + " chat view" + "\n");
        this.clientId = clientId;
    }

    public void login() {
        this.start();
        this.send("logged in");
    }

    public void logout() throws InterruptedException {
        this.send("logged out");
        Thread.sleep(100);
        this.interrupt();
    }

    public void send(String req) {
        ByteBuffer byteBuffer = Charset.forName("ISO-8859-2").encode(clientId + ": " + req + "STOP");                      // encoding req with encoding, because of Polish tokens
        try {
            Thread.sleep(50);
            channel.write(byteBuffer);                                                                            // sending req
        } catch (IOException exception) {
            send(req);
        } catch (InterruptedException ignored) { }
    }

    public String getClientChatView() {
        return clientChatView.toString().replace("STOP", "\n");
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {                                                                                  // reading while Thread is not interrupted
                ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                StringBuilder request = new StringBuilder();

                while (channel.read(byteBuffer) > 0) {
                    byteBuffer.flip();                                                                                  // making buffer ready for a new sequence
                    request.append(Charset.forName("ISO-8859-2").decode(byteBuffer));                                   // reading what is sent to that client
                    byteBuffer.clear();                                                                                 // cleaning buffer after receiving sequence
                }

                if (!request.toString().isEmpty())
                    clientChatView.append(request);                                                                           // appending ChatView with received sequence
            }
        } catch (IOException ignored) { }
    }
}





