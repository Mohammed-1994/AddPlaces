package com.awad.addplace.util;

import java.math.BigDecimal;

public class ddd {
    String location = "32.124,29.3254";
    static {
        String location = "32.124,29.3254";
        String lat = location.substring(0,location.indexOf(','));
        String lon = location.substring(location.indexOf(','), location.length()-1);

        String english = new BigDecimal(location).toString();
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
    }
    private static String arabicToDecimal(String number) {
        char[] chars = new char[number.length()];
        for(int i=0;i<number.length();i++) {
            char ch = number.charAt(i);
            if (ch >= 0x0660 && ch <= 0x0669)
                ch -= 0x0660 - '0';
            else if (ch >= 0x06f0 && ch <= 0x06F9)
                ch -= 0x06f0 - '0';
            chars[i] = ch;
        }
        return new String(chars);
    }

}
