package com.fdbpay.shared.utils;

public final class MaskingUtil {
    private MaskingUtil() {}

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    public static String maskNrc(String nrc) {
        if (nrc == null || nrc.length() < 6) return nrc;
        return nrc.substring(0, 3) + "******" + nrc.substring(nrc.length() - 3);
    }
}
