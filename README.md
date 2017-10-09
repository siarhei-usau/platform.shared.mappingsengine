# team.spike.sdc-mappings-engine

Spike Team mappings engine for StreamSets

## Building

With Java 8 installed, execute:

```bash
./gradlew clean build distTar
```

(on Windows, run `gradlew.bat` instead)

## Installation

_(run a build first, see above)_

Download and isntall StreamSets Data Collector (SDC) `2.7.1.1`

After building the sdc-mappings-engine project, copy the 
resulting `*.tgz` file from `build/distributions into your 
SDC install directory at `${sdc-install-dir}/user-libs` and
untar the archive there, which will create a subdirectory.

Note the name of this directory so that you can edit the security
policy for SDC to allow this plugin to access disk and other
services of the host machine.  

Edit file `${sdc-install-dir}/etc/sdc-security.policy` and add this
section to the end:

```
grant codebase "file://${sdc.dist.dir}/user-libs/mappings-prototype-1.0.1-SNAPSHOT/-" {
  permission java.security.AllPermission;
};
```

Note that you should substitute the correct directory name for the tgz you
unarchived into the first line of that file.  If you want to grant more narrow
permissions, you can add just the settings needed, such as access to read files
from a data input directory:

```
grant codebase "file://${sdc.dist.dir}/user-libs/mappings-prototype-1.0.1-SNAPSHOT/-" {
   permission java.io.FilePermission "home/me/test/data/*", "read";
};
```

Restart SDC when done.

Note, any version number changes would require this policy file to be updated to match the directory 
name of the installed plugin.

## Using the Mapper in an SDC Pipeline

_(you can import the `sample/Mappings Example.json` from this repo into SDC and then edit the settings
mentioned below to be correct for your machine, or start from scratch after you read the base SDC
documentation)_

With the mapper engine installed, create a new pipeline.  

Add an Origin of type `Directory` and configure the `Files` tab to have a correct input folder,
and File Name Pattern Mode to `Glob` with a filename pattern of something like `*.xml`.  Set
the data format to `Whole File`.

Add a processor from that node of type `XML2JSON Canonical Processor ...`.  On the Mappings tab,
you can leave the Raw XML Field to the default value of `/fileRef` (which will pick up the 
file passed from the Directory origin), and add any mappings instructions to the `Mappings JSON` 
field (leave blank for first test).

Add a Destination of Local Filesystem.  On the Output Files tab, set FileType to `Text Files`, and
set Files Suffix to `json`, with Directory Template to some directory you wish to write the output
files, and Max Records per File to `1`.  On the Data Format tab, set Data Format to `Text`, 
Text Field Path to `/json`.

You can now hit the Preview button and view the records at each stage of the process.  Or run the 
pipeline to do a larger conversion.

The `XML2JSON Canonical Processor` can work with any source that either creates a `/fileRef` field
or that adds the full XML as one string field, just change Raw XML Field setting to the input field
name.  It must be of type `String` or of type `FileRef`.

## Using the Mapper from the Command-Line

**TBD**

## Mapping Instructions / Configuration

**TBD**