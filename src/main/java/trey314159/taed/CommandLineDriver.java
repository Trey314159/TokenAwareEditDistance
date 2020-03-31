package trey314159.taed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import trey314159.taed.EDConfig;
import trey314159.taed.NormType;
import trey314159.taed.TokenAwareEditDistance;

public class CommandLineDriver {

    private static TokenAwareEditDistance ed;

    @Option(name = "-l", aliases = { "--editLimit" }, metaVar = " ",
        usage = "raw edit distance limit; 0f indicates no limit")
    private static float defaultLimit = 2.0f;

    @Option(name = "-p", aliases = { "--normEditLimit" }, metaVar = " ",
        usage = "percentage edit distance limit; 0f indicates no limit")
    private static float defaultNormLimit = 0f;

    @Option(name = "-dp", aliases = { "--disablePerTokenLimit" }, metaVar = " ",
        usage = "disable applying percentage limit per token")
    private static boolean noPerTokenLimit = false;

    @Option(name = "-n", aliases = { "--normType" }, metaVar = " ",
        usage = "normalization based on longer string (max), shorter " +
                "string (min), or first string (first)")
    private static String normTypeString = "max";
    private static NormType normType = NormType.MAX;

    @Option(name = "-d", aliases = { "--dupeCost" }, metaVar = " ",
        usage = "reduced cost for inserting or deleting duplicate letters [abc / abbc]")
    private static float duplicateCost = 0.05f;

    @Option(name = "-i", aliases = { "--insDelCost" }, metaVar = " ",
        usage = "cost for insertions or deletions [abc / ab]")
    private static float insDelCost = 1.0f;

    @Option(name = "-s", aliases = { "--substCost" }, metaVar = " ",
        usage = "cost for substitutions [abc / axc]")
    private static float substCost = 1.0f;

    @Option(name = "-w", aliases = { "--swapCost" }, metaVar = " ",
        usage = "cost for swaps/transpositions [abc / acb]")
    private static float swapCost = 1.25f;

    @Option(name = "-c", aliases = { "--digitChangePenalty" }, metaVar = " ",
        usage = "additional penalty for changing digits [a7c / a8c]")
    private static float digitChangePenalty = 0.33f;

    @Option(name = "-t", aliases = { "--tokenInitialPenalty" }, metaVar = " ",
        usage = "additional penalty for changing the first letter of a token [abc / xbc]")
    private static float tokenInitialPenalty = 0.25f;

    @Option(name = "-T", aliases = { "--tokenDeltaPenalty" }, metaVar = " ",
        usage = "additional penalty for changing the number of tokens")
    private static float tokenDeltaPenalty = 0.25f;

    @Option(name = "-S", aliases = { "--tokenSepSubstPenalty" }, metaVar = " ",
        usage = "additional penalty for changing a token separator (space by default)")
    private static float tokenSepSubstPenalty = 0.50f;

    @Option(name = "-P", aliases = { "--spaceOnlyCost" }, metaVar = " ",
        usage = "reduced cost for edits that only involve spaces [abcd / ab cd]")
    private static float spaceOnlyCost = 0.1f;

    @Option(name = "-sep", aliases = { "--tokenSep" }, metaVar = " ",
        usage = "token separator character; default is space")
    private static char tokenSep = ' ';

    @Option(name = "-spl", aliases = { "--tokenSplit" }, metaVar = " ",
        usage = "regex for splitting tokens")
    private static String tokenSplit = "[\\p{Z}\\p{P}\\p{S}]+";


    @Option(name = "-h", aliases = { "--help" }, metaVar = " ",
        usage = "display help")
    private static boolean displayHelp = false;


    @Argument
    private static List<String> arguments = new ArrayList<String>();


    public static void main(String[] args) {
        // configure args4j
        CommandLineDriver cld = new CommandLineDriver();
        ParserProperties properties = ParserProperties.defaults();
        properties.withOptionSorter(null);
        CmdLineParser parser = new CmdLineParser(cld, properties);
        parser.setUsageWidth(100);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            showUsageAndExit(parser, System.err, e.getMessage(), 1);
        }

        if (displayHelp) {
            displayHelp = false; // reset for usage output
            showUsageAndExit(parser, System.out, "", 0);
        }

        normTypeString = normTypeString.toLowerCase();
        switch(normTypeString) {
            case "min":
                normType = NormType.MIN;
                break;
            case "first":
                normType = NormType.FIRST;
                break;
            case "max":
                normType = NormType.MAX;
                break;
            default:
                String errMsg = "\"" + normTypeString + 
                    "\" is not a valid value for \"-n\"";
                normTypeString = "max"; // reset for usage output
                showUsageAndExit(parser, System.err, errMsg, 1);
        }

        ed = new TokenAwareEditDistance(EDConfig.Builder.newInstance()
            .setDefaultLimit(defaultLimit)
            .setDefaultNormLimit(defaultNormLimit)
            .setPerTokenLimit(!noPerTokenLimit)
            .setNormType(normType)
            .setInsDelCost(insDelCost)
            .setSubstCost(substCost)
            .setSwapCost(swapCost)
            .setDuplicateCost(duplicateCost)
            .setDigitChangePenalty(digitChangePenalty)
            .setTokenInitialPenalty(tokenInitialPenalty)
            .setTokenSepSubstPenalty(tokenSepSubstPenalty)
            .setTokenDeltaPenalty(tokenDeltaPenalty)
            .setSpaceOnlyCost(spaceOnlyCost)
            .setTokenSep(tokenSep)
            .setTokenSplit(tokenSplit)
            .build());

        switch (arguments.size()) {
            case 2:
                comp(arguments.get(0), arguments.get(1));
                break;
            case 1:
                compFile(arguments.get(0));
                break;
            default:
                showUsageAndExit(parser, System.err, "Wrong number of arguments", 1);
        }

    }

    private static void compFile(String fileName) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line = reader.readLine();
            int lineCount = 1;

            while (line != null) {
                String[] inputs = line.split("\t", -1);
                if (inputs.length != 2) {
                    System.err.println("Expecting two inputs on line " +
                        lineCount + " of " + fileName + "; found " +
                        inputs.length + ": " + line);
                    System.exit(1);
                }

                comp(inputs[0], inputs[1]);

                line = reader.readLine();
                lineCount++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void comp(String a, String b) {
        float diff = ed.calcEditDistance(a, b);
        System.out.println(
            (diff == Float.POSITIVE_INFINITY ? "9999" : String.format("%2.2f", diff))
            + "\t" + a + "\t" + b
            );
    }

    private static void showUsageAndExit(CmdLineParser parser, PrintStream stream,
                String msg, int exitStatus) {
            if (!msg.equals("")) {
                stream.println(msg + "\n");
                }
            stream.println("Input: <string1> <string2> OR <filename.tab>\n");
            stream.println("Additional options:");
            parser.printUsage(stream);
            System.exit(exitStatus);
    }

}