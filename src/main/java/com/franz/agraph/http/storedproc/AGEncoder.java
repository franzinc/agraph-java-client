/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.storedproc;

import java.util.ArrayList;

/**
 * @since v4.2
 * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
 */
public class AGEncoder {

    static char codeToChar[] =
            {
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
                    'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
                    'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                    '4', '5', '6', '7', '8', '9', '*', '+'
            };

    public static String encode(byte[] arr) {
        ArrayList<Character> resx = new ArrayList<Character>();

        int state = 0;

        int rem = 0;

        for (byte b : arr) {
            switch (state) {
                case 0:
                    resx.add(codeToChar[b & 0x3f]);
                    rem = (b >> 6) & 0x3;
                    break;
                case 1:
                    resx.add(codeToChar[((b & 0xf) << 2) | rem]);
                    rem = (b >> 4) & 0xf;
                    break;
                case 2:
                    resx.add(codeToChar[((b & 0x3) << 4) | rem]);
                    resx.add(codeToChar[((b >> 2) & 0x3f)]);
            }

            state = (state + 1) % 3;

        }

        if (state != 0) {
            resx.add(codeToChar[rem]);
        }

        // there must be an easier way to turn an ArrayList<Character> into a String.
        char[] retstr = new char[resx.size()];
        int index = 0;
        for (Character c : resx) {
            retstr[index++] = c;
        }
        return new String(retstr);

    }

}
