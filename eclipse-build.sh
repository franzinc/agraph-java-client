#!/bin/bash -eEu

# Requires eclipse executable on PATH
# with the eclipse-jdt package
# minimum version 3.3.

ECLIPSE_WORKSPACE=$1
EXPECTED_FILE=$2

ECLIPSE_BUILD="eclipse -nosplash -data $ECLIPSE_WORKSPACE -application org.eclipse.jdt.apt.core.aptBuild"

# exec eclipse multiple times until it builds the expected file.
# Usually 2 or 3 times is sufficient.
# Eclipse doesn't error if the build fails, so manually check for output.

for I in 1 2 3 4
do
    echo EXEC: $ECLIPSE_BUILD
    OUTPUT=$($ECLIPSE_BUILD)
    echo OUTPUT: $OUTPUT
    if [ -f $EXPECTED_FILE ] ; then
        exit 0
    fi
done

echo "Expected output file not found: " $EXPECTED_FILE >&2
exit 1
