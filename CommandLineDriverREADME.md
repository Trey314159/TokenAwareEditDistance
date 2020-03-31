# Token-Aware Edit Distance Command-Line Driver

March 2020

By [Trey Jones](https://github.com/Trey314159).

## Background

This is a simple command-line driver for the token-aware edit distance class. It
allows you to set most of the parameters in the class, and either compare two
strings, or compare tab-separated pairs of strings in a file.

## Build & Run

If you have Maven installed, you can build the self-contained token-aware edit
distance driver by running `mvn clean package` from the top level directory of the
repository. The self-contained executable jar file will be built in
`target/taed-1.0-jar-with-dependencies.jar`.

You can run the executable jar from the command line with the command
`java -jar target/taed-1.0-jar-with-dependencies.jar`.

The usage output is below (see the main `README.md` file for more details on each
option):

    Input: <string1> <string2> OR <filename.tab>

    Additional options:
     -l (--editLimit)               : raw edit distance limit; 0f indicates no limit (default: 2.0)
     -p (--normEditLimit)           : percentage edit distance limit; 0f indicates no limit (default:
                                      0.0)
     -dp (--disablePerTokenLimit)   : disable applying percentage limit per token (default: false)
     -n (--normType)                : normalization based on longer string (max), shorter string (min),
                                      or first string (first) (default: max)
     -d (--dupeCost)                : reduced cost for inserting or deleting duplicate letters [abc /
                                      abbc] (default: 0.05)
     -i (--insDelCost)              : cost for insertions or deletions [abc / ab] (default: 1.0)
     -s (--substCost)               : cost for substitutions [abc / axc] (default: 1.0)
     -w (--swapCost)                : cost for swaps/transpositions [abc / acb] (default: 1.25)
     -c (--digitChangePenalty)      : additional penalty for changing digits [a7c / a8c] (default: 0.33)
     -t (--tokenInitialPenalty)     : additional penalty for changing the first letter of a token [abc
                                      / xbc] (default: 0.25)
     -T (--tokenDeltaPenalty)       : additional penalty for changing the number of tokens (default:
                                      0.25)
     -S (--tokenSepSubstPenalty)    : additional penalty for changing a token separator (space by
                                      default) (default: 0.5)
     -P (--spaceOnlyCost)           : reduced cost for edits that only involve spaces [abcd / ab cd]
                                      (default: 0.1)
     -sep (--tokenSep)              : token separator character; default is space (default:  )
     -spl (--tokenSplit)            : regex for splitting tokens (default: [\p{Z}\p{P}\p{S}]+)
     -h (--help)                    : display help (default: false)

So, to compute the edit distance between `abcd` and `acbd`, using the default
configuration, use the following command:

    java -jar target/taed-1.0-jar-with-dependencies.jar abcd acbd

You can also use quotes around multi-token inputs:

    java -jar target/taed-1.0-jar-with-dependencies.jar "a bc d" "ab cd"

Output is always in the same tab-separated format:

    <dist>	<input1>	<input2>

Values of `Float.POSITIVE_INFINITY` are replaced with `9999` for ease of parsing by
other programs. If this is going to cause problems because you are computing the edit
distance of strings of length approaching 10,000, maybe you should re-examine your
life choices. `;-)`

An example file of input pairs, `example.tab` is provided. You can run it like this:

    java -jar target/taed-1.0-jar-with-dependencies.jar example.tab

Output:

    0.30	a bc d	ab cd
    1.25	abcd	acbd
    1.50	xxxx	yyy
    9999	file	foul
    9999	abbys	a b s

If you want to remove the limits so that all distances are calculated (so that none
are `Float.POSITIVE_INFINITY` / `9999`), set the edit limit to 0 (no limit):

    java -jar target/taed-1.0-jar-with-dependencies.jar example.tab -l 0

Output:

    0.30	a bc d	ab cd
    1.25	abcd	acbd
    1.50	xxxx	yyy
    3.00	file	foul
    3.05	abbys	a b s


## Unavailable Options

This driver does not let you set the **locale** for lowercasing or specify a **custom
tokenization** function, though it should be easy enough to modify it for either such
use case.

Also, figuring out how to properly escape the `tokenSplit` regex on the command line
is left as an exercise for the reader.
