# team.spike.sdc-mappings-engine

Spike Team mappings engine for StreamSets

## Building

With Java 8 installed, execute:

```bash
./gradlew clean build distTar
```

(on Windows, run `gradlew.bat` instead)

## Testing the Mapper from the Command-Line

**TBD** 

_(TODO: package as app, add command-line here)_

## Installation into StreamSets Data Collector (SDC)

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

## Mapping Instructions / Configuration

Mapping instructions are a series of settings defined in a JSON file that apply transformations to the
in memory JSON model before canonical JSON is written to the record.  There are also extra settings
that are passed in via the same configuration, for example to have some incoming XML XPaths retained
as embedded XML instead of being converted to JSON.  The structure of the JSON is:

```json
{
   "mappings": [
      {
          "type": "rename",
          "fromPath": "<jsonPath>",
          "toPath": "<jsonPath | jsonRelPath>",
          "mode": "<mergeMode>"
      },
      {
          "type": "copy",
          "fromPath": "<jsonPath>",
          "toPath": "<jsonPath | jsonRelPath>",
          "mode": "<mergeMode>"
      },
      {
          "type": "lookup",
          "lookupResource": "<uriToLookupFile",
          "filters": [
              {
                "lookup-field": "<fieldNameInLookup>",
                "lookup-value": "<jsonPathFromDocument>"
              },
              {
                "lookup-field": "<fieldNameInLookup>",
                "lookup-value": ["value1", "value2", ..., "valueN"]
              }
          ],
          "toPath": "<jsonPath>",
          "template": {
              <jsonTemplate>
          },
          "mode": "replace"
      },
      ...
   ],
   "configuration": {
       "embedLiteralXmlAtPaths": [ "XPath1", "XPath2", ..., "XPathN"]
   }
}
```

For all instruction types, the `mergeMode` is one of:  `replace` (default) or `append`.  This can
be omitted for `replace` as the default.

An element designated as `jsonPath` is a normal JsonPath statement starting with `$.` from the root.

An element designated as `jsonRelPath` is an expression starting with `@.` being relative to the current
node, and each additional `.` going up one parent level.  For example, `@.foo` is field `foo` in the 
same object as the source being copied or renamed.  But `@..foo` would be a sibling to the
field being renamed (in its parent), and `@...foo` would be up another level and so on.

An element designated as `jsonTemplate` is JSON that should be inserted at the point represented by
`toPath` within the document, for each matching row in the lookup table.  The template can use 
mustache style expressions based on the lookup elements matched, for example `{{Term_ID}}` would 
substitute for the value of the `Term_ID` field from the matching record.

The configuration section of the mappings instructions is for other settings relevant to how the
mapping engine works.  The settings are:

|Setting|Description|
|-------|-----------|
|embedLiteralXmlAtPaths|A list of XPath statements representing nodes in which their children should be retained as embeded XML and not converted to JSON.  This is done as XPath because it happens during the conversion to JSON and not after.


A sample mappings instructions is as follows:

```json
{
   "mappings": [
      {
          "type": "rename",
          "fromPath": "$.book.body[0].book-part[0].book-part-meta[0].book-part-id[0].text",
          "toPath": "@....book-pieces.id",
          "mode": "replace"
      },
      {
          "type": "copy",
          "fromPath": "$.book.body[0].book-part[0].book-part-meta[0].book-part-id[?(@.type=='doi')].text",
          "toPath": "@..doi"
      },
      {
          "type": "lookup",
          "lookupResource": "s3://bucket/prefix/lookup_w_ui.txt",
          "filters": [
              {
                "lookup-field": "AN",
                "lookup-value": "$.book[0].body[0].book-part[0].book-part-meta[0].book-part-id[0].text"
              },
              {
                "lookup-field": "Type",
                "lookup-value": ["subject", "subjectgeo"]
              }
          ],
          "toPath": "$.book.front.article-meta.article-categories.subj-group",
          "template": {
               "group_type": "ebsco_{{Type}}",
               "subject": [
                 {
                   "id": "{{Term_Id}}",
                   "text": "{{Term}}"
                 }
               ]
          },
          "mode": "append"
      }
   ],
   "configuration": {
       "embedLiteralXmlAtPaths": [ 
          "//book[*]/body[*]/book-part[*]/book-front[*]/sec",
          "//book[*]/body[*]/book-part[*]/body[*]/sec"
       ]
   }
}
```
