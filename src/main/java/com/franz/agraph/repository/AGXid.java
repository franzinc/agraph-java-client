package com.franz.agraph.repository;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.transaction.xa.Xid;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * This class provides an AllegroGraph-compatible implementation of the Xid interface.
 * It is used to name prepared commits.
 */
public class AGXid implements Xid {

    private final int formatId;
    private final byte[] globalTransactionId;
    private final byte[] branchQualifier;

    public AGXid(int formatId, byte[] globalTransactionId, byte[] branchQualifier) {
        this.formatId = formatId;
        this.globalTransactionId = Arrays.copyOf(globalTransactionId, globalTransactionId.length);
        this.branchQualifier = Arrays.copyOf(branchQualifier, branchQualifier.length);
    }

    public AGXid(Xid xid) {
        this(xid.getFormatId(), xid.getGlobalTransactionId(), xid.getBranchQualifier());
    }

    public static AGXid AGXidFromString(String s) throws DecoderException {
        String[] parts = s.split(":");

        int formatId = Integer.parseInt(parts[0]);
        byte[] globalTransactionId = decodeXidPart(parts[1]);
        byte[] branchQualifier = decodeXidPart(parts[2]);

        return new AGXid(formatId, globalTransactionId, branchQualifier);
    }

    @Override
    public int getFormatId() {
        return formatId;
    }

    /**
     * Returns a copy of the global transaction id part of this Xid.
     */
    @Override
    public byte[] getGlobalTransactionId() {
        return Arrays.copyOf(globalTransactionId, globalTransactionId.length);
    }

    /**
     * Returns a copy of the branch qualifier part of this Xid.
     */
    @Override
    public byte[] getBranchQualifier() {
        return Arrays.copyOf(branchQualifier, branchQualifier.length);
    }

    // Safe means: letters, digits, and period in the ASCII codepoint range.
    // This is sufficient to allow for xids created by Transaction Essentials to
    // be presented as plaintext, which greatly aids debugging.
    private static boolean isSafeASCII(byte b) {
        return b < 128 && (Character.isLetterOrDigit(b) || b == '.');
    }

    private static boolean isSafeASCII(byte[] arr) {
        if (arr.length >= 2 && arr[0] == '0' && arr[1] == 'x') {
            // The byte array looks like it begins with "0x"
            // which would be confused for the syntax we use to encode
            // unsafe byte arrays, so this is also considered unsafe.
            return false;
        }

        for (byte b : arr) {
            if (!isSafeASCII(b))
                return false;
        }
        return true;
    }

    private static String byteArrayToString(byte[] array) {
        if (isSafeASCII(array)) {
            return new String(array, StandardCharsets.US_ASCII);
        } else {
            return "0x" + Hex.encodeHexString(array);
        }
    }

    private static byte[] hexStringToByteArray(String s) throws DecoderException {
        return Hex.decodeHex(s.toCharArray());
    }

    private static byte[] decodeXidPart(String part) throws DecoderException {
        if (part.startsWith("0x")) {
            return hexStringToByteArray(part.substring(2));
        } else {
            return part.getBytes(StandardCharsets.US_ASCII);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(getFormatId()) + ":"
                + byteArrayToString(getGlobalTransactionId()) + ":"
                + byteArrayToString(getBranchQualifier());
    }

}
