# Token-Aware Edit Distance

March 2020

Algorithm by [Trey Jones](https://meta.wikimedia.org/wiki/User:TJones_%28WMF%29).  
Code improvements by [Zbyszko
Papierski](https://meta.wikimedia.org/wiki/User:ZPapierski_%28WMF%29).  
Post-fork documentation and code updates and general code diminishments also by [Trey
Jones](https://github.com/Trey314159).

*Forked from the
Wikimedia [Search Platform](https://www.mediawiki.org/wiki/Wikimedia_Search_Platform)
team's [Glent](https://gerrit.wikimedia.org/r/#/admin/projects/search/glent) project*

### Changes

* Modified this ReadMe to be less Glent-oriented.
* TODO: add command line driver
* TODO: add full table output

## Background & Rationale

[Edit distance](https://en.wikipedia.org/wiki/Edit_distance) is a commonly used
metric for string similarity. [Levenshtein
distance](https://en.wikipedia.org/wiki/Levenshtein_distance) measures the number of
insertions, deletions, and substitutions required to transform one string into
another. [Damerau–Levenshtein
distance](https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance) also
allows for transpositions (swaps).

[Glent](https://gerrit.wikimedia.org/r/#/admin/projects/search/glent) is a
project/repository intended to improve search suggestions made on-wiki (commonly
called "Did you mean" or "DYM" suggestions). After reviewing the suggestions made by
Glent's Method 1/"similar queries", a number of [common problems among poor-quality
suggestions](https://www.mediawiki.org/wiki/User:TJones_%28WMF%29/Notes/Analysis_of_DYM_Method_1#Method_1_Patterns_and_Anti-Patterns)
became apparent. Token-Aware Edit Distance is intended to address those concerns
related to the edit distance calculation, and to generate a more fine-grained score.

The algorithm may also be useful outside the context of Glent.

## Issues Addressed

**Token-Aware Edit Limits**  
Using edit distance on short strings of a few words can lead to some undesirable
behavior.

A default edit limit for a word of, say, 2 edits is reasonable, with a lower limit
for very short words. This makes perfect sense when applied to individual
words—allowing two edits to *IQ* would allow it to change into any other two-letter
word! With much longer texts, like a paragraph or more, changing two letters—even if
it completely changes a single word—doesn't really impact the overall similarity of
the two paragraphs.

Strings made up of a handful of words, though, are a different matter. Allowing two
edits to *bar band* can result in *a band,* which has a fairly different meaning.
Normally we wouldn't allow a three-letter word like *bar* to have two edits, so we
should block it from happening to the word *bar* as part of a longer string, too.
Hence, the "token-aware" part of Token-Aware Edit Distance, which allows us to apply
edit limits at the token level rather than the string level.

Use `setPerTokenLimit(boolean perTokenLimit)` to configure whether edit limits are
applied per-token. By default, `perTokenLimit` is `true`.

**Normalized Edit Limits (Percentages)**  
To simplify the configuration of variable edit limits, rather than specify specific
cut-offs (e.g., zero edits for one or two letters, one edit for three or four
letters, two edits for five or more letters), it makes more sense to specify the
proportional limit as a fraction of the total length. This also allows for very small
edits, like duplicate letters, even in very short strings. So changing *od* to *odd*
is not one edit, but perhaps only 0.1 edits, which is only 5% of the length of the
original two-letter word. (Note that this kind of math may also affect the effective
length of a word; if inserting or deleting a duplicate letter only costs 0.1 edits,
then *odd* is arguably only 2.1 letters long.)

Normalized edit limits work in conjunction with the hard limit on edits, and
whichever is lower is used. For example, if you have the normalized edit limit set to
0.4 (i.e., 40%) and the plain edit limit set to 2, then a string or token of length 8
would only be allowed 2 edits (i.e., `min(2, 8*0.4)`).

String pairs that are over either limit are assigned an edit distance of
`Float.POSITIVE_INFINITY`.

Use `setDefaultLimit(float defaultLimit)` to configure the maximum number of edits
for the whole string. By default, `defaultLimit` is `2.0f`. Set `defaultLimit` to
`0f` for no hard limit on edits per string.

`defaultLimit` is also used for early termination of the edit distance computation.
For example, if the limit is 2 and we can compute that a minimum of 3 edits are
necessary, we can terminate early (returning a distance of
`Float.POSITIVE_INFINITY`).

Use `setDefaultNormLimit(float defaultNormLimit)` to configure the maximum number of
edits for the whole string. By default, `defaultNormLimit` is `0f`. This value is
interpreted as a percentage, so `0.4` means that a string or token (see
`perTokenLimit` above) can be changed by up to 40%. Set `defaultNormLimit` to `0f`
for no proportional per-string or per-token limit.

If both `defaultLimit` and `defaultNormLimit` are set to `0f`, then the full edit
distance will be computed and returned.

Note that in some cases setting `defaultNormLimit` can result in a larger total edit
distance, because some edit paths are blocked. This is usually a good thing.

**Length Normalization Type**  
When we talk about lower edit distance limits for shorter strings, words, or tokens,
sometimes our edits can cross the boundary between what is or is not "shorter". If,
say, we allow two edits for five-letter words, but only one edit for three-letter
words, how do we handle *brand* being edited (with two deletions) to *ban,* and is
that different from inserting letters in the opposite direction?

To address this, we can specify that edits are counted against the longer string or
token, against the shorter string or token, or against the *first* string or token.
Specifying the first string or token as the one to measure against allows us to skew
the limit based on the "original" string (e.g., user input). Thus, *brand* could be
shortened to *ban* (two edits to a five-letter word) but *ban* could be blocked from
lengthening to *brand* (two edits to a three-letter word). Which choice is the best
is an empirical or philosophical question. In the case of Glent "similar queries", it
seems that being more aggressive and normalizing against the shorter string more
often gives better results. Your mileage may vary.

Use `setNormType(NormType normType)` to specify the length normalization type. By
default, `normType` is `NormType.MAX`; other possible values are `NormType.MIN` and
`NormType.FIRST`.

**Initial Letter Change Penalty**  
While any typo that *can* happen probably *will* happen at some point, changing the
first letter of a word seems like a bigger change than changing any other letter, so
it should carry a greater weight.

Use `setTokenInitialPenalty(float tokenInitialPenalty)` to configure the additional
penalty for changing the first letter of a token. By default, `tokenInitialPenalty`
is `0.25f`.

**Penalty for Changing the Number of Tokens**  
Splitting up words—especially by deleting letters—can really change a query. An
egregious example is  *abbys,* which, after deleting the first *b* and the *y,* gives
*a b s,* which is *very* different! So, we penalize both changing the number of
tokens and having to change a letter to a space (or vice versa).

Use `setTokenDeltaPenalty(float tokenDeltaPenalty)` to configure the additional
penalty for increasing or decreasing the number of tokens; the penalty is per net
token added or lost. By default, `tokenDeltaPenalty` is `0.25f`.

Use `setTokenSepSubstPenalty(float tokenSepSubstPenalty)` to configure the additional
penalty (in addition to the usual substitution cost) for converting a space (or other
token-separator character) to any other character, or vice versa. By default,
`tokenSepSubstPenalty` is `0.5f`.

**Spaceless Equality Exception**  
One exception to the penalty for changing the number of tokens is that strings that
are identical *except* for spaces or punctuation should have a lower edit distance.
So, *abbysplace* and *abby's place* are obviously very similar.

Use `setSpaceOnlyCost(float spaceOnlyCost)` to configure the cost of inserting or
deleting spaces in a strings that differ only by spaces. (Note: spaces, or the
token-separator character you specify.) By default, `spaceOnlyCost` is `0.1f`.

If `spaceOnlyCost` is equal to the usual insertion/deletion cost, then there will be
no discount.

**Duplicated Letters Discount**  
Repeated letters are a common form of typo—*appolonius* vs *apollonius,* for
example—and should not count as heavily as inserting or deleting a random letter.

Use `setDuplicateCost(float duplicateCost)` to configure the cost of inserting or
deleting duplicated letters (instead of the usual insertion/deletion cost). By
default, `duplicateCost` is `0.05f`.

**Number Change Penalty**  
Changes in numbers are more distinct than changes in letters—e.g., *1990s
championship* vs *1920s championship*—so such changes should have a higher cost.

Use `setDigitChangePenalty(float digitChangePenalty)` to configure the additional
cost of changing one digit to another (on top of the usual substitution cost). By
default, `digitChangePenalty` is `0.33f`. (Note that if the `digitChangePenalty` plus
the usual substitution penalty is more than double the insertion/deletion cost, it
will have no effect, because it will be cheaper to delete one digit and insert the
other as two separate operations.)

**Transpositions/Swaps Discount**  
And of course we should be able to discount transpositions—swapped letters, like
*queit* vs *quiet*—as with Damerau–Levenshtein distance.

The swap cost, as well as the default insert/deletion cost and substitution cost, are
all configurable.

Use `setInsDelCost(float insDelCost)` to configure the cost of inserting or deleting
a letter. By default, `insDelCost` is `1.0f`.

Use `setSubstCost(float substCost)` to configure the cost of changing one letter to
another. By default, `substCost` is `1.0f`.

Use `setSwapCost(float swapCost)` to configure the cost of transposing/swapping two
adjacent letters. By default, `swapCost` is `1.25f`.

## Tokenization (& Locale)

Obviously if the edit distance function is going to take tokens into account, it
needs to determine what those tokens are. Also, in the typical case, we probably want
to ignore most punctuation—e.g., *dog* and *dog!* are the same in most use-cases.

You can specify your own tokenizer that takes a string and returns an array of
strings, each of which will be treated as a single token, and joined with the
token-separator character, which is space by default. (You can specify a different
token separator if spaces are meaningful in your text.) Your custom tokenizer should
also do any lowercasing or other necessary text normalization.

For the languages that don't use spaces, like Chinese, a good tokenizer that breaks
a string into words can still be used. Internally, the tokens will be joined by
spaces (or a different token separator if you specify one). If you don't want that to
happen, you can always return a single token from your tokenizer, which might still
strip punctuation and normalize whitespace, for example. Bigram tokenizers or other
n-gram or shingle tokenizers—which are sometimes used for CJK languages—should
probably not be used for edit distance calculations.

If you don't or can't specify a tokenizer, a simple one will be built using the token
separator, token-splitting regex, and locale that you specify. Input strings will
have token separators (spaces by default) trimmed from either end of the string, the
result is lowercased based on the locale (English by default), and that string is
split based on the token-splitting regex (`[\\p{Z}\\p{P}\\p{S}]+` by default, which
matches sequences of Unicode spaces, punctuation, or symbols).

If you specify a `null` locale, no lowercasing will be done.

Another potentially easier but possibly less efficient tokenization option is to
tokenize your string externally, then join the tokens with spaces (or your specified
token separator) and let the built-in tokenizer re-tokenize the string on spaces.
This would prevent you from having to make your tokenizer externally available if
that's difficult or undesirable.

Use `setTokenizer(Function<String, String[]> tokenizer)` to specify a tokenization
function. By default, `tokenizer` is `null` and an internal tokenizer will be built
using the other values below.

Use `setTokenSep(char tokenSep)` to configure the token-separator character. By
default, `tokenSep` is a space character.

Use `setLocale(Locale locale)` to configure the locale used for lowercasing. By
default, `locale` is `Locale.ENGLISH`. Set `locale` to `null` for no lowercasing.
`locale` is only used if no tokenizer is specified with `setTokenizer()`.

Use `setTokenSplit(String tokenSplit)` to configure the regular expression used for
splitting strings into tokens. By default, `tokenSplit` is `[\\p{Z}\\p{P}\\p{S}]+`.
This may be too aggressive for some use cases. If you remove `\\p{S}` (Unicode
symbols) from the regex, it may be advisable to add `\\+` in its place when working
with strings that may have moved across the internet, since `+` gets used instead of
space with some regularity. `tokenSplit` is only used if no tokenizer is specified
with `setTokenizer()`.

## More Info

See the API docs in `target/apidocs/` for more information on class structure and
functions.

See `TokenAwareEditDistanceTest.java` in `src/test/...` for simple examples of
building and configuring a token-aware edit distance object.

On Wikimedia Commons: [Video
presentation](https://commons.wikimedia.org/wiki/File:Edit_Distance_%28%2BGlent%29_Learning_Circle_%282020-01-24%29.webm)
and
[slides](https://commons.wikimedia.org/wiki/File:Edit_Distance_%28%2BGlent%29_Learning_Circle.pdf)
providing an overview of a new token-aware edit distance algorithm, with a review of
basic edit distance and some of its variations, and a look at some of the data from
the Glent search suggestions project that lead to the development of the algorithm.