package trey314159.taed;

/**
  * A whole row of EditDistCells in our crazy edit distance table.
  *
  * Handle row-specific intialization and updates.
  */
final class EditDistRow {
    private final EditDistCell[] row;
    private final ComparisonInfo edRowCompInfo;

    /**
      * Constructor for EditDistRow
      *
      * Create a row of EditDistCells of the right size, and initialize.
      *
      * @param edItem the edit dist item we are building the row for
      * @param theCompInfo the pair-specific computed info
      */
    EditDistRow(EditDistItem edItem, ComparisonInfo theCompInfo) {
        edRowCompInfo = theCompInfo;
        int length = 1 + edItem.text.length;
        row = new EditDistCell[length];
        for (int i = 0; i < length; i++) {
            row[i] = new EditDistCell(edRowCompInfo);
        }
    }

    /**
      * Initialize this as the first row of the edit dist table, based on
      * the second edit dist item.
      *
      * @param edItem2 the second item being compared (goes
      *     across the top of the table).
      */
    void initFirstRow(final EditDistItem edItem2) {
        assert edItem2.text.length + 1 == row.length;
        assert edItem2.text.length > 0;

        // Initialize the very first cell
        // row[0] is already initialized to 0,0,0

        EditDistCell nextCost = new EditDistCell(edRowCompInfo); // temp var for candidate cost

        // initialize the rest of the first row as all insertions
        // Note: row{Curr|Next|Prev}[i] corresponds to str[i - 1]
        for (int i = 1; i <= edItem2.text.length; i++) {

            // copy from cell to left, add insert cost
            nextCost.setCosts(row[i - 1]);
            nextCost.incrementCosts(edItem2.calcInsDelCost(i - 1, edRowCompInfo));
            row[i].setCosts(nextCost);

            // if this is a new token, reset token costs
            if (edItem2.isTokenSep(i - 1)) {
                row[i].startNewToken();
            }
        }
    }

    /**
      * Initialize this as the first cell in the "next" row of the edit dist
      * table, based on the first edit dist item and the current row.
      *
      * @param rowCurr the "current" row above this row in the edit dist table
      * @param edItem1 the first item being compared (goes down the side of the table).
      * @param idx index of relevant codepoint in edItem1
      *
      * @return cost for this cell
      */
    float initFirstCell(final EditDistRow rowCurr, final EditDistItem edItem1,
                        final int idx) {
        EditDistCell nextCost = new EditDistCell(edRowCompInfo); // temp var for candidate cost

        // initialize the first column of the new row; copy from above and
        // add delete cost.
        nextCost.setCosts(rowCurr.row[0]);
        nextCost.incrementCosts(edItem1.calcInsDelCost(idx, edRowCompInfo));
        row[0].setCosts(nextCost);

        // however, if this is a new token in str1, reset token costs
        if (edItem1.isTokenSep(idx)) {
            row[0].startNewToken();
        }

        return row[0].getCost();
    }

    EditDistCell getRow(int i) {
        return row[i];
    }
}
