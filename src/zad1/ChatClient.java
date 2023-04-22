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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatClient implements Runnable {
    private final SocketChannel channel;
    private final StringBuilder chatView;
    private final String id;
    private final Lock lock;

    public ChatClient(String host, int port, String id) throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(false);
        this.chatView = new StringBuilder("=== " + id + " chat view" + "\n");
        this.id = id;
        this.lock = new ReentrantLock();
    }

    public void login() {
        new Thread(this).start();
        this.send("logged in");
    }

    public void logout() throws InterruptedException, IOException {
        String message = "logged out";
        this.send(message);
        lock.lock();
        try {
            chatView.append(id).append(": ").append(message).append("\n");
        } finally {
            lock.unlock();
        }
        Thread.sleep(50);
        channel.close();
    }

    public void send(String req) {
        try {
            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_WRITE);

            ByteBuffer byteBuffer = Charset.forName("ISO-8859-2").encode(id + ": " + req + "\n");

            while (byteBuffer.hasRemaining()) {
                selector.select();

                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    if (key.isWritable()) {
                        lock.lock();
                        try {
                            channel.write(byteBuffer);
                        } finally {
                            lock.unlock();
                        }
                    }
                    selectedKeys.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getChatView() {
        lock.lock();
        try {
            return chatView.toString();
        } finally {
            lock.unlock();
        }
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
                            lock.lock();
                            try {
                                chatView.append(request);
                            } finally {
                                lock.unlock();
                            }
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

