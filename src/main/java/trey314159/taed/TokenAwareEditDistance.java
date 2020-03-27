package trey314159.taed;

import java.util.Arrays;

import lombok.Builder;

/* Token-Aware Edit Distance
 *
 * Compute a modified Damerau–Levenshtein edit distance (inserts, deletions,
 * substitutions, transpositions) that also accounts for duplicated characters,
 * multi-token strings, token-initial character changes, per-string edit limits,
 * per-token edit limits, length-proportional edit limits (for strings and
 * tokens), token-count differences, and strings that differ only by tokenization.
 */
@Builder
public final class TokenAwareEditDistance {

    // use Infinity for being over any edit limits, since you can still do
    // math to it.
    private static final float OVER_LIMIT = Float.POSITIVE_INFINITY;

    // internal configuration, abstracted into a class so we can keep
    // track of what is what.
    private final EDConfig edConfig;

    public TokenAwareEditDistance(final EDConfig edConfig) {
        // the builder loses the reference to this config object, so it is
        // effectively immutable
        this.edConfig = edConfig;
    }

    /**
     * Computes the cost of weighted edits required to transform str1 into str2,
     * using the default edit limits for this TokenAwareEditDistance instance.
     *
     * @param str1 the first string to be compared
     * @param str2 the second string to be compared
     * @return the computed edit distance between the two strings;
     * returns Float.POSITIVE_INFINITY for early termination OR if the edit
     * distance is over either limit.
     */
    public float calcEditDistance(final String str1, final String str2) {
        return calcEditDistance(str1, str2, edConfig.getDefaultLimit(), edConfig.getDefaultNormLimit());
    }

