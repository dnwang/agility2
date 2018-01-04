package org.pinwheel.agility2.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;

/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 */
public final class DigestUtils {

    /**
     * Used to build output as Hex
     */
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private DigestUtils() {
        throw new AssertionError();
    }

    public static String md5(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(str.getBytes());
            return new String(encodeHex(messageDigest.digest()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String md5(File file, int startPon, int length) {
        RandomAccessFile fileReader = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fileReader = new RandomAccessFile(file, "r");
            fileReader.seek(startPon);
            byte[] buffer = new byte[1024];
            int eachLength = 1024;
            int partNum = length / eachLength;
            int lastPartLen = length % eachLength;
            for (int i = 0; i < partNum; i++) {
                if ((eachLength = fileReader.read(buffer)) != -1) {
                    md.update(buffer, 0, eachLength);
                }
            }
            if (lastPartLen != 0) {
                if ((eachLength = fileReader.read(buffer, 0, lastPartLen)) != -1) {
                    md.update(buffer, 0, eachLength);
                }
            }
            return new String(encodeHex(md.digest()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(fileReader);
        }
        return null;
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @return A char[] containing hexadecimal characters
     */
    private static char[] encodeHex(final byte[] data) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return out;
    }


}
