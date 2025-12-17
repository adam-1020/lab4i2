package lab4.common;

/**
 * DTO (Data Transfer Object) — Move: przesylamy jako JSON: {"row":R,"col":C,"player":P}
 */
public class Move {
    public int row;
    public int col;
    public int player;

    public Move() {} // zeby moc stworzyc Move i potem dopisac dane z JSON-a

    public Move(int row, int col, int player) {
        this.row = row;
        this.col = col;
        this.player = player;
    }

    //do ewentualnej pomocy przy printach
    @Override
    public String toString() {
        return "Move[player=" + player + ", row=" + row + ", col=" + col + "]";
    }
}