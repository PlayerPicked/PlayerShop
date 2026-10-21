package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.identifiers.keys.ConfigKeys;
import com.playerpicked.playershop.identifiers.types.AmountFormatType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class AmountFormatUtil {
    private static volatile AmountFormatType type = AmountFormatType.COMPACT;

    private AmountFormatUtil() {}

    public static void reload() {
        String raw = ConfigKeys.FORMATTING_AMOUNT_FORMAT.getString();
        type = parse(raw);
    }

    public static String format(long value) {
        return format((double) value);
    }

    public static String format(double value) {
        return switch (type) {
            case COMPACT -> compact(value);
            case GROUPED_COMMA -> grouped(value, ',');
            case GROUPED_DOT -> grouped(value, '.');
            case GROUPED_SPACE -> grouped(value, ' ');
        };
    }

    private static AmountFormatType parse(String s) {
        if (s == null) return AmountFormatType.COMPACT;
        try {
            return AmountFormatType.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return AmountFormatType.COMPACT;
        }
    }

    @NotNull
    private static String grouped(double v, char sep) {
        boolean neg = v < 0; long n = Math.round(Math.abs(v));
        String s = Long.toString(n);

        StringBuilder out = new StringBuilder(s.length() + s.length() / 3);
        int c = 0; for (int i = s.length() - 1; i >= 0; i--) { out.append(s.charAt(i)); c++; if (i > 0 && c % 3 == 0) out.append(sep); }
        out.reverse();

        return neg ? "-" + out : out.toString();
    }

    @NotNull
    private static String compact(double v) {
        boolean neg = v < 0; double a = Math.abs(v);
        if (a < 1_000d) {
            long rounded = Math.round(a);
            return neg ? "-" + rounded : String.valueOf(rounded);
        }
        if (a < 1_000_000d) return sign(neg) + fmt(a, 1_000d, "k");
        if (a < 1_000_000_000d) return sign(neg) + fmt(a, 1_000_000d, "M");
        if (a < 1_000_000_000_000d) return sign(neg) + fmt(a, 1_000_000_000d, "B");
        if (a < 1_000_000_000_000_000d) return sign(neg) + fmt(a, 1_000_000_000_000d, "T");
        return sign(neg) + fmt(a, 1_000_000_000_000_000d, "Q");
    }

    @NotNull
    @Contract(pure = true)
    private static String sign(boolean neg) {
        return neg ? "-" : "";
    }

    @NotNull
    private static String fmt(double a, double div, String suf) {
        double n = a / div;
        String s = n >= 100 ? Long.toString(Math.round(n)) : oneDec(n);
        return s + suf;
    }

    @NotNull
    private static String oneDec(double n) {
        double r = Math.round(n * 10d) / 10d;
        String s = String.valueOf(r);
        int i = s.indexOf('.'); if (i < 0) return s;
        if (s.length() == i + 2 && s.charAt(i + 1) == '0') return s.substring(0, i);
        return s;
    }

    private static String trim0(double n) {
        long l = Math.round(n);
        if (Math.abs(n - l) < 1e-9) return Long.toString(l);
        String s = Double.toString(n);
        int i = s.indexOf('.'); if (i < 0) return s;
        int e = s.length(); while (e > i + 1 && s.charAt(e - 1) == '0') e--; if (e == i + 1) e = i;
        return s.substring(0, e);
    }
}