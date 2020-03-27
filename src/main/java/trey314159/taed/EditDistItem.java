package trey314159.taed;

import java.util.HashSet;
import java.util.regex.Pattern;

import lombok.Getter;

/**
  * Class to convert a string into an EditDistItem for edit distance
  * computation.
  *
  * To be compatible with 32-bit Unicode characters, we convert strings to
  * an array of ints (representing codepoints). We also calculate various
  * useful stats on the string and compute variants of the string.
  *
  * Future optimization: Consider making this class public and creating a version
  * of computeEditDistance() that takes two EditDistItems, or an EditDistItem and
  * a string, so that when comparing one string against many others the
  * EditDistItem doesn't need to be repeatedly built.
  */
@Getter
final class EditDistItem {
    final int[] text;             // the string as Unicode codepoints
    final boolean[] isDigit;      // for each codepoint, is it a digit?
    final String spacelessText;   // all instances of tokenSep removed for quick comparison
    final float normLength;       // # of codepoints, with duplicates discounted
    final HashSet<Integer> uniqueCodepoints; // hash of unique codepoints
    final int tokenCount;         // number of tokens

    final EDConfig edItemConfig;  // the config for the calling TokenAwareEditDistance

    /**
      * Constructor for EditDistItem
      *
      * Tokenize string, count tokens, create codepoint array and isDigit
      * array, find normalized codepoint length, create spaceless version
      * of the text, and a hash set of unique codepoints.
      *
      * @param s the string to convert to EditDistItem
      * @param edConfig an edit dist config object with all the relevant costs
      *     and tokenizing values.
      */
    EditDistItem(final String s, EDConfig edConfig) {
        edItemConfig = edConfig;
        if (s == null || s.isEmpty()) {
            text = new int[0];
            tokenCount = 0;
            spacelessText = "";
            isDigit = new boolean[0];
            normLength = 0;
            uniqueCodepoints = new HashSet<>();
        } else {
            String[] tokens = edItemConfig.getTokenizer().apply(s);
            tokenCount = tokens.length;
            String tokStr = String.join(edItemConfig.getTokenSepStr(), tokens).trim();

            text = tokStr.codePoints().toArray();
            spacelessText = tokStr.replaceAll(
                Pattern.quote(edItemConfig.getTokenSepStr()), "");
            isDigit = new boolean[text.length];

            // calc normLength of text and init isDigit
            float normLen = 0f;

            for (int i = 0; i < text.length; i++) {
                if (isDuplicate(i)) {
                    normLen += edItemConfig.getDuplicateCost();
                } else {
                    normLen += edItemConfig.getInsDelCost();
                }

                // Note: Character.isDigit() doesn't match everything that the
                // regex \p{N} does; it misses Ethiopic digits, some Tibetan
                // digits, "number" characters (>10) and lots of typographic
                // variants: circled, parens, full stop, etc. All those are
                // generally quite rare, though. isDigit() is fairly slow, so we
                // pre-compute; converting to String to use \p{N} is even
                // slower, alas.
                isDigit[i] = Character.isDigit(text[i]);
            }
            normLength = normLen;

            // create hash of unique codepoints
            uniqueCodepoints = new HashSet<>();
            for (int c : text) {
                uniqueCodepoints.add(c);
            }
        }
    }

    private int codepointAt(final int idx) {
        if (idx < 0) {
            return -1;
        }
        return text[idx];
    }

    /**
      * Note that the codepoint at 0 can't be a duplicate because there is no
      * codepoint before it.
      */
    boolean isDuplicate(final int idx) {
        if (idx == 0) {
            return false;
        }
        return text[idx] == text[idx - 1];
    }

    boolean isTokenSep(final int idx) {
        return text[idx] == edItemConfig.getTokenSep();
    }

    /**
      * return true if the codepoint is the the first codepoint
      *     (idx == 0) or right after a token separator, false if not.
      */
    boolean isTokenStart(final int idx) {
        if (idx == 0) {
            return true;
        }
        return text[idx - 1] == edItemConfig.getTokenSep();
    }

    /**
      * Determine whether two codepoints have been swapped.
      *
      * Let codepointAt() handle index < 0 that are out of range.
      *
      * @param idx index of relevant codepoint in this EditDistItem
      * @param otherItem the other EditDistItem to compare to
      * @param otherIdx index of relevant codepoint in the other
      *     EditDistItem
      *
      * @return true if the codepoints have been swapped, false if
      *     not, or if anything is out of range.
      */
    boolean isSwapped(final int idx, final EditDistItem otherItem,
            final int otherIdx) {
        return this.codepointAt(idx - 1) == otherItem.codepointAt(otherIdx)
            && this.codepointAt(idx) == otherItem.codepointAt(otherIdx - 1);
    }

