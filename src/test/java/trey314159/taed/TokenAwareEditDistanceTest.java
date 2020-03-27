package trey314159.taed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.function.Function;

import org.junit.Test;
import trey314159.taed.EDConfig;
import trey314159.taed.NormType;
import trey314159.taed.TokenAwareEditDistance;

public class TokenAwareEditDistanceTest {

    // values are usually in increments of 0.01, so this seems like a reasonable
    // tolerance for float equivalence
    float delta = 0.00001f;

    private static final float OVER_LIMIT = Float.POSITIVE_INFINITY;

    // edit distance calculator based on default settings
    TokenAwareEditDistance defaultED =
        new TokenAwareEditDistance(EDConfig.Builder.newInstance().build());

    // edit distance calculator that only tokenizes on spaces
    TokenAwareEditDistance spaceTokenED =
            new TokenAwareEditDistance(
        EDConfig.Builder.newInstance()
            .setTokenSplit(" ")
            .build());

    // edit distance calculator with very high edit limit (so it returns the actual
    // edit distance without early termination
    TokenAwareEditDistance highLimitED = new TokenAwareEditDistance(
        EDConfig.Builder.newInstance()
            .setDefaultLimit(100f)
            .setDefaultNormLimit(5.0f)
            .build());

    private void checkEDCalc(String s1, String s2, TokenAwareEditDistance ed, float result) {
        // unless normType == FIRST, results should be symmetric
        assertEquals(result, ed.calcEditDistance(s1, s2), delta);
        assertEquals(result, ed.calcEditDistance(s2, s1), delta);
    }

    private void checkEDCalcWithLim(String s1, String s2, TokenAwareEditDistance ed,
            float result, float edLim, float edNormLim) {
        // unless normType == FIRST, results should be symmetric
        assertEquals(result, ed.calcEditDistance(s1, s2, edLim, edNormLim), delta);
        assertEquals(result, ed.calcEditDistance(s2, s1, edLim, edNormLim), delta);
    }

    private void checkEDCalcAsym(String s1, String s2, TokenAwareEditDistance ed,
            float result, float reversedResult) {
        // if normType == FIRST, results are not necessarily symmetric
        assertEquals(result, ed.calcEditDistance(s1, s2), delta);
        assertEquals(reversedResult, ed.calcEditDistance(s2, s1), delta);
    }

    private void checkEDCalcWithLimAsym(String s1, String s2, TokenAwareEditDistance ed,
            float result, float reversedResult, float edLim, float edNormLim) {
        // unless normType == FIRST, results should be symmetric
        assertEquals(result, ed.calcEditDistance(s1, s2, edLim, edNormLim), delta);
        assertEquals(reversedResult, ed.calcEditDistance(s2, s1, edLim, edNormLim), delta);
    }

    // one ED calculator, one result, lots of pairs of strings that should all get
    // the same result
    private void checkEDCalcArray(TokenAwareEditDistance ed, float result, String[] strings) {
        assertTrue(strings.length % 2 == 0);

        // unless normType == FIRST, results should be symmetric
        for (int i = 0; i < strings.length; i += 2) {
            assertEquals(result, ed.calcEditDistance(strings[i], strings[i + 1]), delta);
            assertEquals(result, ed.calcEditDistance(strings[i + 1], strings[i]), delta);
        }
    }

    @Test
    public void testEmptyNullOrEqual() {

        checkEDCalcArray(defaultED, 0.0f,
            new String[] {
                // should be able to handle null and empty inputs
                null, null,            null, "",              "", "",

                // quick return on equality (modulo lowercasing)
                "dog", "dog",          "DoG", "dOg"
            }
        );

        checkEDCalcArray(defaultED, OVER_LIMIT,
            new String[] {"dog", null,      "dog", ""}
        );

        // shorter strings can be under the limit wrt null/empty
        checkEDCalcArray(defaultED, 1.0f, new String[] {"a",  null,   "a",  ""});
        checkEDCalcArray(defaultED, 2.0f, new String[] {"ab", null,   "ab", ""});
    }

