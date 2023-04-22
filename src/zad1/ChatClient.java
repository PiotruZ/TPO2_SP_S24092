/**
 *
 *  @author Szczęsny Piotr S24092
 *
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class ChatClient implements Runnable {
    private final SocketChannel channel;
    private final StringBuilder chatView;
    private final String id;

    public ChatClient(String host, int port, String id) throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(false);
        this.chatView = new StringBuilder("=== " + id + " chat view" + "\n");
        this.id = id;
    }

    public void login() {
        new Thread(this).start();
        this.send("logged in");
    }

    public void logout() throws InterruptedException, IOException {
        String message = "logged out";
        this.send(message);
        chatView.append(id).append(": ").append(message).append("\n");
        channel.close();
    }


    public void send(String req) {
        ByteBuffer byteBuffer = Charset.forName("ISO-8859-2").encode(id + ": " + req + "\n");
        try {
            Thread.sleep(50);
            channel.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getChatView() {
        return chatView.toString();
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);

            while (!Thread.currentThread().isInterrupted() && channel.isOpen() && channel.isConnected()) {
                selector.select();

                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    if (key.isReadable()) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                        StringBuilder request = new StringBuilder();

                        int bytesRead = channel.read(byteBuffer);
                        if (bytesRead == -1) {
                            channel.close();
                            break;
                        }

                        if (bytesRead > 0) {
                            byteBuffer.flip();
                            request.append(Charset.forName("ISO-8859-2").decode(byteBuffer));
                        }

                        if (!request.toString().isEmpty()) {
                            chatView.append(request);
                        }
                    }
                    selectedKeys.remove();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
