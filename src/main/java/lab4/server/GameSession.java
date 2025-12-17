package lab4.server;

import java.util.ArrayList;
import java.util.List;

import lab4.common.Board;
import lab4.common.JsonUtil;
import lab4.common.Move;

/**
 * Singleton: jedna sesja gry.
 *
 * Wzorce:
 * - Singleton: GameSession.getInstance()
 * - Observer (prymitywny): trzymamy listę ClientHandler i broadcastujemy
 * - DTO (data transfer object): Board i Move są przesyłane/serializowane przez JsonUtil
 */
public class GameSession 
{

    private static GameSession instance = null;

    // utwórz lub pobierz instancję (wywołujemy z ServerMain przy starcie)
    public static synchronized GameSession getInstance(int boardSize)
    {
        if (instance == null) instance = new GameSession(boardSize);
        return instance;
    }

    // pobierz istniejącą instancję (np. z handlerów)
    public static synchronized GameSession getInstance()
    {
        if (instance == null) throw new IllegalStateException("GameSession not initialized");
        return instance;
    }

    private final Board board;
    private final List<ClientHandler> observers = new ArrayList<>();
    private int currentPlayer = 1;
    private boolean started = false;
    private boolean gameOver = false;
    private int consecutivePasses = 0; // bo po 2x PASS konczymy gre

    // previousBoard used to detect Ko (position before last move)
    private int[][] previousBoard = null;

    private GameSession(int boardSize)
    {
        this.board = new Board(boardSize);
    }

    public synchronized void register(ClientHandler h)
    {
        if (observers.size() >= 2)
        {
            h.sendLine("ERROR Server already has two players");
            return;
        }
        observers.add(h);
    }

    public synchronized void startGame()
    {
        if (started) return;
        if (observers.size() != 2) 
        {
            System.out.println("Need exactly 2 players to start game");
            return;
        }
        started = true;
        gameOver = false;
        currentPlayer = 1;
        consecutivePasses = 0;
        previousBoard = null;

        for (ClientHandler h : observers) h.sendLine("START " + h.getPlayerId());
        broadcastBoard();
        notifyTurn();
    }

    private synchronized void notifyTurn()
    {
        for (ClientHandler h : observers)
        {
            if (h.getPlayerId() == currentPlayer) h.sendLine("YOUR_TURN"); //wysylamy do klienta ze jego ruch
            else h.sendLine("OPPONENT_TURN"); //albo ze kolej przeciwnika
        }
    }

    public synchronized void broadcastBoard()
    {
        String json = JsonUtil.boardToJson(board);
        for (ClientHandler h : observers) h.sendLine("BOARD " + json); //wysylamy klientowi board w json
    }

    private synchronized void broadcastInfo(String msg)
    {
        for (ClientHandler h : observers) h.sendLine("INFO " + msg);
    }

    // APPLY MOVE
    public synchronized void applyMove(Move m, ClientHandler ch)
    {
        if (gameOver) { ch.sendLine("ERROR Game already finished"); return; }
        if (m.player != ch.getPlayerId()) { ch.sendLine("ERROR Player id mismatch"); return; }
        if (m.player != currentPlayer) { ch.sendLine("ERROR Not your turn"); return; }

        // backup before move (for Ko detection and possible rollback)
        int[][] before = board.getGridCopy();

        int result = board.applyMoveAndCapture(m.row, m.col, m.player);

        if (result == -1) { ch.sendLine("ERROR Field occupied or out of bounds"); return; }
        if (result == -2) { ch.sendLine("ERROR Suicide move not allowed"); return; }

        // detect Ko: new board equal to previousBoard -> illegal
        if (previousBoard != null && Board.gridsEqual(board.getGridCopy(), previousBoard))
        {
            // rollback
            board.setGridFromCopy(before);
            ch.sendLine("ERROR Ko rule: immediate recapture not allowed");
            return;
        }

        // move accepted: set previousBoard = before (position before this move)
        previousBoard = before;

        // reset consecutive passes
        consecutivePasses = 0;

        broadcastBoard();
        if (result > 0) broadcastInfo("Player " + m.player + " captured " + result + " stone(s).");

        // change turn
        currentPlayer = (currentPlayer == 1 ? 2 : 1);
        notifyTurn();
    }

    // PASS
    public synchronized void playerPassed(ClientHandler ch)
    {
        if (gameOver) { ch.sendLine("ERROR Game already finished"); return; }
        if (ch.getPlayerId() != currentPlayer) { ch.sendLine("ERROR Not your turn"); return; }

        broadcastInfo("Player " + currentPlayer + " passed.");

        // For Ko: treat pass as a move that sets previousBoard to current position
        previousBoard = board.getGridCopy();

        consecutivePasses++;
        if (consecutivePasses >= 2)
        {
            gameOver = true;
            broadcastInfo("Both players passed. Game over.");
            broadcastBoard();
            for (ClientHandler h : observers) h.sendLine("GAME_OVER Both players passed");
            return;
        }

        currentPlayer = (currentPlayer == 1 ? 2 : 1);
        notifyTurn();
    }

    // RESIGN
    public synchronized void playerResigned(ClientHandler ch)
    {
        if (gameOver) { ch.sendLine("ERROR Game already finished"); return; }
        int winner = (ch.getPlayerId() == 1 ? 2 : 1);
        gameOver = true;
        broadcastInfo("Player " + ch.getPlayerId() + " resigned. Player " + winner + " wins.");
        for (ClientHandler h : observers) h.sendLine("GAME_OVER Player " + winner + " wins (resign)");
    }

    // client disconnected
    public synchronized void clientDisconnected(ClientHandler ch)
    {
        observers.remove(ch);
        if (!gameOver)
        {
            gameOver = true;
            for (ClientHandler o : observers)
            {
                o.sendLine("ERROR Opponent disconnected. Game ended.");
                o.sendLine("GAME_OVER Opponent disconnected");
            }
        }
    }
}