    /**
     * Computes the cost of weighted edits required to transform str1 into str2.
     * <p>
     * Allowed edits include inserting, deleting, substituting, transposing, or
     * (de-)duplicating letters. Additional penalties are applied for changing
     * the first character of a token, changing the number of tokens in the
     * string, or modifying digits. There is special discounted processing for
     * strings that are the same modulo the tokenSep character.
     * <p>
     * A maximum allowable edit distance (editLimit) or proportional edit
     * distance (editNormLimit) can be used to terminate early based on the cost
     * of edits (for the whole string or per token) or based on changes to the
     * number of tokens. In the case of early termination the edit distance returned
     * is Float.POSITIVE_INFINITY.
     *
     * @param str1          the first string to be compared
     * @param str2          the second string to be compared
     * @param editLimit     the maximum relevant edit distance; 0 = no limit
     * @param editNormLimit the maximum relevant edit distance as a percentage;
     *                      0 = no limit
     * @return the computed edit distance between the two strings;
     * returns Float.POSITIVE_INFINITY for early termination OR if the edit
     * distance is over either limit.
     */
    // Cyclomatic Complexity was 66 and NPath Complexity was
    // 60,552,907,617,280! It's much lower now. Going further just seems
    // to be "optimizing the metric". Suppress the remaining warnings.
    @SuppressWarnings({"NPathComplexity", "CyclomaticComplexity"})
    public float calcEditDistance(final String str1, final String str2,
            final float editLimit, final float editNormLimit) {

        final EditDistItem edItem1 = new EditDistItem(str1, edConfig);
        final EditDistItem edItem2 = new EditDistItem(str2, edConfig);

        /////////////////
        // Check for simple equality (after tokenization, not of the original
        // strings)
        if (Arrays.equals(edItem1.text, edItem2.text)) {
            return 0;
        }

        // copy internal-only per-comparison computed values into a ComparisonInfo
        // object so we can pass them around together: absolute edit limit,
        // percentage (norm) edit limit, and "spaceless equality" (e.g., same as
        // "sp acel esseq uali ty")
        boolean spacelessEq = edItem1.spacelessText.equals(edItem2.spacelessText);
        ComparisonInfo compInfo = new ComparisonInfo(editLimit, editNormLimit,
                spacelessEq);

        if (edItem1.text.length == 0 || edItem2.text.length == 0) {
            // max is non-zero value
            return calcEmptyInputRetVal(
                    Math.max(edItem1.normLength, edItem2.normLength),
                    edItem1.text.length,
                    compInfo
            );
        }

        float tokenDiffPenalty = edItem1.calcTokenDiffPenalty(edItem2, compInfo);

        float adjustedEditLimit = 0f;

        boolean limitsExist = editLimit > 0 || editNormLimit > 0;

        if (limitsExist) {
            adjustedEditLimit = calcAdjustedEditLimit(edItem1.normLength, edItem2.normLength, compInfo);
            adjustedEditLimit -= tokenDiffPenalty;

            if (adjustedEditLimit < edItem1.calcUniqCharMinCost(edItem2)) {
                // early termination based on unique characters or token diffs
                // (if adjustedEditLimit < 0); note that calcUniqCharMinCost() is
                // always >= 0
                return OVER_LIMIT;
            }
        }

        // Since we aren't calculating the edit path, just the total distance, we
        // only need three working rows for modified Damerau–Levenshtein distance
        // with swaps.
        //
        // Create three rows of length 1 + |str2| to hold our work. 1 + ... because
        // we need an additional element for the initial column. As a result,
        // row{Curr|Next|Prev}[i] corresponds to str[i - 1]
        //
        // Prev is row before Curr, for looking back for swaps
        // Curr is the current row
        // Next is the row we are filling in

        EditDistRow rowPrev = new EditDistRow(edItem2, compInfo);
        EditDistRow rowCurr = new EditDistRow(edItem2, compInfo);
        EditDistRow rowNext = new EditDistRow(edItem2, compInfo);

        rowCurr.initFirstRow(edItem2);

        // for each character of str1, compute the cost (rowNext) from the
        // row before (rowCurr) (with rowPrev available to check for swaps)
        for (int i = 0; i < edItem1.text.length; i++) {

            // minimum value for a given row (for early termination)
            float rowMin = rowNext.initFirstCell(rowCurr, edItem1, i);

            // compute the cost of the rest of the row
            for (int j = 0; j < edItem2.text.length; j++) {

                EditDistCell minCost = new EditDistCell(compInfo);
                // minimum cost to get to the current cell

                EditDistCell nextCost = new EditDistCell(compInfo);
                // temp var for candidate cost

                // is either string at the start of a token?
                boolean atTokenEdge = edItem1.isTokenSep(i) || edItem2.isTokenSep(j);

                /////////////////////////////////
                // check substitution vs equality

                // initialize minCost assuming equality (no add'l cost; copy diagonally)
                // then add substitution cost as needed
                minCost.setCostsAndCheckTokenEdge(rowCurr.getRow(j), atTokenEdge,
                        edConfig.isPerTokenLimit());
                minCost.incrementCosts(edItem1.calcSubstCost(i, edItem2, j));

                /////////////
                // check swap

                // check for possible swap (rowPrev has costs from two rows ago); no
                // penalty for swapping the first character of a token and a token
                // separator
                if (edItem1.isSwapped(i, edItem2, j)) {
                    // copy cost from two back diagonally & add swap cost
                    nextCost.setCostsAndCheckTokenEdge(rowPrev.getRow(j - 1), atTokenEdge,
                            edConfig.isPerTokenLimit());
                    nextCost.incrementCosts(edItem1.calcSwapCost(i, edItem2, j));
                    minCost.setIfCostsLess(nextCost);
                }

                //////////////////
                // check insertion

                // copy from previous column, plus penalty for digit or first char in token
                nextCost.setCostsAndCheckTokenEdge(rowNext.getRow(j), atTokenEdge,
                        edConfig.isPerTokenLimit());
                nextCost.incrementCosts(edItem2.calcInsDelCost(j, compInfo));
                minCost.setIfCostsLess(nextCost);

                /////////////////
                // check deletion

                // copy from previous row, plus penalty for digit or first char in token
                nextCost.setCostsAndCheckTokenEdge(rowCurr.getRow(j + 1), atTokenEdge,
                        edConfig.isPerTokenLimit());
                nextCost.incrementCosts(edItem1.calcInsDelCost(i, compInfo));
                minCost.setIfCostsLess(nextCost);

                // assign cost of minimum cost path
                rowNext.getRow(j + 1).setCosts(minCost);

                //////////////////////////
                // do the per-token things

                // calculate the normalized token length for this token so far
                rowNext.getRow(j + 1).setTokenNormLength(calcTokenNormLength(edItem1, edItem2, i, j,
                        rowNext.getRow(j).getTokenNormLength(),
                        rowCurr.getRow(j + 1).getTokenNormLength()));

                // if we are at a token boundary, start a new token
                if (atTokenEdge) {
                    rowNext.getRow(j + 1).startNewToken();
                }

                // update rowMin if this is lower
                rowMin = Math.min(rowNext.getRow(j + 1).getCost(), rowMin);
            }

            // rotate rowCurr, rowNext, rowPrev for next round of calculations
            EditDistRow rowTemp = rowCurr;
            rowCurr = rowNext; // rowCurr is now the most up-to-date
            rowNext = rowPrev;
            rowPrev = rowTemp;

            if (limitsExist && rowMin > adjustedEditLimit) {
                // early termination based on too many edits on this row
                return OVER_LIMIT;
            }
        }

        // check final edit distance cell against per-token edit limits
        if (rowCurr.getRow(edItem2.text.length).isOverTokenEditLimit(edConfig.isPerTokenLimit())) {
            // final token has too many edits
            return OVER_LIMIT;
        }

        // if there are too many total edits in the final cell of the table, bail
        if (limitsExist && rowCurr.getRow(edItem2.text.length).getCost() > adjustedEditLimit) {
            // total edits for the whole string are over the limit
            return OVER_LIMIT;
        }

        // pull out final edit distance, add penalty for differing number of
        // tokens, and return
        return rowCurr.getRow(edItem2.text.length).getCost() + tokenDiffPenalty;
    }

