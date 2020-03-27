package trey314159.taed;

import lombok.Getter;

/**
  * Class to hold info about the current pair being compared.
  *
  * This class holds widely useful, computed values that are specific to the
  * current pair of strings being compared, so we can pass those values around to
  * all the places they need to be in a relatively transparent way.
  */
@Getter
final class ComparisonInfo {
    // internal-only per-comparison computed values
    private final float currEditLimit; // raw edit limit for the current pair of strings
    private final float currEditNormLimit; // % edit limit for the current pair of strings
    private final boolean spacelessEquals; // do the current strings have spaceless equality?

    ComparisonInfo(final float currEditLim, final float currEditNormLim,
                   final boolean spacelessEq) {
        this.currEditLimit = currEditLim;
        this.currEditNormLimit = currEditNormLim;
        this.spacelessEquals = spacelessEq;
    }
}
