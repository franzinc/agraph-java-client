package com.franz.agraph.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.transaction.xa.XAResource;

import org.apache.commons.codec.binary.Hex;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class AGTransactionalResource extends XATransactionalResource {
    private final AGRepository repo;

    public AGTransactionalResource(final AGRepository repo) throws NoSuchAlgorithmException {
        super(repoToHash(repo));
        this.repo = repo;
    }

    /**
     * XATransactionalResource limits the servername string to some (moderately opaque) number
     * of characters.  Since the repo URL may be any number of characters, we compensate by computing
     * an md5 hash of the UTF-8 encoding of the URL and use the hex string representation of the hash
     * as the servername.
     *
     * @param repo The AGRepository whose URL will be hashed.
     * @return A hex string representing the hash of the repo URL.
     * @throws NoSuchAlgorithmException if MD5 is not available.
     */
    private static String repoToHash(AGRepository repo) throws NoSuchAlgorithmException {
        String repoUrl = repo.getRepositoryURL();
        byte[] repoUrlEncoded = repoUrl.getBytes(StandardCharsets.UTF_8);
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        return new String(Hex.encodeHex(md5.digest(repoUrlEncoded)));
    }


    @Override
    protected XAResource refreshXAConnection() throws ResourceException {
        return repo.getConnection().getXAResource();
    }
}
