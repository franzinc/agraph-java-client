# Handling Failures in a Transaction

## Introduction
When writing a client server application the client must be prepared
to handle the case of the server not responding.  The network can
break or the server's machine can go down, either intentionlally
or unintentionally

The basic idea is that one must make all of the work in a transacion
be repeatable.  That is if there is a problem talking to the
database server at any point in the transaction, you can start again
executing steps from the beginning.

This will require the ability to reset the non-persistent state inside
the application as well because you want to repeat the exact same steps
until the commit at the end of the transaction is executed and returns
successfully.

## Scenarios


Following are  different scenarios which require different methods of handling failures.
We supply one java source file for each scenario.  When run these classes create
a database and start to fill it slowly.  You can try killing the AllegroGraph server
and restarting it to see how the code for each scenario pauses its work
when the server is non-responsive
and when
the server is running resumes where it left off.

### no session
If you are not using sessions then every command to the server is committed immediately.   In this case if the server doesn't respond acknowleding your command you should just try it again (with a delay between tries to give the problem with the server a chance to resolve itself).  The file `NoSessionFail.java` shows this strategy.

### session
If the server doesn't respond you need to reconnect to the server and try again.  For safety it's a good idea to rollback before starting your transaction some parts of the transaction are still recorded on the server but not committed yet. You want to clear those out.  The file `SessionFail.java` shows this.

### connection pool
if your application repeatedly creates a connection, does a transaction, and then closes the connection you would be advised to request a connection pool as this will save a lot of work on the server and thus time in your application.
Creating a session on the server means creating a new Unix process.
Sessions pools hold onto sessions that were asked to be closed so that they can be reused.
With a session pool responding to a server failure is a bit different.
See `PoolFail.java` for a single user using a connection pool and `ManyPoolFail.java` for
a simulation of three users using the same session pool.
As with the other scenarios the `AGAssist.java` file shows how to rerun the transaction on a failure.

### load balancer
When a node of  load balancer fails the load balancer automatically adjusts and sends the next request to a diffeent backend.   The `LBfail.java` file tests this but it requires that you setup a load balancer to test it and that's not done by this code.