    /**
     * Calculate the maximum normalized edit limit based on the norm type and
     * normalized lengths of the items being compared.
     *
     * @param len1 normalized length of the first string
     * @param len2 normalized length of the second string
     * @return maximum normalized edit limit
     */
    private float calcEditNormLimitByType(final float len1, final float len2, ComparisonInfo compInfo) {
        // if getCurrEditNormLimit() == 0, there is no limit
        if (compInfo.getCurrEditNormLimit() <= 0) {
            return 0f;
        }

        switch (edConfig.getNormType()) {
            case MIN:
                return compInfo.getCurrEditNormLimit() * Math.min(len1, len2);
            case FIRST:
                return compInfo.getCurrEditNormLimit() * len1;
            case MAX:
                return compInfo.getCurrEditNormLimit() * Math.max(len1, len2);
            default:
                throw new UnsupportedOperationException("Don't know how to handle case " + edConfig.getNormType());

        }
    }

    /**
     * Calculate the "adjusted" edit limit, which we actually use to check for
     * early termination.
     * <p>
     * Also account for possible decrease in edit cost if insDelCost > swapCost.
     *
     * @param len1 normalized length of the first string
     * @param len2 normalized length of the second string
     * @param compInfo per-pair comparison values
     * @return the maximum number of edits we can allow
     */
    private float calcAdjustedEditLimit(final float len1, final float len2, ComparisonInfo compInfo) {

        float normEditMax = calcEditNormLimitByType(len1, len2, compInfo);

        float adjLimit;
        // if both limits are > 0, i.e., both limits are in effect, then take
        //      the lower limit.
        // if either is <= 0, then take the max, i.e., ignoring the 0 limit
        //      if both are <= 0, the max will also be <= 0 (indicating no limit).
        if (compInfo.getCurrEditLimit() > 0 && normEditMax > 0) {
            adjLimit = Math.min(compInfo.getCurrEditLimit(), normEditMax);
        } else {
            adjLimit = Math.max(compInfo.getCurrEditLimit(), normEditMax);
        }

        // if swap cost is less than insert/delete cost, then cost can go
        // *down* from one row to the next, so adjust edit limit to account
        // for that.
        if (edConfig.getSwapCost() < edConfig.getInsDelCost()) {
            adjLimit += edConfig.getInsDelCost() - edConfig.getSwapCost();
        }
        return adjLimit;
    }

