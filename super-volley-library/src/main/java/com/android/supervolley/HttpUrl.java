package com.android.supervolley;


import okio.Buffer;

class HttpUrl {

    private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    static String canonicalize(String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
        int codePoint;
        for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
            codePoint = input.codePointAt(i);
            if (codePoint < 32 || codePoint == 127 || codePoint >= 128 && asciiOnly || encodeSet.indexOf(codePoint) != -1 || codePoint == 37 && (!alreadyEncoded || strict && !percentEncoded(input, i, limit)) || codePoint == 43 && plusIsSpace) {
                Buffer out = new Buffer();
                out.writeUtf8(input, pos, i);
                canonicalize(out, input, i, limit, encodeSet, alreadyEncoded, strict, plusIsSpace, asciiOnly);
                return out.readUtf8();
            }
        }

        return input.substring(pos, limit);
    }

    static void canonicalize(Buffer out, String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
        Buffer utf8Buffer = null;

        int codePoint;
        for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
            codePoint = input.codePointAt(i);
            if (!alreadyEncoded || codePoint != 9 && codePoint != 10 && codePoint != 12 && codePoint != 13) {
                if (codePoint == 43 && plusIsSpace) {
                    out.writeUtf8(alreadyEncoded ? "+" : "%2B");
                } else if (codePoint >= 32 && codePoint != 127 && (codePoint < 128 || !asciiOnly) && encodeSet.indexOf(codePoint) == -1 && (codePoint != 37 || alreadyEncoded && (!strict || percentEncoded(input, i, limit)))) {
                    out.writeUtf8CodePoint(codePoint);
                } else {
                    if (utf8Buffer == null) {
                        utf8Buffer = new Buffer();
                    }

                    utf8Buffer.writeUtf8CodePoint(codePoint);

                    while (!utf8Buffer.exhausted()) {
                        int b = utf8Buffer.readByte() & 255;
                        out.writeByte(37);
                        out.writeByte(HEX_DIGITS[b >> 4 & 15]);
                        out.writeByte(HEX_DIGITS[b & 15]);
                    }
                }
            }
        }

    }

    static String canonicalize(String input, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
        return canonicalize(input, 0, input.length(), encodeSet, alreadyEncoded, strict, plusIsSpace, asciiOnly);
    }

    static String percentDecode(String encoded, boolean plusIsSpace) {
        return percentDecode(encoded, 0, encoded.length(), plusIsSpace);
    }

    static String percentDecode(String encoded, int pos, int limit, boolean plusIsSpace) {
        for (int i = pos; i < limit; ++i) {
            char c = encoded.charAt(i);
            if (c == 37 || c == 43 && plusIsSpace) {
                Buffer out = new Buffer();
                out.writeUtf8(encoded, pos, i);
                percentDecode(out, encoded, i, limit, plusIsSpace);
                return out.readUtf8();
            }
        }

        return encoded.substring(pos, limit);
    }

    static void percentDecode(Buffer out, String encoded, int pos, int limit, boolean plusIsSpace) {
        int codePoint;
        for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
            codePoint = encoded.codePointAt(i);
            if (codePoint == 37 && i + 2 < limit) {
                int d1 = decodeHexDigit(encoded.charAt(i + 1));
                int d2 = decodeHexDigit(encoded.charAt(i + 2));
                if (d1 != -1 && d2 != -1) {
                    out.writeByte((d1 << 4) + d2);
                    i += 2;
                    continue;
                }
            } else if (codePoint == 43 && plusIsSpace) {
                out.writeByte(32);
                continue;
            }

            out.writeUtf8CodePoint(codePoint);
        }

    }

    static boolean percentEncoded(String encoded, int pos, int limit) {
        return pos + 2 < limit && encoded.charAt(pos) == 37 && decodeHexDigit(encoded.charAt(pos + 1)) != -1 && decodeHexDigit(encoded.charAt(pos + 2)) != -1;
    }

    static int decodeHexDigit(char c) {
        return c >= 48 && c <= 57 ? c - 48 : (c >= 97 && c <= 102 ? c - 97 + 10 : (c >= 65 && c <= 70 ? c - 65 + 10 : -1));
    }
}
