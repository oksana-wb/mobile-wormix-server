package com.pragmatix.steam.utils;

/**
 * Author: Vladimir
 * Date: 14.01.14 11:29
 */
public strictfp class DoubleUtils {
    public static final short SHORT_NORMALIZER = 10000;

    public static long round(double v) {
        if (v >= 0) {
            return StrictMath.round(v);
        } else {
            long res = (long) v;
            if (res - v >= 0.5) {
                res--;
            }
            return res;
        }
    }

    public static double rough(double v) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(v) & 0xFFFFFF0000000000L);
    }

    public static int packToInt(double v) {
        return (int) (Double.doubleToRawLongBits(v) >>> 40);
    }

    public static double unpackFromInt(int v) {
        return Double.longBitsToDouble(((long) v) << 40);
    }

    public static int compare(double d1, double d2) {
        if (d1 < d2) {
            return -1;
        } else if (d1 > d2) {
            return 1;
        } else {
            return 0;
        }
    }

    public static short packToShort(double v) {
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
            throw new IllegalArgumentException("" + v);
        }
        return (short) DoubleUtils.round(v * SHORT_NORMALIZER);
    }

    public static double unpackFromShort(short v) {
        return (double) v / SHORT_NORMALIZER;
    }

    public static double normalizeToShort(double v) {
        return unpackFromShort(packToShort(v));
    }
}
