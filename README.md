# platform.shared.mappingsengine

Platform - Mappings Engine

Confluence page: https://sslvpn.epnet.com/display/entarch/,DanaInfo=confluence+Mappings+Engine+-+platform.shared.mappingsengine

## Project Architecture

The Mappings Engine is broken into 4 code modules:

|module|description|
|------|-----------|
|mappings-engine-xml-reader|stand-alone module that converts XML to JSON with a few configurable adjustments including setting which paths are allowed to have embedded tags (which will be retained as text), which nodes should enforce single value, and which nodes can elevate their text node up to the parent.  This builds a JSON object in memory using a provided JsonProvider, which defaults to Jackson.  This is well covered by unit tests, but has not been tested against malformed documents.|
|mapping-engine|Runs a series of transforms on the JSON object in memory using JsonPath that accesses the JSON document via a JsonProvider. This includes a custom target pathing library to allow for transformations to a document from relative paths (relative to the queried nodes).  See the `PathUtils` and `TestPathUtils` for more on this topic, as it is the key to how transformers can apply changes. |
|mapping-engine-cli|A basic testing tool that runs a single input file and outputs JSON to screen and optionally to a file.|
|mapping-engine-streamsets-plugin|A StreamSets plugin that incorporates the xml reader and mappings engine to transform XML into JSON while applying the mappings.  It reads from an XML from either a text field or StreamSets `fileRef` and writes JSON back to another text field.  This results of this can be parsed into a record using StreamSets JsonParserProcessor or used as-is as text to pass it to another system.|

## Building

To build the distributions for both StreamSets Plugin and the Test CLI, run a top level build:

With Java 8 installed, execute:

```bash
./gradlew clean build distTar
```

(on Windows, run `gradlew.bat` instead)

The distributions can be found in:

| | |
|---|---|
|StreamSets|./mapping-engine-streamsets-plugin/build/distributions|
|Test CLI|./mapping-engine-cli/build/distributions|

## Developing within an IDE (Eclipse or Intellij IDEA)

For development, the Kotlin and Lombok plugins are required for your IDE.  These are available for Intellij IDEA and 
possibly other IDE's.

## Testing the Mapper from the Command-Line

You can either unzip/untar the distribution created by the main build, and run the script in the `bin` directory, or you
can use Gradle to install a copy within the build.

Build the CLI and install:

```bash
./gradlew :mapping-engine-cli:install
```

Which installs a copy to `./mapping-engine-cli/build/install/mapping-engine-cli`.

Then run the tool on Mac/Linux, execute this command from the install directory:

```bash
./bin/mapping-engine-cli  --input sample/1920_rs_54172.xml --config sample/mappings-example.json 
```

or on Windows, excute this command from the install directory:

```bash
bin\mapping-engine-cli.bat  --input sample\1920_rs_54172.xml --config sample\mappings-example.json 
```

Note:  you will need to fix the paths to the input and configuration files to be valid for your machine and the files you want to process.

Command-line options:

|Option|Description|
|------|-----------|
|--input|XML or JSON input file (required)|
|--config|Mappings configuration file (required)|
|--output|Optional output file (output is to stdout as well|
|--quiet (or -q)|Do not output anything other than error messages|


_(TODO: place releases into GitHub releases)_

## Installation into StreamSets Data Collector (SDC)

_(run a build first, see above)_

Download and isntall StreamSets Data Collector (SDC) `2.7.1.1`

After building the sdc-mappings-engine project:
1. copy the resulting `*.tgz` file from `build/distributions into your 
SDC install directory at `${sdc-install-dir}/user-libs` 

2. and untar the archive there, which will create a subdirectory. Note the name of this directory so that you can edit the security
policy for SDC to allow this plugin to access disk and other services of the host machine.  

3. Edit file `${sdc-install-dir}/etc/sdc-security.policy` and add this section to the end:
   
    ```
    grant codebase "file://${sdc.dist.dir}/user-libs/mapping-engine-streamsets-plugin/-" {
      permission java.security.AllPermission;
    };
    ```

    Note that you should substitute the correct directory name for the tgz you
    unarchived into the first line of that file.  If you want to grant more narrow
    permissions, you can add just the settings needed, such as access to read files
    from a data input directory:
    
    ```
    grant codebase "file://${sdc.dist.dir}/user-libs/mapping-engine-streamsets-plugin/-" {
       permission java.io.FilePermission "home/me/test/data/*", "read";
    };
    ```

4. Restart SDC when done.

Any version number changes would require this policy file to be updated to match the directory 
name of the installed plugin.

**TODO:** Create a stage library, and place in internal repo so that this can be installed via that model. 

## Using the Mapper in an SDC Pipeline

_(you can import the `sample/Mappings Example.json` from this repo into SDC and then edit the settings
mentioned below to be correct for your machine, or start from scratch after you read the base SDC
documentation)_

With the mapper engine installed:

1. create a new pipeline.  

2. Add an Origin of type `Directory` and 
    * Set the `Files` tab to have a correct input folder
    * Set File Name Pattern Mode to `Glob` with a filename pattern of something like `*.xml`
    * Set the data format to `Whole File`.

3. Add a processor from that node of type `XML2JSON Canonical Processor ...`.  
    * On the Mappings tab, you can leave the Raw XML Field to the default value of `/fileRef` (which will pick up the 
      file passed from the Directory origin)
    * Add any mappings instructions to the `Mappings JSON` field (leave blank for first test).

4. Add a Destination of Local Filesystem.  
    * On the Output Files tab, set FileType to `Text Files`
    * Set Files Suffix to `json`, with Directory Template to some directory you wish to write the output files
    * Set Max Records per File to `1`
    * On the Data Format tab, set Data Format to `Text` and Text Field Path to `/json`.

You can now hit the Preview button and view the records at each stage of the process.  Or run the 
pipeline to do a larger conversion.

The `XML2JSON Canonical Processor` can work with any source that either creates a `/fileRef` field
or that adds the full XML as one string field, just change Raw XML Field setting to the input field
name.  It must be of type `String` or of type `FileRef`.


## Mapping Instructions / Configuration

see the confluence page for information about mappings configuration:

https://confluence/display/entarch/,DanaInfo=confluence+Mappings+Engine+-+platform.shared.mappingsengine

Sample mappings and input/output files are in [./sample] directory of this repository.

