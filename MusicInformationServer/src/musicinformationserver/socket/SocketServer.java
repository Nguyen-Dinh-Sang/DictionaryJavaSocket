package musicinformationserver.socket;

import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
/*
    - socket server được viết trên một Thread khác để tách biệt với main và tối ưu hơn.
    - class này quản lý server socket.
    - khi một client tạo kết nối tới server, server sẻ tạo ra một Thread socket để kết nối và làm việc với client đó.
 */

public class SocketServer implements Runnable{
    private ServerSocket server;

    //danh sách cách luồng đang kết nối và làm việc với client
    private List<ServerThread> clients = new ArrayList<>();
    private Thread worker;
    private AtomicBoolean running = new AtomicBoolean(false);

    public SocketServer(int port) {
        openSocket(port);
    }

    private void openSocket(int port) {
        try {
            server = new ServerSocket(port);
            System.err.println("Server started");
        } catch (IOException ex) {
        }
    }

    public void closeServer() {
        running.set(false);
        try {
            System.out.println("Closing Server");

            for (ServerThread client : clients) {
                client.closeServerThread();
            }

            server.close();
            System.err.println("Server Closed");
        } catch (IOException ex) {
        }
    }

    public void startServer() {
        worker = new Thread(this);
        worker.start();
    }

    @Override
    public void run() {
        running.set(true);
        int clientCount = 1;
        System.err.println("Waiting for a client ... " + clientCount);

        try {
            while (running.get()) {
                ServerThread serverThread = new ServerThread(server.accept(), clientCount);
                clients.add(serverThread);
                serverThread.startServerThread();

                clientCount++;
                System.err.println("Waiting for a client ... " + clientCount);
            }
        } catch (IOException ex) {
        }
    }
}
