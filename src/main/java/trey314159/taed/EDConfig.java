package trey314159.taed;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

import lombok.Getter;

// EDConfig class to set up TokenAwareEditDistance
@Getter
public final class EDConfig {
    // Edit distance cost params, default values
    private float defaultLimit = 2.0f; // default edit dist limit for early termination
    private float defaultNormLimit; // default to 0; default normalized edit dist limit for
                                    // early termination
    private float insDelCost = 1.0f; // cost for insertion or deletion (abc / ac)
    private float substCost = 1.0f; // cost for substitution (abc / adc)
    private float swapCost = 1.25f; // cost for swapping two letters (abc / bac)
    private float duplicateCost = 0.05f; // cost for a duplicated letter (abc / abbc)
    private float digitChangePenalty = 0.33f; // add'l cost for changing one digit to
                    // another. Note: if digitChangePenalty is more than
                    // insDelCost, it will be cheaper to delete one digit and
                    // insert the other so the effective digitChangePenalty will
                    // be == insDelCost
    private NormType normType = NormType.MAX; // normalization method

    // Token processing params
    private char  tokenSep = ' '; // space -- expected separator between tokens
    private String tokenSepStr = Character.toString(tokenSep);
            // string version of tokenSep (calculated)
    private float tokenInitialPenalty = 0.25f; // add'l cost for changing the first
                                               // char of a token
    private float tokenSepSubstPenalty = 0.50f; // add'l cost for changing the tokenSep
                                                // char to something else
    private float tokenDeltaPenalty = 0.25f; // cost for changing the number of tokens,
                                             // per token
    private float spaceOnlyCost = 0.1f; // cost for inserting/deleting only spaces
    private boolean perTokenLimit = true; // should limits be enforced on each token?

    // Default tokenization params
    private Locale locale = Locale.ENGLISH; // locale for lowercasing; null for none
    private String tokenSplit = "[\\p{Z}\\p{P}\\p{S}]+";
        // regex to split tokens: all space, punct, and symbols, which may be too
        // aggressive in some cases; if \p{S} is removed, add + instead
    private Function<String, String[]> tokenizer; // tokenizer for strings to be
                                     // compared, will be constructed if needed.

    private EDConfig() {}

    public static final class Builder {
        private EDConfig theConfig;

        Builder() {
            this.theConfig = new EDConfig();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public EDConfig build() {
            EDConfig readyConfig = this.theConfig;

            // reset the builder's config so the current config cannot be
            // modified, but the builder instance can be re-used
            this.theConfig = new EDConfig();

            // build a default tokenizer if none has been provided, using the
            // tokenSplit regex
            if (readyConfig.tokenizer == null) {
                String tokenTrim = "^" + Pattern.quote(readyConfig.tokenSplit)
                    + "|" + readyConfig.tokenSplit + "$";
                readyConfig.tokenizer = makeTokenizer(readyConfig.locale,
                    tokenTrim, readyConfig.tokenSplit);
            }

            return readyConfig;
        }

        public Builder setDefaultLimit(final float defaultLimit) {
            this.theConfig.defaultLimit = defaultLimit;
            return this;
        }

        public Builder setDefaultNormLimit(final float defaultNormLimit) {
            this.theConfig.defaultNormLimit = defaultNormLimit;
            return this;
        }

        public Builder setInsDelCost(final float insDelCost) {
            this.theConfig.insDelCost = insDelCost;
            return this;
        }

        public Builder setSubstCost(final float substCost) {
            this.theConfig.substCost = substCost;
            return this;
        }

        public Builder setSwapCost(final float swapCost) {
            this.theConfig.swapCost = swapCost;
            return this;
        }

        public Builder setDuplicateCost(final float duplicateCost) {
            this.theConfig.duplicateCost = duplicateCost;
            return this;
        }

        public Builder setDigitChangePenalty(final float digitChangePenalty) {
            this.theConfig.digitChangePenalty = digitChangePenalty;
            return this;
        }

        public Builder setTokenSep(final char tokenSep) {
            this.theConfig.tokenSep = tokenSep;
            this.theConfig.tokenSepStr = Character.toString(tokenSep);
            return this;
        }

        public Builder setTokenInitialPenalty(final float tokenInitialPenalty) {
            this.theConfig.tokenInitialPenalty = tokenInitialPenalty;
            return this;
        }

        public Builder setTokenSepSubstPenalty(final float tokenSepSubstPenalty) {
            this.theConfig.tokenSepSubstPenalty = tokenSepSubstPenalty;
            return this;
        }

        public Builder setTokenDeltaPenalty(final float tokenDeltaPenalty) {
            this.theConfig.tokenDeltaPenalty = tokenDeltaPenalty;
            return this;
        }

        public Builder setSpaceOnlyCost(final float spaceOnlyCost) {
            this.theConfig.spaceOnlyCost = spaceOnlyCost;
            return this;
        }

        public Builder setPerTokenLimit(final boolean perTokenLimit) {
            this.theConfig.perTokenLimit = perTokenLimit;
            return this;
        }

        public Builder setLocale(final Locale locale) {
            this.theConfig.locale = locale;
            return this;
        }

        public Builder setTokenSplit(final String tokenSplit) {
            this.theConfig.tokenSplit = tokenSplit;
            return this;
        }

        public Builder setTokenizer(final Function<String, String[]> tokenizer) {
            this.theConfig.tokenizer = tokenizer;
            return this;
        }

        public Builder setNormType(final NormType normType) {
            this.theConfig.normType = normType;
            return this;
        }

        /**
          * Create a simple tokenizer function for alphabetic scripts that uses
          * the given locale, tokenSplit regex, and tokenTrim regex.
          *
          * This is a simple tokenizer that works with scripts with separate words
          * (e.g., English, Russian, Hindi, Hebrew, etc., but not CJK, Thai, etc.)
          *
          * @param lowercaseLocale locale for lowercasing
          * @param tokenTrimRegex regex for trimming the beginning and end of the
          *     string before tokenizing
          * @param tokenSplitRegex regex for splitting a string into tokens
          *
          * @return the tokenizer function
          */
        private static Function<String, String[]> makeTokenizer(Locale lowercaseLocale,
                String tokenTrimRegex, String tokenSplitRegex) {
            return s -> {
                if (lowercaseLocale != null) {
                    s = s.toLowerCase(lowercaseLocale);
                }
                s = s.replaceAll(tokenTrimRegex, "");

                return s.split(tokenSplitRegex);
            };
        }
    }
}
