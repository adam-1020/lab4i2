package lab4.client;

import lab4.common.Board;
import lab4.common.JsonUtil;
import lab4.common.Move;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalTime;

/**
 * Swing GUI client (działa jak ClientMain konsolowy, można ich używać zamiennie)
 * - boczna konsola (stylowana, auto-scroll)
 * - wyraźny panel statusu (czyja tura / agreement)
 * - estetyczny panel punktów obu graczy (przydatne przy ustalaniu punktów, żeby wiedzieć kto ile zbił)
 * - kamienie na przecieciach linii
 * - zgodne z logiką serwera
 */
public class SwingClientMain {
    // networking / state
    private ClientConnection conn;
    private int myId = -1;
    private boolean myTurn = false;
    private boolean stoppedForAgreement = false;
    private final int[] wyniki = {0, 0};
    private Board board;

    // Swing
    private JFrame frame;
    private BoardPanel boardPanel; // nasza klasa

    private JLabel turnLabel;
    private JLabel agreementLabel;
    private JLabel scoreLabel;
    private JLabel playerLabel;

    private JButton passBtn;
    private JButton resignBtn;
    private JButton resumeBtn;
    private JButton finishBtn;

    private JTextPane console;
    private StyledDocument consoleDoc;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingClientMain().start()); // kod wewnątrz niej (stworzenie lambdą SwingClientMain oraz wywołanie start) zostanie wykonany na głównym wątku aplikacji Swing
    }

    private void start() {
        try {
            conn = new ClientConnection("localhost", 55555);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection failed: " + e.getMessage());
            return;
        }
        // jak połączenie się powiedzie
        buildGui();
        registerHandlers();
    }

    // GUI
    private void buildGui() {
        frame = new JFrame("SwingClient"); // nazwa okna aplikacji
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout()); // N E W S / Center

        boardPanel = new BoardPanel(); // tworzymy instancja naszej klasy
        frame.add(boardPanel, BorderLayout.CENTER); // boardPanel na środku

        frame.add(buildRightPanel(), BorderLayout.EAST); // tworzymy prawy i dolny panel i dołączamy
        frame.add(buildBottomPanel(), BorderLayout.SOUTH);

        frame.setSize(1100, 750);
        frame.setVisible(true);
        updateButtons(); // ustawia widoczność przycisków (setEnabled)
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(1, 4)); // 1 wiersz; 4 kolumny
        bottom.setBorder(new EmptyBorder(8, 8, 8, 8)); // margines 8px z każdej strony

        playerLabel = label("You are: ?");
        turnLabel = label("Waiting for game..."); // potem będzie aktualizowane na np. Your turn
        agreementLabel = label(" "); // na razie pusty label, zmienimy napis jak bedzie waitingForAgreement
        scoreLabel = label("P1: 0   P2: 0");

        bottom.add(playerLabel);
        bottom.add(turnLabel);
        bottom.add(agreementLabel);
        bottom.add(scoreLabel);
        return bottom;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout()); // layout N E W S / Center
        right.setPreferredSize(new Dimension(300, 600));

        // buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(0, 1, 5, 5)); // liczba wierszy, kolumn, px odstęp poziomy między elementami, odstęp pionowy między elementami (5 px)
        buttons.setBorder(new EmptyBorder(8, 8, 8, 8)); // margines 8px a kazdej strony

        passBtn = new JButton("PASS");
        resignBtn = new JButton("RESIGN");
        resumeBtn = new JButton("RESUME");
        finishBtn = new JButton("FINISH");

        passBtn.addActionListener(e -> conn.sendLine("PASS"));
        resignBtn.addActionListener(e -> conn.sendLine("RESIGN"));
        resumeBtn.addActionListener(e -> conn.sendLine("RESUME"));
        finishBtn.addActionListener(e -> conn.sendLine("FINISH"));

        buttons.add(passBtn);
        buttons.add(resignBtn);
        buttons.add(resumeBtn);
        buttons.add(finishBtn);

        // console
        console = new JTextPane();
        console.setEditable(false);
        console.setFont(new Font("Consolas", Font.PLAIN, 12));
        console.setBackground(new Color(20, 20, 20));
        console.setForeground(Color.LIGHT_GRAY);
        // consoleDoc jest typu StyledDocument
        consoleDoc = console.getStyledDocument(); //getStyledDocument() zwraca obiekt StyledDocument, który: pozwala na formatowanie tekstu (kolory, style, czcionki), będzie używany do wypisywania komunikatów w „konsoli” aplikacji.
        initConsoleStyles(); // w tej funkcji inicjalizujemy style (kolory dla różnych komunikatów)

        JScrollPane scroll = new JScrollPane(console); // Gdy dodajemy tekst do consoleDoc, zmienia się dokument JTextPane się odświeża, JScrollPane od razu to pokazuje

        right.add(buttons, BorderLayout.NORTH); // przyciski na gorze
        right.add(scroll, BorderLayout.CENTER); // konsola na srodku
        return right;
    }

    // pomocnicza funkcja używana w buildBottomPanel()
    private JLabel label(String txt) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER); // wyśrodkowanie tekstu w poziomie wewnątrz etykiety
        l.setFont(new Font("Arial", Font.BOLD, 14)); // czcionka
        return l;
    }

    private void updateButtons() {
        passBtn.setEnabled(myTurn && !stoppedForAgreement);
        resignBtn.setEnabled(!stoppedForAgreement);
        resumeBtn.setEnabled(stoppedForAgreement);
        finishBtn.setEnabled(stoppedForAgreement);
    }

    // console
    private void initConsoleStyles() {
        addStyle("INFO", Color.LIGHT_GRAY);
        addStyle("ERROR", Color.RED);
        addStyle("SYSTEM", new Color(120, 180, 255));
    }

    private void addStyle(String name, Color c) {
        Style s = console.addStyle(name, null);
        StyleConstants.setForeground(s, c);
    }

    // do wpisywania własnych logów z użyciem wcześniej zadeklarowanych stylów (kolorów)
    private void log(String type, String msg) {
    }

    // handlers (do zaimplementowania)
    private void registerHandlers() {
        conn.startListening(new ClientConnection.MessageHandler() {
            @Override public void onStart(int id) {
            }

            @Override public void onBoard(Board b) {
            }

            @Override public void onYourTurn() {
            }

            @Override public void onOpponentTurn() {
            }

            @Override public void onInfo(String msg) {
            }

            @Override public void onError(String msg) {
            }

            @Override public void onGameOver(String msg) {
            }

            @Override public void onDisconnect() {
            }

            @Override public void onUnknown(String line) {
            }

            @Override public void onstoppedForAgreement() {
            }

            @Override public void offstoppedForAgreement() {
            }

            @Override public void wynikiPierwszego(int a) {
            }

            @Override public void wynikiDrugiego(int a) {
            }
        });
    }

    // BoardPanel !!! (do zaimplementowania)
    private class BoardPanel extends JPanel {
        private static final int M = 40; // margines planszy od krawędzi

        BoardPanel() {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    //tutaj zrób obsługę kliknięcia w przecięcie co wysyła move
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (board == null) return;

            //tutaj zrób żeby rysowalo tło na planszy, siatkę przecięć i podwójną petlą kamienie z board
        }
    }
}