package lab4.common;

import java.util.Stack;

/**
 * DTO + logika planszy (capture, suicide check).
 *
 * Uwaga: GameSession zarządza turą, KO, passami i obserwatorami.
 */
public class Board {
    public final int size;
    public int[][] grid; // 0 empty, 1 player1 (X), 2 player2 (O) !!!

    public Board(int size) {
        this.size = size;
        this.grid = new int[size][size];
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < size && c < size;
    }

    public synchronized boolean isEmpty(int r, int c) {
        return inBounds(r, c) && grid[r][c] == 0;
    }

    /**
     * Apply move with captures. Returns:
     *  -1 => occupied / out of bounds
     *  -2 => suicide (illegal)
     *  >=0 => number of captured enemy stones
     *
     * The board is modified (synchronized).
     */
    public synchronized int applyMoveAndCapture(int r, int c, int player) {
        if (!inBounds(r, c)) return -1;
        if (grid[r][c] != 0) return -1; //czyli jest empty

        // place temporarily
        grid[r][c] = player;

        int enemy = (player == 1 ? 2 : 1);
        int captured = 0;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}}; // do iteracji po kierunkach na boki, gora i dol
        // check neighbor enemy groups for capture
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (!inBounds(nr,nc)) continue;
            if (grid[nr][nc] == enemy) {
                if (!hasLiberties(grid, nr, nc)) { // sprawdzamy czy jakis wrogi sasiad nie ma teraz 0 oddechow (umieramy go)
                    captured += removeGroup(nr, nc, enemy); // dostajemy punkty; usuwamy zlepek kulek wroga
                }
            }
        }

        // check if placed group has liberties now
        if (!hasLiberties(grid, r, c)) {
            if (captured == 0) {
                // suicide -> revert
                grid[r][c] = 0;  // zerujemy, czyli pole pozostaje puste
                return -2;
            }
            // else: if captured > 0, the move can be legal (captures freed liberties)
        }

        return captured;
    }

    // removes group of 'color' starting at r,c and returns how many stones removed
    private int removeGroup(int r, int c, int color) {
        if (!inBounds(r,c)) return 0;
        if (grid[r][c] != color) return 0;

        int removed = 0;
        Stack<int[]> st = new Stack<>();
        st.push(new int[]{r,c}); // opis w funkcji nizej
        // mark visited by negating color (temporary marker); zeby nie wpasc w petle z tymi ktore juz zaznaczylismy
        grid[r][c] = -color;

        while (!st.isEmpty()) {
            int[] p = st.pop();
            int pr = p[0], pc = p[1];
            removed++;
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : dirs) {
                int nr = pr + d[0], nc = pc + d[1];
                if (!inBounds(nr,nc)) continue;
                if (grid[nr][nc] == color) {
                    st.push(new int[]{nr,nc});
                    grid[nr][nc] = -color;
                }
            }
        }

        // clear markers -> set to 0 (removed)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == -color) grid[i][j] = 0;
            }
        }

        return removed;
    }

    // check if group at r,c has liberties on the given board array
    // does not modify boardCopy
    private boolean hasLiberties(int[][] boardCopy, int r, int c) {
        if (!inBounds(r,c)) return false;
        int color = boardCopy[r][c];
        if (color == 0) return true;

        boolean[][] visited = new boolean[size][size];
        Stack<int[]> st = new Stack<>();
        st.push(new int[]{r,c}); // wrzucamy pierwszy kamien
        visited[r][c] = true; // odwiedzony

        while (!st.isEmpty()) {
            int[] p = st.pop();
            int pr = p[0], pc = p[1];
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}}; // cztery kierunki
            for (int[] d : dirs) {
                int nr = pr + d[0], nc = pc + d[1];
                if (!inBounds(nr,nc)) continue;
                if (boardCopy[nr][nc] == 0) return true; // znalezlismy wolne pole, czyli ma jakis oodech
                if (!visited[nr][nc] && boardCopy[nr][nc] == color) {
                    visited[nr][nc] = true;
                    st.push(new int[]{nr,nc});
                }
            }
        }
        return false;
    }

    // returns deep copy of grid
    public synchronized int[][] getGridCopy() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++) System.arraycopy(grid[i], 0, copy[i], 0, size);
        return copy;
    }

    // restore from copy
    public synchronized void setGridFromCopy(int[][] src) {
        if (src == null || src.length != size) return;
        for (int i = 0; i < size; i++) System.arraycopy(src[i], 0, grid[i], 0, size);
    }

    // static compare
    public static boolean gridsEqual(int[][] a, int[][] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length; j++) {
                if (a[i][j] != b[i][j]) return false;
            }
        }
        return true;
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        for (int c = 0; c < size; c++) sb.append(String.format("%2d", c));
        sb.append("\n");
        for (int r = 0; r < size; r++) {
            sb.append(String.format("%2d: ", r));
            for (int c = 0; c < size; c++) {
                char ch = '.';
                if (grid[r][c] == 1) ch = 'X';
                if (grid[r][c] == 2) ch = 'O';
                sb.append(" ").append(ch);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}