/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.storedproc;

/**
 * @since v4.2
 * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
 */
public class AGDecoder {

    static byte charToCode[] = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 63, 0, 0, 0, 0,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 0, 0, 0,
            0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 0, 0, 0, 0, 0,
            0, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
    };

    public static byte[] decode(String sval) {
        ByteArray retv = new ByteArray();
        int state = 0;
        byte rem = 0;

        for (int i = 0; i < sval.length(); i++) {
            char ch = sval.charAt(i);
            byte val = charToCode[ch];

            switch (state) {
                case 0:
                    rem = val;
                    break;

                case 1:
                    retv.addbyte((byte) (rem | ((val & 0x3) << 6)));
                    rem = (byte) (val >> 2);
                    break;

                case 2:
                    retv.addbyte((byte) (rem | ((val & 0xf) << 4)));
                    rem = (byte) (val >> 4);
                    break;

                case 3:
                    retv.addbyte((byte) (rem | (val << 2)));

            }

            if (++state > 3) {
                state = 0;
            }
        }
        return retv.extract();
    }

}
