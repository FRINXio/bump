# bump
This tool can be used to parse and rewrite versions of
frinx/opendaylight dependencies. Files containing frinx dependencies,
like pom.xml, features.xml files will be processed, but there is nothing
maven or karaf specific in this tool.

## Dependencies
Bump tool requires git and sed to be installed.

## Terminology
[OSGi versioning]( https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf ) allows version
to be comprised of four elements:
* major
* minor
* micro
* qualifier - usually not used but it is utilized by frinx ODL artifacts as
well as this tool. It is used to distinguish between versions to be transformed and everything else.
* suffix - qualifier or uniqe part of qualifier.

### Example
```
'1.2.3.4-frinxodl'
```
Suffix is frinxodl, 1.2.3.4 is major.minor.micro.qualifier .

## Two modes of operation
* simple - transforms all matching files line by line. Only lines that contain qualifier will be transformed.
* flipLastCommit - only transforms lines that were added or modified in last commit. Creates new commit.

## Usage

### Preparing for transformation
Bump works on folder level, and transforms all matching files. Therefeore
when bumping versions of maven projects, it is advised to clean
generated files first:
```
mvn clean
# or
find . -name target -type d -exec rm -rf {} \;
```

### Displaying help
```
bump.sh -h
```

### Dropping -SNAPSHOT
```
bump.sh simple --suffix frinxodl --snapshot drop
```
Example: 1.2.3.4-frinxodl-SNAPSHOT -> 1.2.3.4-frinxodl
### Increasing qualifier version
```
bump.sh simple --suffix frinxodl --bump qualifier
```
Example: 1.2.3.4-frinxodl -> 1.2.3.5-frinxodl
### Advanced - using sed to alter version
Bump can call sed to transform the version before or after the parsing and bumping:
```
bump.sh simple --suffix frinx --snapshot add --preprocess-sed s/rc1-frinx/frinx/ --postprocess-sed s/frinx/rc2-frinx/
```
Example: 1.2.3.rc1-frinx -> 1.2.3.rc2-frinx-SNAPSHOT
