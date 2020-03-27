package trey314159.taed;

/**
  * Object to hold the info needed in a cell of our crazy edit distance table.
  *
  * Simple Levenshtein edit distance usually only needs an integer in each cell.
  * Damerau–Levenshtein may need floats for non-integral edits (e.g., if
  * swap costs 1.5).
  *
  * We need floats for cost, but we are also tracking per-token normalized length
  * and per-token edit cost in each cell.
  */
final class EditDistCell {
    private float cost; // the cost to get to this cell of the edit distance table
    private float tokenCost; // the cost to get to this cell in the current token
    private float tokenNormLength; // the normalized length of the current token so far

    private ComparisonInfo edCellCompInfo;

    private static final float OVER_LIMIT = Float.POSITIVE_INFINITY;

    /**
      * Constructor takes initial weights, and reference to the pair-specific
      * info object.
      *
      * @param cost initial value
      * @param tokenCost initial value
      * @param tokenNormLength initial value
      * @param theCompInfo the pair-specific computed info
      */
    EditDistCell(final float cost, final float tokenCost, final float tokenNormLength,
                 ComparisonInfo theCompInfo) {
        this.cost = cost;
        this.tokenCost = tokenCost;
        this.tokenNormLength = tokenNormLength;
        this.edCellCompInfo = theCompInfo;
    }

    EditDistCell(ComparisonInfo theCompInfo) {
        this(0f, 0f, 0f, theCompInfo);
    }

    // accessors for cost, tokenCost, or tokenNormLength
    void setTokenNormLength(final float tokenNormLength) {
        this.tokenNormLength = tokenNormLength;
    }

    float getCost() {
        return this.cost;
    }

    float getTokenNormLength() {
        return this.tokenNormLength;
    }

    // set cost and tokenCost from another EditDistCell
    void setCosts(final EditDistCell otherCell) {
        this.cost = otherCell.cost;
        this.tokenCost = otherCell.tokenCost;
    }

    /*
     * Set cost and tokenCost from another EditDistCell, and check if the
     * source cell is over per-token limits.
     *
     * @param otherCell the edit distance cell we are copying costs from
     * @param atTokenEdge whether we are at a token edge in either direction
     * @param perTokLim whether we care about per-token limits
     */
    void setCostsAndCheckTokenEdge(final EditDistCell otherCell,
                                   boolean atTokenEdge, boolean perTokLim) {
        this.cost = otherCell.cost;
        this.tokenCost = otherCell.tokenCost;
        if (atTokenEdge && perTokLim && !edCellCompInfo.isSpacelessEquals() &&
                otherCell.isOverTokenEditLimit(perTokLim)) {
            this.cost = OVER_LIMIT;
        }
    }

    // new token, same old string; reset per-token values to 0
    void startNewToken() {
        this.tokenCost = 0f;
        this.tokenNormLength = 0f;
    }

    // update both costs by the same amount
    // (cost and tokenCost tend to increment together)
    void incrementCosts(final float incr) {
        this.cost += incr;
        this.tokenCost += incr;
    }

    /*
     * Compare EditDistCell costs and copy if the other one is less (tokenCosts
     * are just along for the ride).
     *
     * @param otherCell the edit distance cell we are copying costs from
     */
    void setIfCostsLess(final EditDistCell otherCell) {
        if (otherCell.cost < this.cost) {
            this.setCosts(otherCell);
        }
    }

    /**
      * Determine whether the token has too many edits.
      *
      * Bail if we are not looking at per-token limits or we have two strings
      * that have spaceless equality
      *
      * @param perTokLim whether we care about per-token limits
      *
      * @return true/false based on whether the token has too many edits
      */
    boolean isOverTokenEditLimit(final boolean perTokLim) {

        if (!perTokLim || edCellCompInfo.isSpacelessEquals()) {
            return false;
        }

        if (edCellCompInfo.getCurrEditLimit() > 0
                && tokenCost > edCellCompInfo.getCurrEditLimit()) {
            // too many edits by the raw numbers
            return true;
        }
        if (edCellCompInfo.getCurrEditNormLimit() > 0 &&
                tokenCost > tokenNormLength * edCellCompInfo.getCurrEditNormLimit()) {
            // too many edits as a percentage of this token length
            return true;
        }

        return false;
    }
}
