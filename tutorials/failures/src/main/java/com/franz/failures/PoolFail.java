package com.franz.failures;

import com.franz.agraph.pool.AGConnProp;

/**
 * Usng a connection pool to connect to a repo
 */

public class PoolFail {

    public static void main(String[] args) throws Exception {

        final AGAssist aga = new AGAssist(null, null,
                AGAssist.getenv("AGRAPH_PORT", "10035"), null,
                "myrepo", "test", "xyzzy");
        final int[] atargs = new int[1];

        aga.addConnectionPool(AGConnProp.Session.TX, false, 2, 5);
        aga.conn.clear();  // remove all triples

        for (int i = 0; i < 100; i = i + 1) {
            atargs[0] = i;
            AGAssist.println("will add triple " + i);
            aga.handleFail(() -> {
                aga.conn.rollback();
                AGAssist.println("autocommit is " + aga.conn.isAutoCommit());
                addATriple(aga, atargs);
                AGAssist.println("did 1 of 4");
                aga.sleep(4000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                AGAssist.println("did 2 of 4");
                aga.sleep(4000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                AGAssist.println("did 3 of 4");
                aga.sleep(4000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                aga.conn.commit();

            });

            System.out.println("Added triple " + i);

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private static void addATriple(AGAssist aga, int[] atargs) throws Exception {
        String ns = "http://fail.org/";

        aga.addTriple(aga.resourceString("foo", ns), aga.resourceString("bar", ns), "do-" + atargs[0]);

    }

}
