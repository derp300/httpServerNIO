package httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private Selector selector;
    private Map<SocketChannel, byte[]> responseData;
    private InetSocketAddress address;
    ExecutorService threadService = Executors.newCachedThreadPool();

    public HttpServer(int port) throws IOException {
        address = new InetSocketAddress(port);
        responseData = new ConcurrentHashMap<>();
    }

    public void startServer() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(address);
            serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                this.selector.selectNow();
                Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();

                    keys.remove();

                    if (!key.isValid()) {
                        System.out.println("Invalid key");
                        continue;
                    }

                    if (key.isAcceptable()) {
                        this.accept(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) {
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);
            buffer.clear();

            if (bytesRead == -1) {
                clientChannel.close();
                key.cancel();
                return;
            }
            
            
            String request = new String(Arrays.copyOfRange(buffer.array(), 0, bytesRead));
            try {
                threadService.submit(new SocketThread(clientChannel, key, responseData, request));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(SelectionKey key) throws IOException{
        SocketChannel clientChannel = (SocketChannel) key.channel();
        byte[] data = responseData.get(clientChannel);
        responseData.remove(clientChannel);
        clientChannel.write(ByteBuffer.wrap(data));
        clientChannel.close();
        key.cancel();
    }
}
