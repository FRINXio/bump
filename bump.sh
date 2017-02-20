#!/bin/bash
set -e

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
pomfile=$1
if [[ $? -ge 1 ]]; then
    shift
fi
SUM=$(find "$DIR" -type f -name '*.java'  -o -name 'pom.xml' | xargs cat | md5sum | cut -d' ' -f1)
SUMFILE="$DIR/target/md5sum"
SKIP_BUILD=false
if [ -f $SUMFILE ]; then
    if [ $(cat $SUMFILE) == $SUM ]; then
        SKIP_BUILD=true
    fi
fi
if [ $SKIP_BUILD == false ]; then
    mvn -f "$DIR/pom.xml" clean package
    echo $SUM > $SUMFILE
fi

java -jar $JAVA_OPTS "$DIR/target/bump-1.0.0-SNAPSHOT.jar" "$@"

