import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static final List<ObjectOutputStream> clientStreams = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected...");
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientStreams.add(out);
                new Thread(new ClientHandler(clientSocket, out)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(Object message) {
        synchronized (clientStreams) {
            for (ObjectOutputStream out : clientStreams) {
                try {
                    out.writeObject(message);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream out;

        public ClientHandler(Socket socket, ObjectOutputStream out) {
            this.socket = socket;
            this.out = out;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                Object message;
                while ((message = in.readObject()) != null) {
                    broadcast(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client disconnected.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientStreams) {
                    clientStreams.remove(out);
                }
            }
        }
    }
}
