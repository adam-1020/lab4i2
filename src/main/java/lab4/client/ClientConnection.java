package lab4.client;

import lab4.common.Board;
import lab4.common.JsonUtil;

import java.io.*;
import java.net.Socket;

/**
 * Simple line-based connection to server.
 */
public class ClientConnection {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public ClientConnection(String host, int port) throws IOException {
        socket = new Socket(host, port); // tworzymy nowy socket i do niego mamy in i out (z niego)
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    public void sendLine(String line) {
        out.println(line); // wypisuje line do strumienia out; czyli wysyla tekst do serwera !!!!
    }

    public void sendMoveJson(String json) {
        sendLine("MOVE " + json);
    }

    public void startListening(MessageHandler handler) {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) { //odbierane linie (z serwera)
                    if (line.startsWith("START ")) {
                        handler.onStart(Integer.parseInt(line.substring(6).trim()));
                    } else if (line.startsWith("BOARD ")) {
                        try {
                            Board b = JsonUtil.jsonToBoard(line.substring(6).trim());
                            handler.onBoard(b);
                        } catch (Exception e) {
                            System.err.println("Failed parse BOARD JSON: " + e.getMessage());
                        }
                    } else if (line.equals("YOUR_TURN")) {
                        handler.onYourTurn();
                    } else if (line.equals("OPPONENT_TURN")) {
                        handler.onOpponentTurn();
                    } else if (line.startsWith("INFO ")) {
                        handler.onInfo(line.substring(5));
                    } else if (line.startsWith("ERROR ")) {
                        handler.onError(line.substring(6));
                    } else if (line.startsWith("GAME_OVER")) {
                        handler.onGameOver(line.substring(9).trim());
                    } else {
                        handler.onUnknown(line);
                    }
                }
            } catch (IOException e) {
                handler.onDisconnect();
            }
        }, "ServerListener").start();
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public interface MessageHandler {
        void onStart(int myId);
        void onBoard(Board b);
        void onYourTurn();
        void onOpponentTurn();
        void onInfo(String msg);
        void onError(String msg);
        void onGameOver(String msg);
        void onDisconnect();
        void onUnknown(String line);
    }
}