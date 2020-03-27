package trey314159.taed;

// edit distance length normalization method type
public enum NormType {
    MAX,   // normalize against the longer string (default)
    MIN,   // normalize against the shorter string
    FIRST  // normalize against the first string, i.e., str1
}
