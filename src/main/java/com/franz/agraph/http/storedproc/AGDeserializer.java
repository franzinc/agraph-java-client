/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.storedproc;

import java.util.ArrayList;
import java.util.List;

/**
 * @since v4.2
 * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
 */
public class AGDeserializer {

    /* data to process */
    private byte[] data;

    private int pos;

    private int max;

    public AGDeserializer(byte[] givendata) {
        data = givendata;
        pos = 0;
        max = givendata.length;
    }

    public static Object decodeAndDeserialize(String data) {
        AGDeserializer o = new AGDeserializer(AGDecoder.decode(data));
        return o.deserialize();
    }

    byte nextbyte() {
        if (pos >= max) {
            throw new RuntimeException("ran off the end");
        }
        pos++;
        return data[pos - 1];
    }

    int posInteger() {
        int result = 0;
        int shift = 0;

        while (true) {
            int val = nextbyte();
            int masked;

            masked = val & 0x7f;
            result = result + (masked << shift);
            if ((val & 0x80) == 0) {
                break;
            }
            shift += 7;
        }

        return result;

    }

    public Object deserialize() {
        byte val = nextbyte();
        int length;

        switch (val) {
            case SerialConstants.SO_BYTEVECTOR: {
                length = posInteger();
                byte[] res = new byte[length];
                for (int i = 0; i < length; i++) {
                    res[i] = nextbyte();
                }
                return res;
            }

            case SerialConstants.SO_VECTOR: {
                length = posInteger();
                Object[] res = new Object[length];
                for (int i = 0; i < length; i++) {
                    res[i] = deserialize();
                }
                return res;
            }

            case SerialConstants.SO_LIST: {
                length = posInteger();
                List res = new ArrayList(length);
                for (int i = 0; i < length; i++) {
                    res.add(deserialize());
                }
                // TODO: extra null needed by lisp side, bug in lisp?
                nextbyte();
                return res;
            }

            case SerialConstants.SO_STRING: {
                length = posInteger();

                StringBuilder res = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    res.append((char) nextbyte());
                }
                return res.toString();
            }

            case SerialConstants.SO_POS_INTEGER: {
                return posInteger();
            }

            case SerialConstants.SO_NEG_INTEGER: {
                return -posInteger();
            }

            case SerialConstants.SO_NULL:
                return null;

            case SerialConstants.SO_END_OF_ITEMS:
                return null;

            default:
                throw new RuntimeException("bad code found by deserializer: " + val);

        }
    }

}
