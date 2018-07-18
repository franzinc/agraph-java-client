package com.franz.agraph.repository;

import com.franz.agraph.http.exception.AGHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.codec.DecoderException;

public class AGXAResource implements XAResource {
    AGRepositoryConnection conn;

    Logger logger = LoggerFactory.getLogger(AGXAResource.class);

    /**
     * Examines AGHttpException and tries to find a corresponding XAException and throws it further.
     * If matching XAException cannot be found the original exception is re-thrown.
     * @param e Original AGHttpException
     * @throws XAException
     * @throws AGHttpException
     */
    private void tryThrowAsXAException(AGHttpException e) throws XAException, AGHttpException {
        if (e.getMessage().contains("Could not find a prepared commit with xid")) {
            throw new XAException(XAException.XAER_NOTA);
        } else {
            throw e;
        }
    }

    // Constructor
    public AGXAResource(AGRepositoryConnection conn) {
        this.conn = conn;
    }

    /**
     * Starts work on behalf of a transaction branch specified in xid. If TMJOIN
     * is specified, the start applies to joining a transaction previously seen
     * by the resource manager. If TMRESUME is specified, the start applies to
     * resuming a suspended transaction specified in the parameter xid. If
     * neither TMJOIN nor TMRESUME is specified and the transaction specified
     * by xid has previously been seen by the resource manager, the resource
     * manager throws the XAException exception with XAER_DUPID error code.
     *
     * @param xid A global transaction identifier to be associated with the resource.
     * @param flags One of TMNOFLAGS, TMJOIN, or TMRESUME.
     */
    @Override
    public void start(Xid xid, int flags) throws XAException {
        if (logger.isDebugEnabled()) {
            logger.debug("AGXAResource.start() called on " + this);
            logger.debug("---> XID " + xid.toString());
        }

        switch (flags) {
        case TMNOFLAGS:
            // Good to go
            break;
        case TMJOIN:
            throw new XAException("start doesn't handle TMJOIN yet");
        case TMRESUME:
            throw new XAException("start doesn't handle TMRESUME yet");
        default:
            // Supplied flags do not match the specification
            throw new XAException(XAException.XAER_INVAL);
        }

        // Use of XAResource is only suitable for non-auto-commit mode
        if (!conn.isActive()) {
            throw new XAException(XAException.XAER_OUTSIDE); // I guess
        }

    }

