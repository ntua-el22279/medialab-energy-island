package gr.ntua.medialab.energyisland.model;

/**
 * A cell coordinate on the island grid. {@code x} is the column and {@code y} the row,
 * with {@code (0, 0)} at the top-left corner.
 *
 * @param x column index
 * @param y row index
 */
public record Position(int x, int y) {

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
