package com.franz.failures;

public class NoSessionFail {

    public static void main(String[] args) throws Exception {
        String ns = "http://foo.com/";

        final AGAssist aga = new AGAssist(null, null,
                AGAssist.getenv("AGRAPH_PORT", "10035"), null,
                "myrepo", "test", "xyzzy");
        final int[] atargs = new int[1];

        aga.conn.clear();  // remove all triples

        for (int i = 0; i < 100; i = i + 1) {
            atargs[0] = i;
            aga.handleFail(() -> {
                addATriple(aga, atargs);
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