    /**
     * Disassociate the transaction (identified by xid) from the resource
     *
     * Ends the work performed on behalf of a transaction branch. The resource
     * manager disassociates the XA resource from the transaction branch
     * specified and lets the transaction complete. If TMSUSPEND is specified in
     * the flags, the transaction branch is temporarily suspended in an
     * incomplete state. The transaction context is in a suspended state and
     * must be resumed via the start method with TMRESUME specified. If TMFAIL
     * is specified, the portion of work has failed. The resource manager may
     * mark the transaction as rollback-only If TMSUCCESS is specified, the
     * portion of work has completed successfully.
     *
     * @param xid A global transaction identifier that is the same as the identifier
     *            used previously in the start method.
     * @param flags One of TMSUCCESS, TMFAIL, or TMSUSPEND.
     */
    @Override
    public void end(Xid xid, int flags) throws XAException {
        if (logger.isDebugEnabled()) {
            logger.debug("AGXAResource.end() called on " + this);
        }

        switch (flags) {
        case TMSUCCESS:
            logger.debug("AGXAResource.end() called with TMSUCCESS");
            break;
        case TMFAIL:
            logger.debug("AGXAResource.end() called with TMFAIL");
            break;
        case TMSUSPEND:
            logger.debug("AGXAResource.end() called with TMSUSPEND");
            throw new XAException("TMSUSPEND not handled yet");
        default:
            logger.debug("AGXAResource.end() called with unknown flags: " + flags);
            throw new XAException(XAException.XAER_INVAL);

        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        if (logger.isDebugEnabled()) {
            logger.debug("AGXAResource.rollback() called on " + this);
            logger.debug("---> Supplied xid " + xid.toString());
        }

        try {
            conn.rollback(xid);
        } catch (AGHttpException e) {
            tryThrowAsXAException(e);
        }
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the
     * transaction specified in xid.
     *
     * @param xid The global transaction identifier to assign to the prepared commit.
     * @return A value indicating the resource manager's vote on
     *         the outcome of the transaction. The possible values are: XA_RDONLY or
     *         XA_OK. If the resource manager wants to roll back the transaction, it
     *         should do so by raising an appropriate XAException in the prepare method.
     * @throws XAException An error has occurred. Possible exception values
     *                     are: XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or
     *                     XAER_PROTO.
     */
    @Override
    public int prepare(Xid xid) throws XAException {
        if (logger.isDebugEnabled())
            logger.debug("AGXAResource.prepare() called on " + this);

        // First phase of 2PC

        try {
            conn.prepareCommit(xid);
        } catch (AGHttpException e) {
            tryThrowAsXAException(e);
        }

        return XA_OK;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (logger.isDebugEnabled())
            logger.debug("AGXAResource.commit(xid=" + xid + ", onePhase=" + onePhase + ") called on " + this);

        try {
            if (onePhase) {
                conn.commit();
            } else {
                /* Second phase of 2PC */
                conn.commit(xid);
            }
        } catch (AGHttpException e) {
            tryThrowAsXAException(e);
        }
    }

    @Override
    public void forget(Xid xid) throws XAException {
        /*
         * We shouldn't receive this call since the server doesn't do heuristic
         * operations yet.
         */
        throw new XAException("Don't know how to forget " + xid + " yet");

    }

    /**
     * This method must return true if xares and 'this' correspond to the same repository,
     * regardless of how the connection is made or who is accessing.  We compare storeIDs to
     * make that determination.
     *
     * @param xares The XAResource to compare with.
     * @return true if xares and 'this' correspond to the same repository,
     *         regardless of how the connection is made or who is accessing.
     */
    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        if (logger.isDebugEnabled())
            logger.debug("isSameRM comparing us (" + this + " with " + xares);

        if (xares instanceof AGXAResource) {
            // Return true if the storeIDs are the same
            return conn.getStoreID() == ((AGXAResource) xares).conn.getStoreID();
        } else {
            return false;
        }
    }

    /**
     * Obtains a list of prepared transaction branches from a resource manager.
     * The transaction manager calls this method during recovery to obtain the
     * list of transaction branches that are currently in prepared or
     * heuristically completed states.
     *
     * @param flag One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS.
     *        TMNOFLAGS must be used when no other flags are set
     *        in the parameter.
     * @return An array of zero or more XIDs of the transaction branches that are currently
     *         in a prepared or heuristically completed state. If an error occurs during the operation,
     *         the resource manager should throw the appropriate XAException.
     *
     * @throws XAException An error has occurred. Possible values are XAER_RMERR,
     *                     XAER_RMFAIL, XAER_INVAL, and XAER_PROTO.
     */
    @Override
    public Xid[] recover(int flag) throws XAException {
        switch (flag) {
        case TMSTARTRSCAN:
            if (logger.isDebugEnabled())
                logger.debug("AGXAResource.recover() called with TMSTARTRSCAN on " + this);

            Xid[] res;
            try {
                res = conn.getPreparedTransactions();

                if (logger.isDebugEnabled()) {
                    logger.debug("--> Returning " + res.length + " XIDs");
                    for (Xid xid : res) {
                        logger.debug("----> " + xid);
                    }
                }

                return res;
            } catch (DecoderException e) {
                e.printStackTrace();
                return null;
            }
        case TMENDRSCAN:
            if (logger.isDebugEnabled())
                logger.debug("AGXAResource.recover() called with TMENDRSCAN");
            // We already returned everything we know about in the TMSTARTRSCAN
            // case.
            return new Xid[0];
        case TMNOFLAGS:
            if (logger.isDebugEnabled())
                logger.debug("AGXAResource.recover() called with TMNOFLAGS");
            // We already returned everything we know about in the TMSTARTRSCAN
            // case.
            return new Xid[0];
        default:
            if (logger.isDebugEnabled())
                logger.debug("AGXAResource.recover() called with unknown flag: " + flag);
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        // false indicates that we don't support setting the transaction timeout
        return false;
    }
}
