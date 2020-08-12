package com.franz.failures;

import com.franz.agraph.pool.AGConnProp;

/**
 * Three connections to a repo, all sharing the same connection pool
 * 
 * @author jkf
 */
public class ManyPoolFail {

    public static void main(String[] args) throws Exception {

        final int numberWorkers = 3;

        AGAssist agas[] = new AGAssist[numberWorkers];

        AGAssist aga = new AGAssist(null, null,
                AGAssist.getenv("AGRAPH_PORT", "10035"), null,
                "myrepo", "test", "xyzzy");

        aga.addConnectionPool(AGConnProp.Session.TX, false, 3, 40);
        aga.conn.clear();
        aga.close();

        aga.println("pool built");
        aga.sleep(20000);
        aga.println("get connection");

        for (int ww = 0; ww < numberWorkers; ww++) {
            agas[ww] = worker(aga, null);
        }

        for (int ww = 0; ww < numberWorkers; ww++) {
            worker(aga, agas[ww]);
        }

        for (int ww = 0; ww < numberWorkers; ww++) {
            agas[ww].close();
        }

        System.out.println("closing pool");
        aga.pool.close();
        System.out.println("done now");

    }

    static AGAssist worker(AGAssist mainaga, AGAssist thisaga) {
        if (thisaga == null) {
            thisaga = new AGAssist(mainaga);
        }

        final int[] atargs = new int[1];
        final AGAssist aga = thisaga;

        for (int i = 0; i < 3; i = i + 1) {
            atargs[0] = i;
            AGAssist.println("will add triple " + i);
            aga.handleFail(() -> {
                aga.conn.rollback();
                AGAssist.println("autocommit is " + aga.conn.isAutoCommit());
                AGAssist.println("sessions is " + aga.sessions);
                addATriple(aga, atargs);
                AGAssist.println("did 1 of 4");
                // aga.sleep(4000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                AGAssist.println("did 2 of 4");
                // aga.sleep(4000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                AGAssist.println("did 3 of 4");
                // aga.sleep(4000);
                atargs[0] = atargs[0] * 10;
                addATriple(aga, atargs);
                aga.conn.commit();

            });

            System.out.println("Added triple " + i);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return thisaga;

    }

    private static void addATriple(AGAssist aga, int[] atargs) throws Exception {
        String ns = "http://fail.org/";

        aga.addTriple(aga.resourceString("foo", ns), aga.resourceString("bar", ns), "do-" + atargs[0]);

    }
}