    @Test
    public void testUnicodeChars() {
        // test non-ASCII chars, esp. multi-byte chars

        checkEDCalcArray(defaultED, 1.0f,
            new String[] {
                // start by removing or changing the last letter in the name of
                // Wikipedia in various languages/scripts (so, edit dist == 1)

                // two bytes per char
                "Βικιπαίδεια", "Βικιπαίδει",   "Википедия", "Википедињ",
                "Вікіпедыя", "Вікіпеды",       "Վիքիպեդիա", "Վիքիպեդիչ",
                "ויקיפדיה", "ויקיפדי",             "ویکیپیدیا", "ویکیپیدی",

                // three bytes per char
                "ვიკიპედია", "ვიკიპედი",         "ውክፔዲያ", "ውክፔዲⶐ",
                "विकिपीडिया", "विकिपीडिय",            "উইকিপিডিয়া", "উইকিপিডিয়স",
                "ৱিকিপিডিয়া", "ৱিকিপিডিয়",            "ਵਿਕੀਪੀਡੀਆ", "ਵਿਕੀਪੀਡੀ",
                "વિકિપીડિયા", "વિકિપીડિય",            "ଉଇକିପିଡ଼ିଆ", "ଉଇକିପିଡ଼ିଉ",
                "விக்கிப்பீடியா", "விக்கிப்பீடிய",  "ವಿಕಿಪೀಡಿಯ", "ವಿಕಿಪೀಡಿ",
                "വിക്കിപീഡിയ", "വിക്കിപീഡി",       "විකිපීඩියා", "විකිපීඩිය",
                "วิกิพีเดีย", "วิกิพีเดี",                "ວິກິພີເດຍ", "ວິກິພີເດະ",
                "ལྦེ་ཁེ་རིག་མཛོད", "ལྦེ་ཁེ་རིག་མཛོ",             "ဝီကီပီးဒီးယား", "ဝီကီပီးဒီးယာ",
                "វិគីភីឌា", "វិគីភីឌ",             "ᱣᱤᱠᱤᱯᱤᱰᱤᱭᱟ", "ᱣᱤᱠᱤᱯᱤᱰᱤᱭᱰ",
                "ᏫᎩᏇᏗᏯ", "ᏫᎩᏇᏗ",              "ᐅᐃᑭᐱᑎᐊ", "ᐅᐃᑭᐱᑎᖐ",
                "위키백과", "위키백",                 "ウィキペディア", "ウィキペディニ",
                "維基大典", "維基大",                 "維基百科", "維基百",
                "维基百科", "维基百",

                // four bytes per character
                "𐍅𐌹𐌺𐌹𐍀𐌰𐌹𐌳𐌾𐌰", "𐍅𐌹𐌺𐌹𐍀𐌰𐌹𐌳𐌾",          "𐍅𐌹𐌺𐌹𐍀𐌰𐌹𐌳𐌾𐌰", "𐍅𐌹𐌺𐌹𐍀𐌰𐌹𐌳𐌾𐌸",

                // test various other 32-bit unicode character strings with last
                // character missing or changed (so, edit dist == 1)
                "𝐀𝐁𝐂𝐃𝐄", "𝐀𝐁𝐂𝐃",                  "𐤀𐤁𐤂𐤃𐤄", "𐤀𐤁𐤂𐤃",
                "𐊀𐊁𐊂𐊃𐊄", "𐊀𐊁𐊂𐊃",                 "𐊠𐊡𐊢𐊣𐊤", "𐊠𐊡𐊢𐊣𐋋",
                "𐐀𐐁𐐂𐐃𐐄", "𐐀𐐁𐐂𐐃",                 "𐀀𐀁𐀂𐀃𐀄", "𐀀𐀁𐀂𐀃𐁃",
                "𐎀𐎁𐎂𐎃𐎄", "𐎀𐎁𐎂𐎃",                "你再冤卉周", "你再冤卉",
                "𐋠𐋡𐋢𐋣𐋤", "𐋠𐋡𐋢𐋣𐋷"
            }
        );

        checkEDCalcArray(spaceTokenED, 1.0f,
            new String[] {
                // some of these characters are treated as symbols or punctutation,
                // so we need a special space-based tokenizer; four-byte characters
                // and IPA
                "🌓🌔🌕🌖🌗", "🌓🌔🌕🌖",          "😀😁😂😃😄", "😀😁😂😃",
                "🚀🚁🚂🚃🚄", "🚀🚁🚂🚃",          "🠀🠁🠂🠃🠄", "🠀🠁🠂🠃",
                "ˌwɪkiˈpiːdi.ə", "ˌwɪkiˈpiːdi.", "𐇐𐇑𐇒𐇓𐇔", "𐇐𐇑𐇒𐇓"
            }
        );
    }

