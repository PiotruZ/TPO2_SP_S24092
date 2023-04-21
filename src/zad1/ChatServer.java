/**
 * @author Szczęsny Piotr S24092
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ChatServer {

    private String host;
    private int port;
    private Selector selector;
    private Map<SocketChannel, String> userNames;
    private ByteBuffer buffer;
    private Thread serverThread;
    private volatile boolean isStopped;
    private final CountDownLatch serverStartedLatch = new CountDownLatch(1);

    public ChatServer(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        selector = Selector.open();
        this.userNames = new HashMap<>();
        this.buffer = ByteBuffer.allocate(1024);
        this.isStopped = false;
    }

    public void startServer() {
        serverThread = new Thread(() -> {
            try {
                selector = Selector.open();
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);
                serverChannel.bind(new InetSocketAddress(host, port));
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println("Server started");
                serverStartedLatch.countDown();

                while (!isStopped) {
                    int readyChannels = selector.select();
                    if (readyChannels == 0) {
                        continue;
                    }
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        } else if (key.isReadable()) {
                            readFromClient(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        try {
            serverStartedLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("New client connected: " + socketChannel.getRemoteAddress());
    }

    private void readFromClient(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        buffer.clear();
        int numBytes = socketChannel.read(buffer);
        if (numBytes == -1) {
            disconnectClient(socketChannel);
            return;
        }
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String message = new String(bytes);
        if (!userNames.containsKey(socketChannel)) {
            String userName = message.trim();
            userNames.put(socketChannel, userName);
            broadcastMessage("Server", userName + " joined the chat.");
        } else {
            String userName = userNames.get(socketChannel);
            broadcastMessage(userName, message.trim());
        }
    }

    private void disconnectClient(SocketChannel socketChannel) throws IOException {
        String userName = userNames.get(socketChannel);
        socketChannel.close();
        userNames.remove(socketChannel);
        broadcastMessage("Server", userName + " left the chat.");
        System.out.println("Client disconnected: " + socketChannel.getRemoteAddress());
    }

    private void broadcastMessage(String sender, String message) throws IOException {
        for (SocketChannel channel : userNames.keySet()) {
            if (channel.isOpen()) {
                String user = userNames.get(channel);
                String msg = "[" + sender + "]: " + message;
                ByteBuffer msgBuffer = ByteBuffer.wrap(msg.getBytes());
                channel.write(msgBuffer);
            }
        }
    }

    public void stopServer() {
        isStopped = true;
        try {
            serverThread.join();
            if (selector != null) {
                selector.close();
            }
            System.out.println("Server closed");
        } catch (IOException e) {
            System.err.println("Error occurred while closing selector: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getServerLog() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<SocketChannel, String> entry : userNames.entrySet()) {
            sb.append("Client: ").append(entry.getValue())
                    .append(", address: ").append(entry.getKey().getRemoteAddress())
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }
}