    /**
      * Caclulate the minimum edit distance based on unique codepoints.
      *
      * Used for early termination of edit distance calculation. Calculate
      * the minimum possible cost between two items based on the raw number of
      * unique codepoints in each item (excesses in either direction incur
      * insDelCost), and the non-overlap in unique codepoint sets
      * (non-matching leftovers incur substCost)
      *
      * @param otherItem the other EditDistItem to compare to
      *
      * @return the minimum cost based on unique codepoint info
      */
    float calcUniqCharMinCost(final EditDistItem otherItem) {
        // find the *size* of the intersection of the unique codepoints
        float charIntersectionCount = 0;
        for (int cp : this.uniqueCodepoints) {
            if (otherItem.uniqueCodepoints.contains(cp)) {
                charIntersectionCount++;
            }
        }

        /* Every unique character has to be inserted, deleted, or changed so
         * diffs in raw counts indicate necessary insertions (insDelCost).
         * remaining unique characters must at least be substituted
         * (substCost).
         *
         * e.g., best case for the unique set of letters {a,b,c,d} vs {a,f,g} is:
         * 1 stays the same (a), plus 2 substitutions (b->f, c->g), plus 1 delete
         * (d). [Note that the b/c/d vs f/g alignment is arbitrary]
         */

        // minimum insertions is difference in set sizes. e.g., {a,b,c,d} has 4,
        // but {a,f,g} has 3, so there has to be one insertion.
        float minInsertions = Math.abs((float)this.uniqueCodepoints.size() -
                    otherItem.uniqueCodepoints.size());
        minInsertions *= edItemConfig.getInsDelCost(); // multiply by cost per insertion

        // minimum substitutions is smaller set size, minus size of overlap.
        // e.g., {a,b,c,d} (4) and {a,f,g} (3) overlap by {a} (1), so the two
        // (3-1) remaining elements {f,g} could at best be substitutions.
        float minSubstitutions = Math.min(this.uniqueCodepoints.size(),
                    otherItem.uniqueCodepoints.size()) -
                    charIntersectionCount;
        minSubstitutions *= edItemConfig.getSubstCost(); // multiply by cost per substitution

        return minInsertions + minSubstitutions;
    }

    /**
      * Caclulate penalty for differing number of tokens.
      *
      * Used for early termination of edit distance calculation. Calculate
      * the penalty incurred for having differing number of tokens, taking
      * into account the discount (i.e., no penalty) for spaceless equality
      * (in which case we just want to find the spaces).
      *
      * @param otherItem the other EditDistItem to compare to
      * @param theCompInfo the pair-specific computed info
      *
      * @return the token difference penalty
      */
    float calcTokenDiffPenalty(final EditDistItem otherItem,
            ComparisonInfo theCompInfo) {
        return theCompInfo.isSpacelessEquals() ? 0 : // no penalty for spaceless equality
            Math.abs((this.tokenCount - otherItem.tokenCount) *
                edItemConfig.getTokenDeltaPenalty());
    }

    /**
      * Caclulate cost for substituting codepoints.
      *
      * Calculate cost for substitution; if codepoints are equal, no cost!
      * otherwise, check for token start and digit penalties.
      *
      * @param idx index of relevant codepoint in this EditDistItem
      * @param otherItem the other EditDistItem to compare to
      * @param otherIdx index of relevant codepoint in the other
      *     EditDistItem
      *
      * @return the final substitution cost
      */
    float calcSubstCost(final int idx, final EditDistItem otherItem,
            final int otherIdx) {
        float cost = 0f;

        if (this.codepointAt(idx) != otherItem.codepointAt(otherIdx)) {
            // if not equal, add cost for substitution
            cost += edItemConfig.getSubstCost();

            // if we are at the start of either token, add penalty
            if (this.isTokenStart(idx) || otherItem.isTokenStart(otherIdx)) {
                cost += edItemConfig.getTokenInitialPenalty();
            }

            // if we are changing a token sep character, add penalty
            if (this.isTokenSep(idx) || otherItem.isTokenSep(otherIdx)) {
                cost += edItemConfig.getTokenSepSubstPenalty();
            }

            // add penalty for changing numbers
            if (this.isDigit[idx] && otherItem.isDigit[otherIdx]) {
                cost += edItemConfig.getDigitChangePenalty();
            }
        }

        return cost;
    }

    /**
      * Caclulate cost for swapping codepoints.
      *
      * Calculate cost for swap; check for digit penalties.
      *
      * @param idx index of relevant codepoint in this EditDistItem
      * @param otherItem the other EditDistItem to compare to
      * @param otherIdx index of relevant codepoint in the other
      *     EditDistItem
      *
      * @return the final swap cost
      */
     float calcSwapCost(final int idx, final EditDistItem otherItem,
            final int otherIdx) {
        if (this.isDigit[idx] && otherItem.isDigit[otherIdx]) {
            return edItemConfig.getSwapCost() + edItemConfig.getDigitChangePenalty();
        }

        return edItemConfig.getSwapCost();
    }

    /**
      * Caclulate cost for inserting or deleting a codepoint.
      *
      * Handle all possible variations of inserting or deleting a codepoint,
      * including spaceless equality discount, token start and digit
      * penalties, and duplicated character discount.
      *
      * @param idx index of relevant codepoint in this EditDistItem
      * @param theCompInfo the pair-specific computed info
      *
      * @return the final insert or delete cost
      */
     float calcInsDelCost(final int idx, final ComparisonInfo theCompInfo) {
        // if spaceless-equal then only incur discounted cost
        if (theCompInfo.isSpacelessEquals() && this.isTokenSep(idx)) {
            return edItemConfig.getSpaceOnlyCost();
        }

        // extra penalty for changing the first codepoint in a token or
        // inderting/deleting a digit.
        float penalty = 0f;
        if (this.isTokenStart(idx)) {
            penalty += edItemConfig.getTokenInitialPenalty();
        }
        if (this.isDigit[idx]) {
            penalty += edItemConfig.getDigitChangePenalty();
        }

        // give duplicate discount, or full insert/delete cost
        if (this.isDuplicate(idx)) {
            return edItemConfig.getDuplicateCost() + penalty;
        }

        return edItemConfig.getInsDelCost() + penalty;
    }
}
