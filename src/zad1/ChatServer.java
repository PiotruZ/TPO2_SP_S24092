/**
 * @author Szczęsny Piotr S24092
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer implements Runnable {
    private static final Map<String, SocketChannel> clients = Collections.synchronizedMap(new HashMap<>());
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private ServerSocketChannel serverSocketChannel;
    private final StringBuilder serverLog;
    private Selector selector;
    private int activeConnections = 0;
    private final Thread thread;

    public ChatServer(String host, int port) {
        try {
            serverSocketChannel = ServerSocketChannel.open();                                                           // opening channel
            serverSocketChannel.socket().bind(new InetSocketAddress(host, port));                                       // binding related address (localhost, port) to which clients will be connected
            serverSocketChannel.configureBlocking(false);                                                               // none-blocking channel

            selector = Selector.open();                                                                                 // Selector declaration
            serverSocketChannel.register(selector, serverSocketChannel.validOps());                                     // registering selector
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverLog = new StringBuilder();
        thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }

                    if (key.isReadable()) {
                        handleRead(key);
                    }

                    iterator.remove();
                }
            }
            handleShutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SelectableChannel selectableChannel = key.channel();
        SocketChannel socketChannel = (SocketChannel) selectableChannel;

        if (socketChannel.isOpen()) {
            String request = readRequest(socketChannel);

            if (request.contains("logged in")) {
                handleLogin(request, socketChannel);
            }

            if (request.contains("logged out")) {
                handleLogout(request, socketChannel);
            }

            if (!request.isEmpty()) {
                handleRequest(request);
            }
        }
    }

    private String readRequest(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        StringBuilder request = new StringBuilder();

        while (socketChannel.read(byteBuffer) > 0) {
            byteBuffer.flip();
            request.append(Charset.forName("ISO-8859-2").decode(byteBuffer));
            byteBuffer.clear();
        }

        return request.toString();
    }

    private void handleLogin(String request, SocketChannel socketChannel) {
        clients.put(request.substring(0, request.indexOf(":")), socketChannel);
    }


    private void handleLogout(String request, SocketChannel socketChannel) throws IOException {
        String ID = request.substring(0, request.indexOf(":"));
        clients.get(ID).write(Charset.forName("ISO-8859-2").encode(request));
        clients.remove(ID);
        activeConnections--;
    }

    private synchronized void handleRequest(String request) throws IOException {
        String messageToSend = request.replaceAll(": logged", " logged");
        String logMessage = dateFormat.format(new Date()) + " " + messageToSend;
        serverLog.append(logMessage);

        for (Map.Entry<String, SocketChannel> entry : clients.entrySet()) {
            entry.getValue().write(Charset.forName("ISO-8859-2").encode(messageToSend));
        }
    }


    private void handleShutdown() throws InterruptedException {
        synchronized(this) {
            activeConnections--;
            if (activeConnections == 0) {
                this.notify();  // notify stopServer() method
            }
        }
    }

    public void startServer() {
        thread.start();
        System.out.println("Server started" + "\n");
    }

    public void stopServer() throws InterruptedException {
        synchronized(this) {
            while (activeConnections > 0) {
                this.wait();
            }
        }
        thread.interrupt();
        System.out.println("Server stopped");
    }

    public String getServerLog() {
        return serverLog.toString();
    }
}