    /**
     * Calculate the normalized length of the token so far based on the cell
     * above and the cell to the left, based on the normalization type.
     * <p>
     * This requires a lot of information, so we have to pass a lot in. We need
     * the editDistItems and indexes into them so we can determine whether
     * relevant characters are duplicated or token separators. We need the
     * current normalized token length of the cell to the left and the cell
     * above to build on.
     *
     * @param ed1      edit dist item for the first string
     * @param ed2      edit dist item for the second string
     * @param idx1     index of current char in ed1
     * @param idx2     index of current char in ed2
     * @param tnlLeft  the tokenNormLength from the cell to the left
     * @param tnlAbove the tokenNormLength from the cell above
     * @return the normalized length of the token so far
     */
    private float calcTokenNormLength(final EditDistItem ed1, final EditDistItem ed2,
                                      final int idx1, final int idx2, final float tnlLeft,
                                      final float tnlAbove) {

        // if we are at a token boundary, we may need to increment
        // tokenNormalizedLength; calculate the increments (with discount for
        // duplicates)
        float incrCostLeft = ed2.isDuplicate(idx2) ? edConfig.getDuplicateCost() : edConfig.getInsDelCost();
        float incrCostAbove = ed1.isDuplicate(idx1) ? edConfig.getDuplicateCost() : edConfig.getInsDelCost();

        switch (edConfig.getNormType()) {
            case MIN:
                // take the min of tokenNormLength to left and above, plus any
                // increments
                return Math.min(tnlLeft + incrCostLeft, tnlAbove + incrCostAbove);

            case FIRST:
                if (ed2.isTokenStart(idx2)) {
                    // if we are the beginning of a token in str2, copy from
                    // tokenNormLength above and increment tokenNormalizedLength
                    return tnlAbove + incrCostAbove;
                } else {
                    // just copy from the tokenNormalizedLength to the left
                    return tnlLeft;
                }

            case MAX:
                if (!ed1.isTokenStart(idx1)) {
                    incrCostLeft = 0f;
                }
                if (idx1 != 0 && !ed2.isTokenStart(idx2)) {
                    incrCostAbove = 0f;
                }

                // take the max of tokenNormalizedLength to left and above, plus any
                // increment
                return Math.max(tnlLeft + incrCostLeft, tnlAbove + incrCostAbove);

            default:
                throw new UnsupportedOperationException("Don't know how to handle case " + edConfig.getNormType());

        }
    }

    /**
     * Computes the return value (finite or infinite) when one of the input items is
     * empty (or null), based on the proposed return value and the edit distance
     * limits, if any.
     *
     * @param retVal   the normalized length of the non-empty string
     * @param firstLen the length of the first item
     * @param compInfo per-pair comparison values
     * @return retVal if the length is not over the raw edit distance or normalized
     * edit dist limit; OVER_LIMIT otherwise
     */
    private float calcEmptyInputRetVal(float retVal, float firstLen, ComparisonInfo compInfo) {
        // retVal can only be zero if both inputs are zero-length, so bail now.
        // (This shouldn't happen with the way the code is currently structured, but
        // play it safe.)
        if (retVal == 0) {
            return retVal;
        }

        // is the raw edit limit relevant? Are we over the raw limit?
        if (compInfo.getCurrEditLimit() > 0 && retVal > compInfo.getCurrEditLimit()) {
            return OVER_LIMIT;
        }

        // is the normalized edit limit relevant?
        // IF NormType == MIN *OR*
        //    NormType == FIRST + first item is empty
        // THEN limit == 0, and we are over since second item is not also empty
        // IF NormType == MAX *OR*
        //    NormType == FIRST + first item is *not* empty,
        // THEN dist == 100%, so the only thing that matters is whether the limit
        //    is < 100%
        if (compInfo.getCurrEditNormLimit() > 0 &&
                (edConfig.getNormType() == NormType.MIN ||
                        (edConfig.getNormType() == NormType.FIRST && firstLen == 0) ||
                        compInfo.getCurrEditNormLimit() < 1)) {
            return OVER_LIMIT;
        }

        // not over any limits
        return retVal;
    }

}
