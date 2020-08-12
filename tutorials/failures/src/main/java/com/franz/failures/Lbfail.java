package com.franz.failures;

import com.franz.agraph.pool.AGConnProp;

/**
 * This class demonstrates failures when using a load balancer.
 * The creation of a load balancer is not part of this source.
 * It was tested with an AWS load balancer.  
 * 
 * This class will not run as-is.  You need to create a load 
 * balancer and replace the DNS name of the load balancer below
 * with the DNS name of your load balancer.
 * 
 * We built our load balancer from this
 * https://github.com/franzinc/agraph-examples/tree/master/clustering/terraform-elb
 * 
 * @author jkf
 */

public class Lbfail {

    public static void main(String[] args) throws Exception {

        final int numberWorkers = 6;

        AGAssist agas[] = new AGAssist[numberWorkers];

        AGAssist aga = new AGAssist(null, "ag-elb-1410477638.us-east-1.elb.amazonaws.com",
                "10035", null,
                "myrepl", "test", "xyzzy");

        // each connection will be session not in autocommit mode, thus
        // requiring a commit to save the changes.
        // The second arguement puts the pool in FIFO mode thus assuring
        // that we don't reuse the same connection object over and over
        // and as a result end up sending data to just one backend through
        // the load balancer
        aga.addConnectionPool(AGConnProp.Session.TX, false, 3, 40);
        aga.conn.clear();
        aga.close();

        aga.println("pool built");

        // the sleep allows you to see the effect of creating the pool
        // in the agraph.log file before the workers start adding triples.
        aga.sleep(10000);

        for (int ww = 0; ww < numberWorkers; ww++) {
            agas[ww] = worker(aga, null, ww);
        }

        for (int ww = 0; ww < numberWorkers; ww++) {
            worker(aga, agas[ww], ww);
        }

        for (int ww = 0; ww < numberWorkers; ww++) {
            agas[ww].close();
        }

        aga.println("closing pool");
        aga.pool.close();
        aga.println("done now");

    }

    static AGAssist worker(AGAssist mainaga, AGAssist thisaga, int wnum) {
        // the worker adds a set of triples within a transaction
        // and commits.
        // The triples are added slowly giving you a chance to kill
        // the server and see the effect.

        if (thisaga == null) {
            thisaga = new AGAssist(mainaga);
        }

        // variables used inside the lambda expression must be
        // declared final
        final int[] atargs = new int[2];
        final AGAssist aga = thisaga;

        AGAssist.println("--- worker " + wnum + " ---");
        for (int i = 0; i < 3; i = i + 1) {

            atargs[1] = i;
            AGAssist.println("will add triple group " + i);
            aga.handleFail(() -> {

                aga.conn.rollback();
                atargs[0] = atargs[1];

                addATriple(aga, atargs);
                AGAssist.println("did 1 of 4");
                aga.sleep(1000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                AGAssist.println("did 2 of 4");
                aga.sleep(1000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                AGAssist.println("did 3 of 4");
                aga.sleep(1000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                aga.conn.commit();

            });

            aga.println("Added triple group " + i);
            aga.sleep(5000);

        }

        return thisaga;

    }

    private static void addATriple(AGAssist aga, int[] atargs) throws Exception {
        String ns = "http://fail.org/";

        aga.addTriple(aga.resourceString("foo", ns), aga.resourceString("bar", ns), "do-" + atargs[0]);

    }
}
