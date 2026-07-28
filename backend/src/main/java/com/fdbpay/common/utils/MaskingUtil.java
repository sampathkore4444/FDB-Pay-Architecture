package com.fdbpay.common.utils;

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

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
