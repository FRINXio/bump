#!/bin/bash

set -e

#FIXME: md5 of everything except target folder
GROUP_ID="tbd"
VERSION=1.0.0-SNAPSHOT

PROGNAME="$0"
if [ "$$1" = "" ]; then
    echo "$PROGNAME: Must supply name of script as first argument."
    exit 1
fi
SOURCE_PATHNAME=$(readlink -f "$1")
SOURCE_FILENAME=$(basename "$SOURCE_PATHNAME")
# endsWith .java
if [[ "$SOURCE_FILENAME" != *.java ]]; then
    echo "$PROGNAME: File name must end with .java"
    exit 1
fi


SOURCE_DIRECTORY="."
TEST_DIRECTORY="."
SOURCE_DIR=$(dirname "$SOURCE_PATHNAME")

CLASS_NAME="${SOURCE_FILENAME::-5}"
CLASS_FQNAME="${CLASS_NAME}"

if [[ $SOURCE_PATHNAME == *"/src/main/java/"* ]]; then
    SOURCE_DIRECTORY=""
    SOURCE_DIR="${SOURCE_PATHNAME%/src/main/java/*}"
    if [ -d $SOURCE_DIR/src/test ]; then
        TEST_DIRECTORY=""
    else
        TEST_DIRECTORY='${project.build.sourceDirectory}'
    fi
    LOCAL_DIR="${SOURCE_PATHNAME#*/src/main/java/}"
    PACKAGE_NAME="`dirname $LOCAL_DIR | sed -r 's/([\/])/./g'`"
    # remove last package - should correspond with artifact name
    GROUP_ID="`dirname $LOCAL_DIR | xargs dirname | sed -r 's/([\/])/./g'`"
    CLASS_FQNAME="${PACKAGE_NAME}.${CLASS_NAME}"
fi

# for sh script
UNDERSCORE_NAME=$(echo $CLASS_NAME | sed -r 's/([a-z0-9])([A-Z])/\1_\L\2/g' | tr '[:upper:]' '[:lower:]')
# for artifact name
DASH_NAME=$(echo $UNDERSCORE_NAME | sed -r 's/([_])/-/g')
POM_FILENAME="$SOURCE_DIR/pom.xml"

echo "Generating $POM_FILENAME"
POM_BEFORE="<?xml version='1.0' encoding='UTF-8'?>
<project xmlns='http://maven.apache.org/POM/4.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
     xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd'>
<modelVersion>4.0.0</modelVersion>

<groupId>$GROUP_ID</groupId>
<artifactId>$DASH_NAME</artifactId>
<version>$VERSION</version>
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>

<dependencies>
"
echo $POM_BEFORE > $POM_FILENAME

cat $SOURCE_PATHNAME | while read line
do
    if [[ $line == //\ m2:* ]]; then
        # // m2:junit:junit:jar:4.12
        arrIN=(${line//:/ })
        GROUP_ID=${arrIN[2]}
        ARTIFACT_ID=${arrIN[3]}
        TYPE=${arrIN[4]}
        VERSION=${arrIN[5]}

        echo "<dependency>
        <groupId>$GROUP_ID</groupId>
        <artifactId>$ARTIFACT_ID</artifactId>
        <version>$VERSION</version>
        <type>$TYPE</type>
    </dependency>" >>  $POM_FILENAME

    else
        break
    fi
done


POM_AFTER="</dependencies>

<build>"
if [[ -n $SOURCE_DIRECTORY ]]; then
   POM_AFTER+="
       <sourceDirectory>$SOURCE_DIRECTORY</sourceDirectory>
   "
fi
if [[ -n $TEST_DIRECTORY ]]; then
   POM_AFTER+="
        <testSourceDirectory>$TEST_DIRECTORY</testSourceDirectory>
   "
fi
POM_AFTER+="
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.5.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>2.4.3</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <createDependencyReducedPom>false</createDependencyReducedPom>
                        <transformers>
                            <transformer
                                    implementation='org.apache.maven.plugins.shade.resource.ManifestResourceTransformer'>
                                <mainClass>$CLASS_FQNAME</mainClass>
                            </transformer>
                        </transformers>
                        <filters>
                            <filter>
                                <artifact>*:*:*:*</artifact>
                                <excludes>
                                    <exclude>META-INF/*.SF</exclude>
                                    <exclude>META-INF/*.DSA</exclude>
                                    <exclude>META-INF/*.RSA</exclude>
                                </excludes>
                            </filter>
                        </filters>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>1.4.0</version>
            <executions>
                <execution>
                    <id>run</id>
                    <goals>
                        <goal>java</goal>
                    </goals>
                    <configuration>
                        <mainClass>${CLASS_FQNAME}</mainClass>
                    </configuration>
                </execution>
                <execution>
                    <id>md5</id>
                    <goals>
                        <goal>exec</goal>
                    </goals>
                    <phase>prepare-package</phase>
                    <configuration>
                        <executable>/bin/bash</executable>
                        <commandlineArgs>-c \"find \${project.basedir} -type f -name '*.java'  -o -name 'pom.xml' | xargs cat | md5sum | cut -d' ' -f1 > \${project.build.directory}/md5sum\"</commandlineArgs>
                    </configuration>
                </execution>
            </executions>
        </plugin>

    </plugins>

    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.19.1</version>
                <configuration>
                    <includes>
                        <include>**/*.java</include>
                    </includes>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>

</project>
"
echo $POM_AFTER >> $POM_FILENAME
xmllint --format $POM_FILENAME -o $POM_FILENAME



EXEC_FILE_NAME="$UNDERSCORE_NAME.sh"
EXEC_FILE_PATH="$SOURCE_DIR/$EXEC_FILE_NAME"
echo "Generating $EXEC_FILE_NAME"

echo "#!/bin/bash
set -e

DIR=\$( cd \"\$( dirname \"\${BASH_SOURCE[0]}\" )\" && pwd )
pomfile=\$1
if [[ \$? -ge 1 ]]; then
    shift
fi
SUM=\$(find \"\$DIR\" -type f -name '*.java'  -o -name 'pom.xml' | xargs cat | md5sum | cut -d' ' -f1)
SUMFILE=\"\$DIR/target/md5sum\"
SKIP_BUILD=false
if [ -f \$SUMFILE ]; then
    if [ \$(cat \$SUMFILE) == \$SUM ]; then
        SKIP_BUILD=true
    fi
fi
if [ \$SKIP_BUILD == false ]; then
    mvn -f \"\$DIR/pom.xml\" clean package
    echo \$SUM > \$SUMFILE
fi

java -jar \$JAVA_OPTS \"\$DIR/target/${DASH_NAME}-${VERSION}.jar\" \"\$@\"
" > $EXEC_FILE_PATH
chmod u+x $EXEC_FILE_PATH

DEBUG_EXEC_FILE_PATH="$SOURCE_DIR/$UNDERSCORE_NAME-debug.sh"
echo "Generating $DEBUG_EXEC_FILE_PATH"

echo "#!/bin/bash
set -e

DIR=\$( cd \"\$( dirname \"\${BASH_SOURCE[0]}\" )\" && pwd )
JAVA_OPTS=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005\" \"\$DIR/$EXEC_FILE_NAME\" \"\$@\"
" > $DEBUG_EXEC_FILE_PATH
chmod u+x $DEBUG_EXEC_FILE_PATH
