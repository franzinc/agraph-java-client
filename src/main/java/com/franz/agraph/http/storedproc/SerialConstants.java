/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.storedproc;

/**
 *
 */
interface SerialConstants {

    byte SO_VECTOR = 1;
    byte SO_STRING = 5;
    byte SO_NULL = 7;
    byte SO_LIST = 8;
    byte SO_POS_INTEGER = 9;
    byte SO_END_OF_ITEMS = 10;
    byte SO_NEG_INTEGER = 11;
    byte SO_BYTEVECTOR = 15; // usb8 vector

}
