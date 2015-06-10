package org.kaloz.homework;

class Zip {
    public int solution(final int A, final int B) {

        String result = calculate(Integer.toString(A), Integer.toString(B), new StringBuilder(""));

        if (Long.parseLong(result) > 100_000_000L)
            return -1;
        else
            return Integer.parseInt(result);
    }

    private String calculate(final String A, final String B, final StringBuilder zip) {
        if (A.length() == 0) {
            return zip.append(B).toString();
        } else if (B.length() == 0) {
            return zip.append(A).toString();
        } else {
            return calculate(A.substring(1), B.substring(1), zip.append(A.charAt(0)).append(B.charAt(0)));
        }

    }
}
