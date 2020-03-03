package com.company;

import java.util.ArrayList;
import java.util.List;

public class Algorithm {
    private Algorithm() { }

    private static final byte SUPPL_BIT_POSITION = 6;
    private static final byte NUMBER_OF_SUPPL_BITS = 2;


    static byte[] RLE (byte[] input) {
        List<Byte> result = new ArrayList<>();
        int i = 0;
        while (i < input.length) {
            byte seqLen = 1;
            while (i + seqLen < input.length && input[i] == input[i + seqLen] && seqLen < Byte.MAX_VALUE / 2) {
                seqLen++;
            }
            if (seqLen == 1) {
                //последовательность разных байтов
                while (i + seqLen + 1 < input.length && input[i + seqLen + 1] != input[i + seqLen] && seqLen < Byte.MAX_VALUE / 2) {
                    seqLen++;
                }
                byte decoder = 0;
                decoder += seqLen;
                result.add(decoder);
                for (int j = 0; j < seqLen; j++) {
                    result.add(input[i + j]);
                }
            }
            else {
                //последовательность одинаковых байтов
                byte decoder = 1;
                decoder <<= SUPPL_BIT_POSITION;
                decoder += seqLen;
                result.add((byte) (decoder));
                result.add(input[i]);
            }
            i += seqLen;
        }
        byte[] res = new byte[result.size()];
        int j = 0;
        for(Byte b: result)
            res[j++] = b.byteValue();
        return res;
    }


    static byte[] RLE_Decode (byte[] input) {
        List<Byte> result = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            byte decoder = input[i++];
            byte length = (byte) ((decoder << NUMBER_OF_SUPPL_BITS) >> NUMBER_OF_SUPPL_BITS);
            decoder >>= SUPPL_BIT_POSITION;
            if (decoder > 0) {
                //последовательность одинаковых байтов
                for (int j = 0; j < length; j++) {
                    result.add(input[i]);
                }
            }
            else {
                //последовательность разных байтов
                for (int j = 0; j < length && (i + j) < input.length; j++) {
                    result.add(input[i + j]);
                }
                i += length - 1;
            }
        }
        byte[] res = new byte[result.size()];
        int j = 0;
        for(Byte b: result)
            res[j++] = b.byteValue();
        return res;
    }
}

