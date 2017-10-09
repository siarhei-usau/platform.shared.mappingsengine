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

After building the sdc-mappings-engine project:
1. copy the resulting `*.tgz` file from `build/distributions into your 
SDC install directory at `${sdc-install-dir}/user-libs` 

2. and untar the archive there, which will create a subdirectory. Note the name of this directory so that you can edit the security
policy for SDC to allow this plugin to access disk and other services of the host machine.  

3. Edit file `${sdc-install-dir}/etc/sdc-security.policy` and add this section to the end:
   
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

4. Restart SDC when done.

Any version number changes would require this policy file to be updated to match the directory 
name of the installed plugin.

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

Mapping instructions are a series of settings defined in a JSON file that apply transformations to the
in memory JSON model before canonical JSON is written to the record.  There are also extra settings
that are passed in via the same configuration, for example to have some incoming XML XPaths retained
as embedded XML instead of being converted to JSON.  The structure of the JSON is:

```text
{
   "id": "<mappings ID>",
   "version": "<version>",
   "notes": "<optionalNotes>",
   "primaryKey": "<jsonPath>",
   "mappings": [
      {
          "id": "<optionalID>",
          "notes": "<optionalNotes>",
          "type": "rename",
          "fromPath": "<jsonPath>",
          "toPath": "<jsonPath | jsonRelPath>",
          "mode": "<mergeMode>"
      },
      {
          "id": "<optionalID>",
          "notes": "<optionalNotes>",
          "type": "copy",
          "fromPath": "<jsonPath>",
          "toPath": "<jsonPath | jsonRelPath>",
          "mode": "<mergeMode>"
      },
      {
          "id": "<optionalID>",
          "notes": "<optionalNotes>",
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

The `id` and `notes` fields are mostly optional and later will be used to add MetaData into StreamSets
logs or record attributes.  The `primaryKey` path statement is resolved last and should resolve to only 
one value, otherwise is an error.

An element designated as `jsonPath` is a normal JsonPath statement starting with `$.` from the root.  If
a `jsonPath` is used in a target for a mapping, for example in the `toPath` setting, then it will be 
prefixed matched against the `fromPath` and turned into a relative path downwards from any matching
prefix.  This allows the rest of the path to work within the same array indexes for the matching prefix
as the object on which the mapping is using as a source.

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
   "id": "rs-mappings",
   "version": "1.0.0",
   "primaryKey": "$.book.body[0].book-part[0].book-part-meta[0].doi",
   "mappings": [
      {
          "type": "rename",
          "fromPath": "$.book.body[0].book-part[0].book-part-meta[0].book-part-id[*].value",
          "toPath": "@..id",
          "mode": "replace"
      },
      {
          "type": "copy",
          "fromPath": "$.book.body[0].book-part[0].book-part-meta[0].book-part-id[?(@.pub-id-type=='doi')].id",
          "toPath": "@...doi"
      },
      {
          "type": "lookup",
          "lookupResource": "s3://bucket/prefix/lookup_w_ui.txt",
          "filters": [
              {
                "lookup-field": "AN",
                "lookup-value": "$.book[0].body[0].book-part[0].book-part-meta[0].book-part-id[0].value"
              },
              {
                "lookup-field": "Type",
                "lookup-value": ["subject", "subjectgeo"]
              }
          ],
          "toPath": "$.book.front[0].article-meta[0].article-categories[0].subj-group[0]",
          "template": {
               "group_type": "ebsco_{{Type}}",
               "subject": [
                 {
                   "id": "{{Term_Id}}",
                   "value": "{{Term}}"
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

```

## Sample Converted JSON file (after XML conversion, pre-mapping)

```json
{
  "book" : {
    "book-series-meta" : [ {
      "book-id" : [ {
        "value" : "10.3331/sp_ency",
        "pub-id-type" : "doi"
      } ]
    } ],
    "book-meta" : [ {
      "book-id" : [ {
        "value" : "10.3331/1920_rs",
        "pub-id-type" : "doi"
      } ],
      "book-title-group" : [ {
        "book-title" : {
          "value" : "Salem Press Encyclopedia"
        },
        "alt-title" : [ {
          "value" : "G97S",
          "alt-title-type" : "mid"
        }, {
          "value" : "10.3331/1920",
          "alt-title-type" : "derived_from_id"
        }, {
          "value" : "The Twenties in America",
          "alt-title-type" : "original"
        } ]
      } ],
      "publisher" : [ {
        "publisher-name" : {
          "value" : "Salem Press"
        }
      } ],
      "pub-date" : [ {
        "year" : {
          "value" : "2012"
        },
        "pub-type" : "original"
      }, {
        "year" : {
          "value" : "2016"
        },
        "pub-type" : "update"
      } ]
    } ],
    "body" : [ {
      "book-part" : [ {
        "book-part-meta" : [ {
          "book-part-id" : [ {
            "value" : "10.3331/1920_rs_54171",
            "pub-id-type" : "doi"
          } ],
          "title-group" : [ {
            "title" : {
              "value" : "Academy Awards begin"
            }
          } ],
          "contrib-group" : [ {
            "contrib" : [ {
              "name" : [ {
                "given-names" : {
                  "value" : "Leon"
                },
                "x" : [ {
                  "value" : null,
                  "xml:space" : "preserve"
                } ],
                "surname" : {
                  "value" : "Lewis"
                },
                "name-style" : "western"
              } ],
              "contrib-type" : "author",
              "type" : "simple"
            } ]
          } ],
          "abstract" : [ {
            "p" : {
              "value" : "The first awards ceremony held by the Motion Picture Academy of Arts and Sciences took place in 1929, a mere two years after the first “talking” pictures were released, providing both prestige and publicity for the already rapidly expanding filmmaking industry."
            },
            "abstract-type" : "short"
          } ],
          "custom-meta-wrap" : [ {
            "custom-meta" : [ {
              "meta-name" : {
                "value" : "Derived from"
              },
              "meta-value" : {
                "value" : "10.3331/1920_0001"
              },
              "type" : "simple"
            } ]
          } ]
        } ],
        "book-front" : [ {
          "sec" : [ {
            "value" : "<title/>\n<p>\n   <bold>Last reviewed:</bold> February 2016</p>",
            "sec-type" : "reviewed"
          } ]
        } ],
        "body" : [ {
          "sec" : [ {
            "value" : "<title/>\n<p>\n   <italic>The first awards ceremony held by the Motion Picture Academy of Arts and Sciences took place in 1929, a mere two years after the first “talking” pictures were released, providing both prestige and publicity for the already rapidly expanding filmmaking industry.</italic>\n</p>\n<p>In the wake of a 1926 union agreement between film studios and technicians, <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88801928\" xlink:type=\"simple\">Louis B. Mayer</related-article>, the production chief of <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88960862\" xlink:type=\"simple\">Metro-Goldwyn-Mayer (MGM)</related-article> and one of the most powerful people in Hollywood, sought to strengthen the position of the studios in future union <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89163882\" xlink:type=\"simple\">negotiations</related-article>. In January 1927, Mayer and two colleagues invited more than thirty industry executives to a banquet, where they introduced their proposal for the formation of the International Academy of Motion Picture Arts and Sciences. The word “International” was soon dropped, and the state of <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88112623\" xlink:type=\"simple\">California</related-article> granted a charter giving it nonprofit status as a legal corporation in May 1927, with the distinguished <related-article related-article-type=\"no\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89550122\" xlink:type=\"simple\">actor</related-article>\n   <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88830490\" xlink:type=\"simple\">Douglas Fairbanks</related-article>, Sr., as its first president. Membership in the organization was available for $100, and 231 people accepted an invitation to join. Among other goals, academy founders wanted to establish “awards of merit for distinctive achievement,” and by 1928, a committee had developed a system through which each member would cast a vote for a nomination in his or her discipline. The nominations would be tallied, and a panel of judges with one member from each discipline would choose the winners.</p>",
            "indexed" : "true",
            "sec-type" : "bodytext"
          }, {
            "value" : "<title>Establishing the Categories</title>\n<p>In July, 1928, the awards committee announced there would be prizes in twelve categories: actor, actress, dramatic director, comedy director, <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89250390\" xlink:type=\"simple\">cinematography</related-article>, art direction, <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89250440\" xlink:type=\"simple\">engineering</related-article> effects, outstanding picture, artistic production, original story, adapted story, and title writing. The release of <italic>The Jazz Singer</italic> in 1927, the first “talking picture,” or “talkie,” had such an impact on the industry that it was ruled ineligible for the outstanding picture award because it was considered unfair <related-article related-article-type=\"no\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89551660\" xlink:type=\"simple\">competition</related-article> for films without synchronized sound that contained written inserts (or “titles”). Otherwise, films opening between August 1, 1927, and July 31, 1928, were eligible for consideration.</p>\n<p>Almost one thousand nominations were received, and a list of the ten candidates with the most votes in each category was compiled. Boards of judges representing the five disciplines—acting, writing, directing, producing, and technical production—assembled a short list of three selections in each category. A Central Board of Judges then made the final selections, joined by Mayer. The winners were announced in mid-February 1929, right after the selection meeting, and the awards were presented at the Academy’s second-anniversary dinner in Los Angeles on May 16, 1929. Sculptor George Stanley had been paid $500 to cast a small bronze statuette (eventually called the “Oscar”), finished with 24-karat gold plate, to be given to the twelve winners of the <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"87998190\" xlink:type=\"simple\">Academy Award</related-article> of Merit.</p>",
            "indexed" : "true",
            "sec-type" : "bodytext"
          }, {
            "value" : "<title>The First Academy Awards</title>\n<p>As an acknowledgment of the importance of synchronized sound, the first Academy Awards ceremony began with a demonstration of the newly developed Western Electric projection device for showing talking pictures. The film clip showed Douglas Fairbanks presenting the outstanding picture award for <italic>\n      <related-article related-article-type=\"no\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88833390\" xlink:type=\"simple\">Wings</related-article>\n   </italic> to Paramount Pictures president Adolph Zuckor in Paramount’s <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88112650\" xlink:type=\"simple\">New York</related-article> studio. <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89407313\" xlink:type=\"simple\">F. W. Murnau</related-article>’s <italic>Sunrise: A Song of Two Humans</italic> won the artistic production award over <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88826623\" xlink:type=\"simple\">King Vidor</related-article>’s <italic>The Crowd</italic>, partly due to Mayer’s active support for a film that he felt would lend prestige to the industry. (Mayer’s own studio, MGM, had produced Vidor’s film, which Mayer considered too bleak to represent the industry.) <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88833002\" xlink:type=\"simple\">Janet Gaynor</related-article> (<italic>Seventh Heaven</italic>) and Emil Jannings (<italic>The Last Command</italic> and <italic>The <related-article related-article-type=\"no\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"87575458\" xlink:type=\"simple\">Way of All Flesh</related-article>\n   </italic>) were named best actress and best actor, respectively. <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88828468\" xlink:type=\"simple\">Frank Borzage</related-article> was named best dramatic director for <italic>Seventh Heaven</italic>, Lewis Milestone won best comedy director for <italic>Two Arabian</italic>\n   <italic>Knights,</italic> and Charles Rosher and Karl Struss shared the cinematography award for their innovative work on <italic>Sunrise</italic>. Though ineligible for outstanding picture, <italic>The Jazz Singer</italic> was nominated for engineering effects (won by <italic>Wings</italic>) and for adapted story writing (won by <italic>Seventh Heaven</italic>), and <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88960978\" xlink:type=\"simple\">Warner Bros.</related-article> received a special award for producing <italic>The Jazz Singer.</italic>\n</p>",
            "indexed" : "true",
            "sec-type" : "bodytext"
          }, {
            "value" : "<title>The Evolution of the Awards</title>\n<p>Shifts in the number of nominees in each category and the names of the categories for the second and third Academy Awards ceremonies reflected changes in which elements the academy considered most worthy of recognition. Because of changes in timing and scheduling, both of these ceremonies were held in 1930: The second Academy Awards, honoring films made in 1928 and 1929, took place on April 3, 1930; the third, honoring films from 1929 and 1930, took place on November 5.</p>\n<p>Of the twelve initial award categories, only seven were retained for the second (1928/1929) Academy Awards ceremony. There were five nominees each for outstanding picture, actor, art direction, and cinematography, and six for actress, director, and writing. Among the winners were the 1929 musical <italic>The Broadway Melody </italic>(best picture), Frank Lloyd (best director, <italic>The Divine </italic>Lady), Warner Baxter (best actor, <italic>In Old <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88112621\" xlink:type=\"simple\">Arizona</related-article>\n   </italic>), and <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"88828173\" xlink:type=\"simple\">Mary Pickford</related-article> (best actress, <italic>Coquette</italic>).</p>\n<p>The third (1929/1930) Academy Awards expanded the categories to eight, adding an award for best <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"87320955\" xlink:type=\"simple\">sound recording</related-article>, captured by <italic>The Big House </italic>and its sound director Douglas Shearer. Lewis Milestone was named best director for <italic>All Quiet on the Western Front</italic>, which was also named outstanding (best) picture, beginning the common practice of pairing these two awards with the same film. Also for the first time, nominations were voted on by all of the members of each discipline, and the entire membership of the academy voted on the final ballot.</p>",
            "indexed" : "true",
            "sec-type" : "bodytext"
          }, {
            "value" : "<title>Impact</title>\n<p>A tradition born in the late 1920s with the advent of sound films, the Academy Awards have become one of the most important honors in <related-article related-article-type=\"rs\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"89185627\" xlink:type=\"simple\">popular culture</related-article>, capturing a worldwide audience and considerably enhancing the reputation of its winners.</p>",
            "indexed" : "true",
            "sec-type" : "bodytext"
          }, {
            "value" : "<title>Bibliography</title>\n<ref-list>\n   <ref>\n      <citation citation-type=\"booksimple\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\">Holden, Anthony. <italic>Behind the Oscar: The Secret History of the Academy Awards</italic>. New York: Simon and Schuster, 1993. A witty, incisive, and readable history of more than six decades of Oscars.</citation>\n   </ref>\n   <ref>\n      <citation citation-type=\"booksimple\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\">Levy, Emanuel. <italic>All About Oscar: The History and Politics of the Academy Awards</italic>. New York: Continuum, 2003. A thorough scholarly study, reliable and authoritative.</citation>\n   </ref>\n   <ref>\n      <citation citation-type=\"booksimple\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\">Osborne, Robert. <italic>80 Years of the Oscar: The Official History of the Academy Awards</italic>. New York: Abbeyville, 2008. A comprehensive and complete history by an expert.</citation>\n   </ref>\n   <ref>\n      <citation citation-type=\"booksimple\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\">Wiley, Mason, Damien Bona, and Gail MacColl. <italic>Inside Oscar: The Unofficial History of the Academy Awards</italic>. 5th ed. New York: Ballantine, 1996. An entertaining, informative, and irreverent account.</citation>\n   </ref>\n</ref-list>",
            "indexed" : "true",
            "sec-type" : "hangindent"
          }, {
            "value" : "<title/>\n<ref-list>\n   <ref>\n      <citation citation-type=\"original\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\">\n         <article-title>Academy Awards</article-title>\n         <italic>The Twenties in America</italic>\n         <person-group person-group-type=\"editor\">\n            <name name-style=\"western\">\n               <given-names>Carl</given-names>\n               <x xml:space=\"preserve\"> </x>\n               <surname>Rollyson</surname>\n            </name>\n         </person-group>\n         <year>2012</year>\n         <publisher-name>Salem Press</publisher-name>\n      </citation>\n   </ref>\n</ref-list>",
            "indexed" : "true",
            "sec-type" : "rs_citation"
          } ]
        } ],
        "book-part-type" : "chapter",
        "id" : "Twenties_RS.10.3331.54171",
        "indexed" : "true",
        "type" : "simple",
        "xml:lang" : "EN"
      } ]
    } ],
    "dtd-version" : "2.3",
    "xml:lang" : "EN",
    "xmlns:mml" : "http://www.w3.org/1998/Math/MathML",
    "xmlns:xlink" : "http://www.w3.org/1999/xlink",
    "xmlns:xsi" : "http://www.w3.org/2001/XMLSchema-instance"
  }
}
```

## Future Work:

*TBD:*

* configuration for paths that should be treated as single value, not arrays
* configuration for paths that should be collapsed into their parent (i.e. text value)
* check ordering of attributes that they appear before other values in JSON maps (they are going at end, but should be first if possible)
* reduce multiple spaces in embedded XML to one