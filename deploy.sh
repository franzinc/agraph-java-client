#!/bin/bash

# This script is invoked by make deploy
# It packages the Java client and uploads it to OSSRH.
# The final release (to Maven Central) still has to be approved through
# the web interface at https://oss.sonatype.org/
# To make the upload possible you need:
#   - OSSRH credentials, configured in ~/.m2/settings.xml,
#     see http://books.sonatype.com/nexus-book/reference/usertoken.html#_accessing_and_using_your_user_tokens
#   - GPG key for support@franz.com. The script will prompt
#     for the passphrase for this key.
# Note that release versions (i.e. those without the -SNAPSHOT modifier)
# cannot be changed after they're approved and uploaded to the central
# repository. This script will not allow an already released version
# to be uploaded again. It is possible to upload a version more than once
# if it is a snapshot or if it has not been approved yet.

set -e

# Name of the GPG key used to sign releases.
KEY_NAME=support@franz.com

# URL where the deployed versions are visible.
DEPLOYMENT_URL=https://oss.sonatype.org/content/groups/public/com/franz/agraph-java-client

# URL used to check OOSRH credentials.
NEXUS_CHECK_URL=https://oss.sonatype.org/service/local/staging/profiles

# Used to accumulate options for the final mvn call
# Skip pre-release tests by default, post-release tests will still be run.
MVN_OPTS=-DskipTests=true

function info () {
    echo $@ >&2
}

function err () {
    echo $@ >&2
}

# Check if the GPG key is available
if ! gpg -K "${KEY_NAME}" > /dev/null; then
    err "You must have a key named ${KEY_NAME} in your GPG keyring."
    exit 1
fi

# Check if OSSRH credentials are configured.
# This does not check if the user has the right permissions to deploy
# the artifact.
if ! mvn wagon:exist -Dwagon.serverId=ossrh -Dwagon.url=${NEXUS_CHECK_URL}; then
    cat <<EOF >&2
Cannot access oss.sonatype.org Please add proper credentials to the
<servers> section in ~/.m2/settings.xml.

If you do not have a ~/.m2/settings.xml file, copy the default one
from MAVEN_HOME/conf/settings.xml. On AG development machines the full
path is /usr/share/apache-maven/conf/settings.xml .

The credentials should look like this: 

<server>
  <id>ossrh</id>
  <username>your-ossrh-username</username>
  <password>your-ossrh-password</password>
</server>

Consider replacing the username and password with a token, see
http://books.sonatype.com/nexus-book/reference/usertoken.html#_accessing_and_using_your_user_tokens .
EOF
    exit 1
fi

VERSION=$(./version.sh)

info "Preparing version ${VERSION} for upload."

if [[ ${VERSION} =~ -SNAPSHOT$ ]]; then
    info "Uploading a SNAPSHOT version."
else
    info "Uploading a RELEASE version." 
    info "${DEPLOYMENT_URL}/${VERSION}"
    if curl --fail --silent --head "${DEPLOYMENT_URL}/${VERSION}" > /dev/null; then
        err "Version ${VERSION} already exists and cannot be overwritten."
        exit 1
    fi
fi

read -s -p "Deployment key passphrase: " KEY_PASSWORD
echo

mvn clean deploy -P release ${MVN_OPTS} "-Dgpg.passphrase=${KEY_PASSWORD}"
