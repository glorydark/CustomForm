package glorydark.nukkit.customform.utils;

public enum MathCompareSign {
    EQUAL,
    BIGGER_OR_EQUAL,
    BIGGER,
    SMALLER_OR_EQUAL,
    SMALLER,
    NOT_A_SIGN;

    public static MathCompareSign getComparedSignType(String s) {
        switch (s) {
            case ">":
                return BIGGER;
            case ">=":
                return BIGGER_OR_EQUAL;
            case "<":
                return SMALLER;
            case "<=":
                return SMALLER_OR_EQUAL;
            case "=":
                return EQUAL;
        }
        return NOT_A_SIGN;
    }

    public static boolean compareValue(double value, MathCompareSign type, double comparedValue) {
        switch (type) {
            case EQUAL:
                return value == comparedValue;
            case BIGGER:
                return value > comparedValue;
            case SMALLER:
                return value < comparedValue;
            case BIGGER_OR_EQUAL:
                return value >= comparedValue;
            case SMALLER_OR_EQUAL:
                return value <= comparedValue;
            default:
                return false;
        }
    }

    public static boolean compareValue(int value, MathCompareSign type, int comparedValue) {
        switch (type) {
            case EQUAL:
                return value == comparedValue;
            case BIGGER:
                return value > comparedValue;
            case SMALLER:
                return value < comparedValue;
            case BIGGER_OR_EQUAL:
                return value >= comparedValue;
            case SMALLER_OR_EQUAL:
                return value <= comparedValue;
            default:
                return false;
        }
    }
}
