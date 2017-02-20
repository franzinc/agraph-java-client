Version rewriter app
====================

This directory contains a simple Java application that adjusts version 
numbers in all relevant files when the client is released.

It can operate in three modes:

1. Strip SNAPSHOT from the version number and also update the README.
2. Increase the version number and ensure that the -SNAPSHOT qualifier is used.
3. Set the version to an arbitrary number with the -SNAPSHOT qualifier.

To compile the application, type

    mvn package
    
Then run it with 

    java -jar target/version-rewriter.jar [. [NEW-VERSION]]
    
If a version number is provided the app will run in mode 3. Otherwise it 
will run in mode 1 if the current version is a snapshot and in mode 2 if 
it is a release. It is also possible to force mode 2 by using
"next-snapshot" as the NEW-VERSION argument.

Note the '.' in the arguments list - this should be a path to the source 
directory of version-rewriter, it is used to find the files to be 
modified.
