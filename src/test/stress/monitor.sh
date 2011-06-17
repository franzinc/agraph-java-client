#!/bin/sh

if test "$1" = start; then
    echo "monitor.sh: Starting monitoring for $2..."
    nmon -t -C agraph -F "$2.nmon" -s 10
elif test "$1" = end; then
    echo "monitor.sh: Ending monitoring."
    killall -USR2 nmon
else
    echo $0: unknown first argument: $1
fi