    @Test
    public void testTokenization() {
        ////////////////////
        // Test basic built-in tokenization
        checkEDCalc("This (string) has PUNCTUATION!",
                    "this...string.has-Punctuation();", defaultED, 0.0f);

        ////////////////////
        // silly custom external tokenizer does soundex-like compression and
        // splits on space
        Function<String, String[]> myTokenizer = s -> {
                s = s.replaceAll("(?i)[aeiouhwy]", "a");
                s = s.replaceAll("(?i)[bfpv]", "b");
                s = s.replaceAll("(?i)[cgjkqsxz]", "c");
                s = s.replaceAll("(?i)[td]", "d");
                s = s.replaceAll("(?i)[l]", "l");
                s = s.replaceAll("(?i)[mn]", "m");
                s = s.replaceAll("(?i)[r]", "r");
                s = s.replaceAll("(.)\\1+", "$1");
                return s.split(" ");
                };

        // no-limit tokenizer that uses soundex-like tokenizer
        TokenAwareEditDistance soundexTokenED =
                new TokenAwareEditDistance(
            EDConfig.Builder.newInstance()
                .setTokenizer(myTokenizer)
                .build());

        // "balcam"
        checkEDCalc("Paulson", "Balkan", soundexTokenED, 0.0f);
        checkEDCalc("Paulson", "Balkan", highLimitED, 4.25f);

        // "cabama armar"
        checkEDCalc("Giovanni Warner", "Johbany Armoir", soundexTokenED, 0.0f);
        checkEDCalc("Giovanni Warner", "Johbany Armoir", highLimitED, 9.55f);

        // "babam" vs "bacam"
        checkEDCalc("boffin", "vacuum", soundexTokenED, 1.0f);
        checkEDCalc("boffin", "vacuum", highLimitED, 5.35f);

        ////////////////////
        // use a Turkish locale such that İ/i and I/ı are the relevant
        // upper-/lowercase pairs
        TokenAwareEditDistance turkishLocaleED =
                new TokenAwareEditDistance(
            EDConfig.Builder.newInstance()
                .setLocale(Locale.forLanguageTag("tr"))
                .build());

        // separate i's with x's so duplicates don't come into it
        checkEDCalc("ıxIxİxi", "ıxıxixi", turkishLocaleED, 0.0f);
        checkEDCalc("ıxIxİxi", "ıxıxixi", highLimitED, 2.0f);
        checkEDCalc("Istanbul", "istanbul", turkishLocaleED, 1.25f);
        checkEDCalc("Istanbul", "istanbul", highLimitED, 0.0f);

        ////////////////////
        // use a null locale and there will be no lowercasing
        TokenAwareEditDistance nullLocaleED =
                new TokenAwareEditDistance(
            EDConfig.Builder.newInstance()
                .setLocale(null)
                .build());
        checkEDCalc("Istanbul", "istanbul", nullLocaleED, 1.25f);
        checkEDCalc("istanbul", "istanbul", nullLocaleED, 0.0f);
        checkEDCalc("camelCase", "camelcase", nullLocaleED, 1.0f);

        ////////////////////
        // set the token split regex to split strings on vowels
        TokenAwareEditDistance vowelSplitED =
                new TokenAwareEditDistance(
            EDConfig.Builder.newInstance()
                .setTokenSplit("[aeiouy]+")
                .build());

        // use pipe instead of space as the tokenSep and things change!
        TokenAwareEditDistance vowelSplitPipeSepED =
                new TokenAwareEditDistance(
            EDConfig.Builder.newInstance()
                .setTokenSplit("[aeiouy]+")
                .setTokenSep('|')
                .build());

        checkEDCalc("pilomotor", "polymeter", vowelSplitED, 0.0f);
        checkEDCalc("pilomotor", "polymeter", highLimitED, 4.0f);
        // vowelSplitED generates c,p,t,l and c,p t,l as tokens, but space is the
        // tokenSep, so it works out that they are the same.
        checkEDCalc("capital", "cup toil", vowelSplitED, 0.0f);
        // With | as the tokenSep, "p t" remains a token, and the edit distance is
        // greater than zero.
        checkEDCalc("capital", "cup toil", vowelSplitPipeSepED, 1.75f);
        checkEDCalc("capital", "cup toil", highLimitED, 4.75f);

        ////////////////////
        // set the token separator to •
        TokenAwareEditDistance dotSepED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setTokenSep('•')
                .build());

