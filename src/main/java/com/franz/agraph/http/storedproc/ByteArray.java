/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.storedproc;

/**
 * useful operations on byte arrays
 */
class ByteArray {

    private byte[] data;
    private int wpos; // next index into which to write

    public ByteArray() {
        data = new byte[1024];
        wpos = 0;
    }

    public ByteArray(byte[] barr) {
        data = barr;
        wpos = data.length;
    }

    public void append(byte[] src, int max) {
        // add the given byte array up to max to the curret
        // byte array
        if (wpos + max >= data.length) {
            // need new data
            byte[] newdata = new byte[wpos + max + 2048];
            System.arraycopy(data, 0, newdata, 0, wpos);
            data = newdata;
        }

        System.arraycopy(src, 0, data, wpos, max);
        wpos += max;
    }

    void addbyte(byte b) {
        if (wpos + 1 >= data.length) {
            // need new data
            byte[] newdata = new byte[wpos + 2048];
            System.arraycopy(data, 0, newdata, 0, wpos);
            data = newdata;
        }
        data[wpos] = b;
        wpos++;

    }

    public byte[] extract() {
        // return a byte array with the contents of this object
        byte[] retobj = new byte[wpos];

        System.arraycopy(data, 0, retobj, 0, wpos);
        return retobj;
    }

}
