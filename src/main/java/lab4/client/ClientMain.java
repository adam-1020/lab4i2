package lab4.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import lab4.common.Board;
import lab4.common.JsonUtil;
import lab4.common.Move;

/**
 * Console client:
 * - commands: row col  (MOVE), PASS, RESIGN, quit/exit
 * - trims and uppercases commands, so PASS/Resign/move are robust against whitespace/case
 */
public class ClientMain
{
     public static void main(String[] args) throws IOException
     {
        String host = "localhost";
        int port = 55555;

        final ClientConnection conn;
        try
        {
            conn = new ClientConnection(host, port);
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return;
        }

        final boolean[] myTurn = {false};
        final int[] myId = {-1}; //tablica jednoelementowa; finalna; ale jej elementy mozna zmieniac

        conn.startListening(new ClientConnection.MessageHandler() {
            @Override
            public void onStart(int myId0)
            {
                myId[0] = myId0;
                System.out.println("Game started. You are player " + myId0 + " (X=1, O=2)");
            }

            @Override
            public void onBoard(Board b)
            {
                System.out.println("--- BOARD ---");
                System.out.println(b.toString());
            }

            @Override
            public void onYourTurn()
            {
                myTurn[0] = true;
                System.out.println("Your turn. Enter: row col   (or type PASS or RESIGN)");
            }

            @Override
            public void onOpponentTurn()
            {
                myTurn[0] = false;
                System.out.println("Waiting for opponent...");
            }

            @Override
            public void onInfo(String msg)
            {
                System.out.println("[INFO] " + msg);
            }

            @Override
            public void onError(String msg)
            {
                System.err.println("[ERROR] " + msg);
            }

            @Override
            public void onGameOver(String msg)
            {
                System.out.println("[GAME OVER] " + msg);
                System.exit(0);
            }

            @Override
            public void onDisconnect()
            {
                System.err.println("Disconnected from server.");
                System.exit(0);
            }

            @Override
            public void onUnknown(String line)
            {
                System.out.println("[SERVER] " + line);
            }
        });

        System.out.println("Connected. Wait until game starts...");
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true) 
        {
            String raw = console.readLine();
            if (raw == null) break;
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String up = line.toUpperCase();

            // immediate quit/exit/resign
            if (up.equals("QUIT") || up.equals("EXIT"))
            {
                conn.sendLine("RESIGN");
                conn.close();
                break;
            }

            if (up.equals("RESIGN"))
            {
                conn.sendLine("RESIGN");
                continue;
            }

            if (up.equals("PASS"))
            {
                conn.sendLine("PASS");
                continue;
            }

            // otherwise try to parse move row col
            if (!myTurn[0])
            {
                System.out.println("Not your turn yet.");
                continue;
            }

            String[] parts = line.split("\\s+"); // \s -> dowolny znak biały; jeden lub wiecej takich znakow
            if (parts.length < 2)
            {
                System.out.println("Bad input. Use: row col   or PASS   or RESIGN");
                continue;
            }

            try {
                int r = Integer.parseInt(parts[0]);
                int c = Integer.parseInt(parts[1]);
                Move m = new Move(r, c, myId[0]);
                String json = JsonUtil.moveToJson(m);
                conn.sendMoveJson(json);
            } catch (NumberFormatException e) {
                System.out.println("Bad numbers: " + e.getMessage());
            }
        }
    }

}