        // default split includes spaces and •, so this shouldn't matter
        checkEDCalc("abc def", "abc•def", dotSepED, 0.0f);
        checkEDCalc("abc def", "abc•def", highLimitED, 0.0f);
        checkEDCalc("abc def", "abc def", dotSepED, 0.0f);
        checkEDCalc("abc def", "abc def", highLimitED, 0.0f);
        checkEDCalc("abc•def", "abc•def", dotSepED, 0.0f);
        checkEDCalc("abc•def", "abc•def", highLimitED, 0.0f);

        ////////////////////
        // set the token separator to • AND only split on •
        TokenAwareEditDistance dotSepDotSplitED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setTokenSplit("[•]")
                .setTokenSep('•')
                .build());

        checkEDCalc("abc def", "abc•def", dotSepDotSplitED, 1.75f);
        checkEDCalc("abc def", "abc•def", highLimitED, 0.0f);
    }

    @Test
    public void testEditLimits() {
        TokenAwareEditDistance loLimHiNormLimED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setDefaultLimit(1.0f) // only one edit allowed
                .setDefaultNormLimit(5.0f) // 500% limit
                .build());

        TokenAwareEditDistance hiLimLoNormLimED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setDefaultLimit(10f) // ten edits allowed
                .setDefaultNormLimit(0.25f) // 25% limit
                .build());

        // 2 is over the low limit of 1 -> infinity
        checkEDCalc("abcdefghij", "acefghij", loLimHiNormLimED, OVER_LIMIT);
        checkEDCalc("abcdefghij", "acefghij", hiLimLoNormLimED, 2.0f);

        // 1 of 2 is over the low *norm* limit of 25%
        checkEDCalc("ab", "ad", loLimHiNormLimED, 1.0f);
        checkEDCalc("ab", "ad", hiLimLoNormLimED, OVER_LIMIT);

        // do some random checks with various combinations of limits with final
        // result or early termination
        checkEDCalcWithLim("abcde", "aghij", defaultED, 4.0f, 0.0f, 0.0f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, 4.0f, 4.0f, 0.0f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, 4.0f, 0.0f, 0.80f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, 4.0f, 4.0f, 0.80f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, OVER_LIMIT, 1.0f, 0.0f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, OVER_LIMIT, 0.0f, 0.6f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, OVER_LIMIT, 1.0f, 0.6f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, OVER_LIMIT, 5.0f, 0.6f);
        checkEDCalcWithLim("abcde", "aghij", defaultED, OVER_LIMIT, 1.0f, 1.2f);
    }

    @Test
    public void testPerTokenLimits() {
        TokenAwareEditDistance noPerTokLimitsED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setDefaultNormLimit(0.25f) // 25% limit, per string
                .setPerTokenLimit(false)
                .build());

        TokenAwareEditDistance perTokLimitsED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setDefaultNormLimit(0.25f) // 25% limit, per string, and per token
                .build());

        // an vs a is 50% change and over the limit on a per-token basis
        checkEDCalc("an dog", "a dog", noPerTokLimitsED, 1.0f);
        checkEDCalc("an dog", "a dog", perTokLimitsED, OVER_LIMIT);
    }

    @Test
    public void testCustomCosts() {
        // set customized edit costs and test them one-by-one
        // values are assigned so they are easy to detect
        // set limits very high in case we want to do something ridiculous
        TokenAwareEditDistance customCostED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setInsDelCost(1.1f)
                .setSubstCost(1.2f)
                .setSwapCost(1.3f)
                .setDuplicateCost(0.5f)
                .setDigitChangePenalty(0.07f)
                .setTokenInitialPenalty(0.24f)
                .setTokenSepSubstPenalty(0.36f)
                .setTokenDeltaPenalty(0.4f)
                .setSpaceOnlyCost(0.5f)
                .setDefaultLimit(100f)
                .setDefaultNormLimit(5.0f)
                .build());

        // custom insert/delete cost: 1.1 x2 vs 1.0 x2
        checkEDCalc("abcde", "ace", customCostED, 2.2f);
        checkEDCalc("abcde", "ace", highLimitED, 2.0f);

        // custom substitution cost: 1.2 vs 1.0
        checkEDCalc("abcde", "abxde", customCostED, 1.2f);
        checkEDCalc("abcde", "abxde", highLimitED, 1.0f);

        // no digit penalty because only one char is a number
        checkEDCalc("abcde", "ab7de", customCostED, 1.2f);
        checkEDCalc("abcde", "ab7de", highLimitED, 1.0f);

        // custom swap cost: 1.3 vs 1.25
        checkEDCalc("abcde", "abdce", customCostED, 1.3f);
        checkEDCalc("abcde", "abdce", highLimitED, 1.25f);

        // custom duplicate cost: 0.5 x5 vs 0.05 x5
        checkEDCalc("aabbccddee", "abcde", customCostED, 2.5f);
        checkEDCalc("aabbccddee", "abcde", highLimitED, 0.25f);

        // custom digit penalty: 1.3 + 0.07 vs 1.25 + 0.33
        checkEDCalc("12345", "12435", customCostED, 1.37f);
        checkEDCalc("12345", "12435", highLimitED, 1.58f);

        // custom digit penalty: 1.2 + 0.07 vs 1 + 0.33
        checkEDCalc("12345", "12045", customCostED, 1.27f);
        checkEDCalc("12345", "12045", highLimitED, 1.33f);

        // custom token-initial penalty: 1.2 + 0.24 vs 1 + 0.25
        checkEDCalc("abcde", "zbcde", customCostED, 1.44f);
        checkEDCalc("abcde", "zbcde", highLimitED, 1.25f);

        // custom tokenSep substitution penalty & token delta penalty:
        // 1.2 + 0.36 + 0.4 vs 1 + 0.50 + 0.25
        checkEDCalc("abcde", "ab de", customCostED, 1.96f);
        checkEDCalc("abcde", "ab de", highLimitED, 1.75f);

        // custom space-only cost: 0.5 x2 vs 0.1 x2
        checkEDCalc("ab cdef", "abcd ef", customCostED, 1.0f);
        checkEDCalc("ab cdef", "abcd ef", highLimitED, 0.2f);

        // If the swap cost is LESS than the insert cost, the min cost per row can go
        // down from one row to the next, so we have to be careful not to terminate
        // early.
        TokenAwareEditDistance swapLessThanInsCostED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setInsDelCost(1.0f)
                .setSwapCost(0.75f)
                .setDefaultLimit(0.99f)
                .build());

        TokenAwareEditDistance swapMoreThanInsCostED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setInsDelCost(1.0f)
                .setSwapCost(1.25f)
                .setDefaultLimit(0.99f)
                .build());

        checkEDCalc("abc", "acb", swapLessThanInsCostED, 0.75f);
        checkEDCalc("abc", "acb", swapMoreThanInsCostED, OVER_LIMIT);
    }

    @Test
    public void testNormTypes() {
        TokenAwareEditDistance maxNormED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setNormType(NormType.MAX)
                .setDefaultNormLimit(0.22f) // 22% edit limit
                .build());

        TokenAwareEditDistance minNormED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setNormType(NormType.MIN)
                .setDefaultNormLimit(0.22f) // 22% edit limit
                .build());

        TokenAwareEditDistance firstNormED =
            new TokenAwareEditDistance(EDConfig.Builder.newInstance()
                .setNormType(NormType.FIRST)
                .setDefaultNormLimit(0.22f) // 22% edit limit
                .build());

        // 22% of 5 (max) is 1.10, so edit dist of 1 is okay
        checkEDCalc("abcde", "abcd", maxNormED, 1.0f);

        // 22% of 4 (min) is 0.88, so edit dist of 1 is too much
        checkEDCalc("abcde", "abcd", minNormED, OVER_LIMIT);
        // 22% of 5 (min) is 1.10, so edit dist of 1 okay
        checkEDCalc("abcde", "abcdef", minNormED, 1.0f);

        // NormType.First takes the limit from the first string to be compared
        checkEDCalcAsym("abcde", "abcd", firstNormED, 1.0f, OVER_LIMIT);

        ////////////////
        // NormType interacts with empty/null strings, so let's check that
        // alternating empty vs null strings

        // longer string vs empty/null is too many edits
        checkEDCalc("abcde", "", maxNormED, OVER_LIMIT);
        checkEDCalc("abcde", "", minNormED, OVER_LIMIT);
        checkEDCalc("abcde", "", firstNormED, OVER_LIMIT);

        // shorter string vs empty/null can be okay
        // use editLim = 2, editNormLim = 0 (no limit)
        checkEDCalcWithLim("ab", "", maxNormED, 2.0f, 2.0f, 0.0f);
        checkEDCalcWithLim("ab", null, minNormED, 2.0f, 2.0f, 0.0f);
        checkEDCalcWithLim("ab", null, firstNormED, 2.0f, 2.0f, 0.0f);

        // no limits, should get the normalized length of the other one
        checkEDCalcWithLim("abcde", "", maxNormED,   5.0f, 0.0f, 0.0f);
        checkEDCalcWithLim("abcde", "", minNormED,   5.0f, 0.0f, 0.0f);
        checkEDCalcWithLim("abcde", "", firstNormED, 5.0f, 0.0f, 0.0f);

        // high edit limit, no normalized limit, should get the normalized length of
        // the other one
        checkEDCalcWithLim("abcde", null, maxNormED,   5.0f, 10.0f, 0.0f);
        checkEDCalcWithLim("abcde", null, minNormED,   5.0f, 10.0f, 0.0f);
        checkEDCalcWithLim("abcde", null, firstNormED, 5.0f, 10.0f, 0.0f);

        // no edit limit, high normalized limit, for max, this should get the
        // normalized length of the other one
        checkEDCalcWithLim("abcde", "",   maxNormED, 5.0f, 0.0f, 2.0f);

        // however, if NormType is MIN, min = 0, so norm limit == 0
        checkEDCalcWithLim("abcde", null, minNormED, OVER_LIMIT, 0.0f, 2.0f);

        // for FIRST, order determines the result, of course
        checkEDCalcWithLimAsym("abcde", "", firstNormED, 5.0f, OVER_LIMIT, 0.0f, 2.0f);
    }

    @Test
    public void testEarlyTermination() {
        // let's try to trigger various early termination criteria
        // these are somewhat implementation-dependent, but they help to exercise
        // more of the code

        checkEDCalcArray(defaultED, OVER_LIMIT,
            new String[] {
                // too many tokens: 10 vs 1 -> 0.25 x9 = 2.25, over the limit of 2
                "a b c d e f g h i j", "jihgfedcba",

                // too many different unique characters: 8 vs 5 = 1.0 x3 inserts
                "abcdefgh", "abcde",

                // unique characters are too different: no overlap = 1.0 x3 substitutions
                "abc", "def",

                // after ab vs ed, it's already too late
                "abcde", "edcba",

                // the very last cell is over the per-token limit
                "abc", "bcd",

                // the very last cell is over the per-string limit
                "xxx abc", "xxx bcd"
            }
        );
    }

    @Test
    public void testMethodOverload() {
        // regular call with default values is over the limit
        assertEquals(OVER_LIMIT,
            defaultED.calcEditDistance("abcdefg", "abecdgf"), delta);

        // call with no limits (lim = 0) returns a value
        assertEquals(3.0f, defaultED.calcEditDistance("abcdefg", "abecdgf", 0.0f, 0.0f), delta);

        // call with high limits returns a value
        assertEquals(3.0f, defaultED.calcEditDistance("abcdefg", "abecdgf", 3.0f, 0.50f), delta);
    }

}
