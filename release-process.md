AllegroGraph Java client release process
========================================

# Introduction

Java client releases are deployed to the central Maven repository. This 
is a common place from which Java build tools, such as Maven or Gradle, 
download build dependencies.

The central repository does not allow direct upload of archives. It is 
instead regularly synchronized with a few approved hosting services. The 
one that we use is called OSSRH (https://oss.sonatype.org)

The deployment process has two phases:

  * First the project is uploaded to OSSRH ("staged")
  * Then it is manually approved through the web interface of OSSRH.

Unapproved artfiacts can be downloaded directly from OSSRH, but will not 
be synced to the central repository.

Coordinates
-----------
Artifacts (i.e. files that can be downloaded) in a Maven repository are 
identified using five values ("coordinates"):

   * groupId - in our case it is going to be `com.franz`
   * artifactId: `allegrograph-java-client`
   * version -  we use version numbers of the form `MAJOR.MINOR.PATCH`,
     starting at 1.0.0. These numbers bear no relation to AllegroGraph's 
     version. See also the note regarding snapshots below.
   * packaging - File type, the default is `jar`.
   * classifier - This value is used to disambiguate between multiple 
     files produced by Maven for the same project. The default value 
     is empty and refers to the binary JAR. Other files deployed with 
     the Java client are:
       * Sources jar (`sources`)
       * Javadocs packaged in a JAR (`javadoc`)
       * Compiled tests (`test`).
        
Note that packaging and classifier are rarely mentioned, as the default 
values are almost always appropriate.

Snapshots
---------
A released Maven artifact cannot be modified in any way. Yet it is often 
desirable to use an in-development version without the need to increment 
the version number every time the code is modified. This can be achieved 
with the use of so called "snapshots". Snapshots are Maven artifacts 
that:

   * Have a version number ending in `-SNAPSHOT` (e.g. `1.0.1-SNAPSHOT`)
   * Are not subject to the "no modifications" restriction mentioned
     above.
   * Are *not* deployed to the central repository, but can be used 
     locally or directly from OSSRH.
   * When comparing versions, a snapshot is considered to be older
     than the corresponding release (`1.0.0-SNAPSHOT` < `1.0.0`).
     
During development the project is generally kept in a snapshot version.
It should only be switched to a regular version during deployment.

# Requirements

There are two basic requirements for deploying the Java client releases 
(aside from Maven and Java that are required to build it): an OSSRH 
 account and access to the GPG key used to sign releases.

## OSSRH account

OSSRH (https://oss.sonatype.org) is the service that hosts the artifacts 
that are later uploaded to the central repository. To deploy a new 
release it is necessary to have an account there, with appropriate 
permissions for the 'com.franz' group.

Credentials for this account should be added to a file named 
`~/m2/settings.xml`. If you do not have such a file, copy the default 
one from `MAVEN_HOME/conf/settings.xml`. On AG development machines the 
full path is /usr/share/apache-maven/conf/settings.xml .

The credentials should look like this: 

```
<server>
  <id>ossrh</id>
  <username>your-ossrh-username</username>
  <password>your-ossrh-password</password>
</server>
```

Consider replacing the username and password with a token, see
http://books.sonatype.com/nexus-book/reference/usertoken.html#_accessing_and_using_your_user_tokens .

## GPG key

All releases must be signed using a GPG key for `support@franz.com`.
 
This key must be available in your GPG keyring for the deployment 
process to succeed. 

# The release script

The release process is started by invoking the `release.sh` script.
It operates in the following steps:

1. The `-SNAPSHOT` qualifier is stripped from the version number in 
   the POM file and all other files that mention it.
2. The resulting code is commited to git and tagged with the version
   number.
3. The code is compiled and uploaded (staged) to OSSRH. You'll be 
   prompted for the GPG key passphrase.
4. Tests are run against the freshly uploaded JAR. This phase requires 
   a working AllegroGraph server.
5. The version number is adjusted again, by incrementing the patch level
   by one and adding the `-SNAPSHOT` qualifier back.
6. The result is again commited.

Once the script finishes successfully two steps must be performed
manually:
 
1. The two commits mentioned above must be pushed to gerrit and merged.
2. The staged release must be approved on OSSRH. To do that:
   
     * Go to https://oss.sonatype.org/ 
     * Make sure you're logged in (there is a 'Log In' button in the 
       upper right hand corner.
     * Click 'Staging Repositories.
     * Scroll down until you find an item named `comfranz-something`.
     * Select that item and then click 'Release` at the top fo the list.
         
         * You can also choose 'Drop' if something is wrong with the 
           release and you do *not* want to approve it.
     
# Adjusting major/minor version number
The release script takes care of managing the version number, but it 
only increments the patch number. After more serious API changes the 
other segments of the version might also need to be adjusted. 

See README.md in `version-rewriter/` to learn how to do this.


# Snapshot deployment

The current code can be deployed to OSSRH as a snapshot at any time. 
This can be achieved by doing `make deploy`. This will not cause the 
version number to be modified in any way, the code will just be compiled
and uploaded. Since snapshots are not synchronized to Maven Central no 
approval phase is required.