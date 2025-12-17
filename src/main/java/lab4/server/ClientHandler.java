package lab4.server;

import lab4.common.JsonUtil;
import lab4.common.Move;

import java.io.*;
import java.net.Socket;

/**
 * Handles a single client connection.
 * Accepts text commands (line-based). Commands are parsed case-insensitively.
 *
 * Allowed commands:
 *  - MOVE {json}
 *  - PASS
 *  - RESIGN
 *
 * Sends back lines like: (wysyla np. GameSession)
 *  - INFO ...
 *  - ERROR ...
 *  - BOARD ...
 */

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final int playerId;

    public ClientHandler(Socket socket, int playerId) throws IOException {
        this.socket = socket; // tutaj bierzemy socket (utworzony w ClientConnection) pozyskany przez serverSocket.accept() w ServerMain
        this.playerId = playerId;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    public int getPlayerId() { return playerId; }

    public void sendLine(String line) { // tutaj wysylamy linie do klienta !!!
        try { out.println(line); } catch (Exception e) { System.err.println("Send failed to p" + playerId + ": " + e.getMessage()); }
    }

    @Override
    public void run() {
        try {
            sendLine("INFO Connected as player " + playerId);
            String raw;
            while ((raw = in.readLine()) != null) {
                if (raw == null) break;
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) continue;

                // split into command and optional argument (like MOVE json)
                String[] parts = trimmed.split("\\s+", 2); // Rozdziel tekst po dowolnej liczbie białych znaków; Podziel maksymalnie na 2 części
                String cmd = parts[0].toUpperCase(); // komenda
                String arg = parts.length > 1 ? parts[1].trim() : ""; // argumenty

                switch (cmd) {
                    case "MOVE":
                        if (arg.isEmpty()) {
                            sendLine("ERROR MOVE requires JSON argument");
                        } else {
                            try {
                                Move m = JsonUtil.jsonToMove(arg);
                                m.player = this.playerId; // enforce player id !
                                GameSession.getInstance().applyMove(m, this); //przez obecnego clientHandlera obslugujemy move (wywolujac applyMove w GameSession)
                            } catch (IllegalArgumentException ex) {
                                sendLine("ERROR Bad move JSON: " + ex.getMessage());
                            }
                        }
                        break;

                    case "PASS":
                        GameSession.getInstance().playerPassed(this);
                        break;

                    case "RESIGN":
                        GameSession.getInstance().playerResigned(this);
                        break;

                    default:
                        sendLine("ERROR Unknown command: [" + cmd + "]");
                }
            }
        } catch (IOException e) {
            System.err.println("Client " + playerId + " disconnected: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            try { GameSession.getInstance().clientDisconnected(this); } catch (Exception ignored) {}
        }
    }
}