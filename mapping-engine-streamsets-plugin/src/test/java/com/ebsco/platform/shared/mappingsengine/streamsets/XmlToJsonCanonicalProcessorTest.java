package com.ebsco.platform.shared.mappingsengine.streamsets;

import com.ebsco.entarch.mappings.streamsets.InputTypes;
import com.ebsco.entarch.mappings.streamsets.InstructionsSources;
import com.ebsco.entarch.mappings.streamsets.XmlToJsonCanonicalProcessor;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.FileRef;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.sdk.ProcessorRunner;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.sdk.StageRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.ebsco.entarch.mappings.streamsets.RecordUtils.write;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class XmlToJsonCanonicalProcessorTest {

    private static final String XML_INPUT_FIELD_NAME = "/rawXML";
    private static final String JSON_INPUT_FIELD_NAME = "/rawJSON";
    private static final String FILE_REF_FIELD_NAME = "/fileRef";
    private static final String OUT_JSON_FIELD_NAME = "/json";

    @Test
    @Ignore
    public void processorCanRunFromString() throws Exception {
        ProcessorRunner runner = new ProcessorRunner.Builder(XmlToJsonCanonicalProcessor.class)
                .addConfiguration("mappingInstructionsSource", InstructionsSources.Inline)
                .addConfiguration("mappingInstructions", getText())
                .addConfiguration("rawXmlField", XML_INPUT_FIELD_NAME)
                .addConfiguration("rawInputType", InputTypes.XML)
                .addConfiguration("outJsonField", OUT_JSON_FIELD_NAME)
                .addOutputLane("output")
                .build();
        runner.runInit();
        try {
            Record record = RecordCreator.create();
            record.set(Field.create(new HashMap<>()));

            write(record, XML_INPUT_FIELD_NAME, REALISTIC_XML);

            StageRunner.Output output = runner.runProcess(singletonList(record));

            assertEquals(1, output.getRecords().get("output").size());

            Record result = output.getRecords().get("output").get(0);
            result.getEscapedFieldPaths().forEach(it -> System.out.println("FILED: " + it));
            String resultJson = result.get(OUT_JSON_FIELD_NAME).getValueAsString();

            System.out.println("OUTPUT FINAL RECORD:\n\n" + resultJson);
        } finally {
            runner.runDestroy();
        }
    }

    @NotNull
    private String getText() throws IOException {
        return new String(Files.readAllBytes(Paths.get("../sample/mappings-example.json")));
    }

    @Test
    @Ignore
    public void processorCanRunFromFileRef() throws Exception {
        ProcessorRunner runner = new ProcessorRunner.Builder(XmlToJsonCanonicalProcessor.class)
                .addConfiguration("mappingInstructionsSource", InstructionsSources.Inline)
                .addConfiguration("mappingInstructions", getText())
                .addConfiguration("rawXmlField", FILE_REF_FIELD_NAME)
                .addConfiguration("rawInputType", InputTypes.XML)
                .addConfiguration("outJsonField", OUT_JSON_FIELD_NAME)
                .addOutputLane("output")
                .build();
        runner.runInit();
        try {
            Record record = RecordCreator.create();
            record.set(Field.create(new HashMap<>()));

            FileRef fakeFileRef = new FileRef(8192) {
                @Override
                public <T extends AutoCloseable> Set<Class<T>> getSupportedStreamClasses() {
                    Set<Class<T>> result = new HashSet<>();
                    result.add((Class<T>) InputStream.class);
                    return result;
                }

                @Override
                public <T extends AutoCloseable> T createInputStream(Stage.Context context, Class<T> streamClassType) throws IOException {
                    InputStream is = new ByteArrayInputStream(REALISTIC_XML.getBytes());
                    return (T) is;
                }
            };

            record.set(FILE_REF_FIELD_NAME, Field.create(Field.Type.FILE_REF, fakeFileRef));
            write(record, FILE_REF_FIELD_NAME, REALISTIC_XML.trim());

            StageRunner.Output output = runner.runProcess(singletonList(record));

            assertEquals(1, output.getRecords().get("output").size());

            Record result = output.getRecords().get("output").get(0);
            result.getEscapedFieldPaths().forEach(it -> System.out.println("FILED: " + it));
            String resultJson = result.get(OUT_JSON_FIELD_NAME).getValueAsString();

            System.out.println("OUTPUT FINAL RECORD:\n\n" + resultJson);
        } finally {
            runner.runDestroy();
        }
    }

    @Test
    @Ignore
    public void processJsonInputType() throws Exception {
        ProcessorRunner runner = new ProcessorRunner.Builder(XmlToJsonCanonicalProcessor.class)
                .addConfiguration("mappingInstructionsSource", InstructionsSources.Inline)
                .addConfiguration("mappingInstructions", getText())
                .addConfiguration("rawXmlField", JSON_INPUT_FIELD_NAME)
                .addConfiguration("rawInputType", InputTypes.JSON)
                .addConfiguration("outJsonField", OUT_JSON_FIELD_NAME)
                .addOutputLane("output")
                .build();
        runner.runInit();
        try {
            Record record = RecordCreator.create();
            record.set(Field.create(new HashMap<>()));

            write(record, JSON_INPUT_FIELD_NAME, BASIC_JSON_BOOK);

            StageRunner.Output output = runner.runProcess(singletonList(record));

            assertEquals(1, output.getRecords().get("output").size());

            Record result = output.getRecords().get("output").get(0);
            result.getEscapedFieldPaths().forEach(it -> System.out.println("FILED: " + it));
            String resultJson = result.get(OUT_JSON_FIELD_NAME).getValueAsString();

            System.out.println("OUTPUT FINAL RECORD:\n\n" + resultJson);
        } finally {
            runner.runDestroy();
        }
    }

    @Test
    @Ignore
    public void canLoadMappingsFromFile() throws Exception {
        ProcessorRunner runner = new ProcessorRunner.Builder(XmlToJsonCanonicalProcessor.class)
                .addConfiguration("mappingInstructionsSource", InstructionsSources.File)
                .addConfiguration("mappingInstructionsFilename", "./src/test/resources/mappings-example.json")
                .addConfiguration("rawXmlField", XML_INPUT_FIELD_NAME)
                .addConfiguration("rawInputType", InputTypes.XML)
                .addConfiguration("outJsonField", OUT_JSON_FIELD_NAME)
                .addOutputLane("output")
                .build();
        runner.runInit();
        runner.runDestroy();
    }

    @Test
    @Ignore
    public void canLoadMappingsFromClasspath() throws Exception {
        ProcessorRunner runner = new ProcessorRunner.Builder(XmlToJsonCanonicalProcessor.class)
                .addConfiguration("mappingInstructionsSource", InstructionsSources.Classpath)
                .addConfiguration("mappingInstructionsFilename", "/mappings-example.json")
                .addConfiguration("rawXmlField", XML_INPUT_FIELD_NAME)
                .addConfiguration("rawInputType", InputTypes.XML)
                .addConfiguration("outJsonField", OUT_JSON_FIELD_NAME)
                .addOutputLane("output")
                .build();
        runner.runInit();
        runner.runDestroy();
    }

    private static final String BASIC_JSON_BOOK = "{\"book\": { \"author\": \"fred\" } }";

    private static final String REALISTIC_XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?><?xml-stylesheet href=\"file:////edc-filer1/busdev/PropPubProjects/Research Starters/Common Documents/CSS/CSS_Salem.css\"?>\n" +
            "<!DOCTYPE book>\n" +
            "<book xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" dtd-version=\"2.3\" xml:lang=\"EN\">\n" +
            "   <book-series-meta>\n" +
            "      <book-id pub-id-type=\"doi\">10.3331/sp_ency</book-id>\n" +
            "   </book-series-meta>\n" +
            "   <book-meta>\n" +
            "      <book-id pub-id-type=\"doi\">10.3331/1920_rs</book-id>\n" +
            "      <book-title-group>\n" +
            "         <book-title>Salem Press Encyclopedia</book-title>\n" +
            "         <alt-title alt-title-type=\"mid\">G97S</alt-title>\n" +
            "         <alt-title alt-title-type=\"derived_from_id\">10.3331/1920</alt-title>\n" +
            "         <alt-title alt-title-type=\"original\">The Twenties in America</alt-title>\n" +
            "      </book-title-group>\n" +
            "      <publisher>\n" +
            "         <publisher-name>Salem Press</publisher-name>\n" +
            "      </publisher>\n" +
            "      <pub-date pub-type=\"original\">\n" +
            "         <year>2012</year>\n" +
            "      </pub-date>\n" +
            "      <pub-date pub-type=\"update\">\n" +
            "         <year>2016</year>\n" +
            "      </pub-date>\n" +
            "   </book-meta>\n" +
            "   <body>\n" +
            "      <book-part id=\"Twenties_RS.10.3331.54171\" indexed=\"true\" book-part-type=\"chapter\" xlink:type=\"simple\" xml:lang=\"EN\">\n" +
            "         <book-part-meta>\n" +
            "            <book-part-id pub-id-type=\"doi\">10.3331/1920_rs_54171</book-part-id>\n" +
            "            <title-group>\n" +
            "               <title>Academy Awards begin</title>\n" +
            "            </title-group>\n" +
            "            <contrib-group>\n" +
            "               <contrib contrib-type=\"author\" xlink:type=\"simple\">\n" +
            "                  <name name-style=\"western\">\n" +
            "                     <given-names>Leon</given-names>\n" +
            "                     <x xml:space=\"preserve\"> </x>\n" +
            "                     <surname>Lewis</surname>\n" +
            "                  </name>\n" +
            "               </contrib>\n" +
            "            </contrib-group>\n" +
            "            <abstract abstract-type=\"short\">\n" +
            "               <p>The first awards ceremony held by the Motion Picture Academy of Arts and Sciences took place in 1929, a mere two years after the first “talking” pictures were released, providing both prestige and publicity for the already rapidly expanding filmmaking industry.</p>\n" +
            "            </abstract>\n" +
            "            <custom-meta-wrap>\n" +
            "               <custom-meta xlink:type=\"simple\">\n" +
            "                  <meta-name>Derived from</meta-name>\n" +
            "                  <meta-value>10.3331/1920_0001</meta-value>\n" +
            "               </custom-meta>\n" +
            "            </custom-meta-wrap>\n" +
            "         </book-part-meta>\n" +
            "         <book-front>\n" +
            "            <sec sec-type=\"reviewed\">\n" +
            "               <title/>\n" +
            "               <p>\n" +
            "                  <bold>Last reviewed:</bold> February 2016</p>\n" +
            "            </sec>\n" +
            "\n" +
            "         </book-front>\n" +
            "         <body>\n" +
            "            <sec sec-type=\"bodytext\" indexed=\"true\">\n" +
            "               <title/>\n" +
            "               <p>\n" +
            "                  <italic>The first awards ceremony held by the Motion Picture Academy of Arts and Sciences took place in 1929, a mere two years after the first “talking” pictures were released, providing both prestige and publicity for the already rapidly expanding filmmaking industry.</italic>\n" +
            "               </p>\n" +
            "               <p>In the wake of a 1926 union agreement between film studios and technicians, <related-article related-article-type=\"rs\" xlink:href=\"88801928\" xlink:type=\"simple\">Louis B. Mayer</related-article>, the production chief of <related-article related-article-type=\"rs\" xlink:href=\"88960862\" xlink:type=\"simple\">Metro-Goldwyn-Mayer (MGM)</related-article> and one of the most powerful people in Hollywood, sought to strengthen the position of the studios in future union <related-article related-article-type=\"rs\" xlink:href=\"89163882\" xlink:type=\"simple\">negotiations</related-article>. In January 1927, Mayer and two colleagues invited more than thirty industry executives to a banquet, where they introduced their proposal for the formation of the International Academy of Motion Picture Arts and Sciences. The word “International” was soon dropped, and the state of <related-article related-article-type=\"rs\" xlink:href=\"88112623\" xlink:type=\"simple\">California</related-article> granted a charter giving it nonprofit status as a legal corporation in May 1927, with the distinguished <related-article related-article-type=\"no\" xlink:href=\"89550122\" xlink:type=\"simple\">actor</related-article>\n" +
            "                  <related-article related-article-type=\"rs\" xlink:href=\"88830490\" xlink:type=\"simple\">Douglas Fairbanks</related-article>, Sr., as its first president. Membership in the organization was available for ${'$'}100, and 231 people accepted an invitation to join. Among other goals, academy founders wanted to establish “awards of merit for distinctive achievement,” and by 1928, a committee had developed a system through which each member would cast a vote for a nomination in his or her discipline. The nominations would be tallied, and a panel of judges with one member from each discipline would choose the winners.</p>\n" +
            "            </sec>\n" +
            "            <sec sec-type=\"bodytext\" indexed=\"true\">\n" +
            "               <title>Establishing the Categories</title>\n" +
            "               <p>In July, 1928, the awards committee announced there would be prizes in twelve categories: actor, actress, dramatic director, comedy director, <related-article related-article-type=\"rs\" xlink:href=\"89250390\" xlink:type=\"simple\">cinematography</related-article>, art direction, <related-article related-article-type=\"rs\" xlink:href=\"89250440\" xlink:type=\"simple\">engineering</related-article> effects, outstanding picture, artistic production, original story, adapted story, and title writing. The release of <italic>The Jazz Singer</italic> in 1927, the first “talking picture,” or “talkie,” had such an impact on the industry that it was ruled ineligible for the outstanding picture award because it was considered unfair <related-article related-article-type=\"no\" xlink:href=\"89551660\" xlink:type=\"simple\">competition</related-article> for films without synchronized sound that contained written inserts (or “titles”). Otherwise, films opening between August 1, 1927, and July 31, 1928, were eligible for consideration.</p>\n" +
            "               <p>Almost one thousand nominations were received, and a list of the ten candidates with the most votes in each category was compiled. Boards of judges representing the five disciplines—acting, writing, directing, producing, and technical production—assembled a short list of three selections in each category. A Central Board of Judges then made the final selections, joined by Mayer. The winners were announced in mid-February 1929, right after the selection meeting, and the awards were presented at the Academy’s second-anniversary dinner in Los Angeles on May 16, 1929. Sculptor George Stanley had been paid ${'$'}500 to cast a small bronze statuette (eventually called the “Oscar”), finished with 24-karat gold plate, to be given to the twelve winners of the <related-article related-article-type=\"rs\" xlink:href=\"87998190\" xlink:type=\"simple\">Academy Award</related-article> of Merit.</p>\n" +
            "            </sec>\n" +
            "            <sec sec-type=\"bodytext\" indexed=\"true\">\n" +
            "               <title>The First Academy Awards</title>\n" +
            "               <p>As an acknowledgment of the importance of synchronized sound, the first Academy Awards ceremony began with a demonstration of the newly developed Western Electric projection device for showing talking pictures. The film clip showed Douglas Fairbanks presenting the outstanding picture award for <italic>\n" +
            "                     <related-article related-article-type=\"no\" xlink:href=\"88833390\" xlink:type=\"simple\">Wings</related-article>\n" +
            "                  </italic> to Paramount Pictures president Adolph Zuckor in Paramount’s <related-article related-article-type=\"rs\" xlink:href=\"88112650\" xlink:type=\"simple\">New York</related-article> studio. <related-article related-article-type=\"rs\" xlink:href=\"89407313\" xlink:type=\"simple\">F. W. Murnau</related-article>’s <italic>Sunrise: A Song of Two Humans</italic> won the artistic production award over <related-article related-article-type=\"rs\" xlink:href=\"88826623\" xlink:type=\"simple\">King Vidor</related-article>’s <italic>The Crowd</italic>, partly due to Mayer’s active support for a film that he felt would lend prestige to the industry. (Mayer’s own studio, MGM, had produced Vidor’s film, which Mayer considered too bleak to represent the industry.) <related-article related-article-type=\"rs\" xlink:href=\"88833002\" xlink:type=\"simple\">Janet Gaynor</related-article> (<italic>Seventh Heaven</italic>) and Emil Jannings (<italic>The Last Command</italic> and <italic>The <related-article related-article-type=\"no\" xlink:href=\"87575458\" xlink:type=\"simple\">Way of All Flesh</related-article>\n" +
            "                  </italic>) were named best actress and best actor, respectively. <related-article related-article-type=\"rs\" xlink:href=\"88828468\" xlink:type=\"simple\">Frank Borzage</related-article> was named best dramatic director for <italic>Seventh Heaven</italic>, Lewis Milestone won best comedy director for <italic>Two Arabian</italic>\n" +
            "                  <italic>Knights,</italic> and Charles Rosher and Karl Struss shared the cinematography award for their innovative work on <italic>Sunrise</italic>. Though ineligible for outstanding picture, <italic>The Jazz Singer</italic> was nominated for engineering effects (won by <italic>Wings</italic>) and for adapted story writing (won by <italic>Seventh Heaven</italic>), and <related-article related-article-type=\"rs\" xlink:href=\"88960978\" xlink:type=\"simple\">Warner Bros.</related-article> received a special award for producing <italic>The Jazz Singer.</italic>\n" +
            "               </p>\n" +
            "            </sec>\n" +
            "            <sec sec-type=\"bodytext\" indexed=\"true\">\n" +
            "               <title>The Evolution of the Awards</title>\n" +
            "               <p>Shifts in the number of nominees in each category and the names of the categories for the second and third Academy Awards ceremonies reflected changes in which elements the academy considered most worthy of recognition. Because of changes in timing and scheduling, both of these ceremonies were held in 1930: The second Academy Awards, honoring films made in 1928 and 1929, took place on April 3, 1930; the third, honoring films from 1929 and 1930, took place on November 5.</p>\n" +
            "               <p>Of the twelve initial award categories, only seven were retained for the second (1928/1929) Academy Awards ceremony. There were five nominees each for outstanding picture, actor, art direction, and cinematography, and six for actress, director, and writing. Among the winners were the 1929 musical <italic>The Broadway Melody </italic>(best picture), Frank Lloyd (best director, <italic>The Divine </italic>Lady), Warner Baxter (best actor, <italic>In Old <related-article related-article-type=\"rs\" xlink:href=\"88112621\" xlink:type=\"simple\">Arizona</related-article>\n" +
            "                  </italic>), and <related-article related-article-type=\"rs\" xlink:href=\"88828173\" xlink:type=\"simple\">Mary Pickford</related-article> (best actress, <italic>Coquette</italic>).</p>\n" +
            "               <p>The third (1929/1930) Academy Awards expanded the categories to eight, adding an award for best <related-article related-article-type=\"rs\" xlink:href=\"87320955\" xlink:type=\"simple\">sound recording</related-article>, captured by <italic>The Big House </italic>and its sound director Douglas Shearer. Lewis Milestone was named best director for <italic>All Quiet on the Western Front</italic>, which was also named outstanding (best) picture, beginning the common practice of pairing these two awards with the same film. Also for the first time, nominations were voted on by all of the members of each discipline, and the entire membership of the academy voted on the final ballot.</p>\n" +
            "            </sec>\n" +
            "            <sec sec-type=\"bodytext\" indexed=\"true\">\n" +
            "               <title>Impact</title>\n" +
            "               <p>A tradition born in the late 1920s with the advent of sound films, the Academy Awards have become one of the most important honors in <related-article related-article-type=\"rs\" xlink:href=\"89185627\" xlink:type=\"simple\">popular culture</related-article>, capturing a worldwide audience and considerably enhancing the reputation of its winners.</p>\n" +
            "            </sec>\n" +
            "            <sec sec-type=\"hangindent\" indexed=\"true\">\n" +
            "               <title>Bibliography</title>\n" +
            "               <ref-list>\n" +
            "                  <ref>\n" +
            "                     <citation citation-type=\"booksimple\" xlink:type=\"simple\">Holden, Anthony. <italic>Behind the Oscar: The Secret History of the Academy Awards</italic>. New York: Simon and Schuster, 1993. A witty, incisive, and readable history of more than six decades of Oscars.</citation>\n" +
            "                  </ref>\n" +
            "                  <ref>\n" +
            "                     <citation citation-type=\"booksimple\" xlink:type=\"simple\">Levy, Emanuel. <italic>All About Oscar: The History and Politics of the Academy Awards</italic>. New York: Continuum, 2003. A thorough scholarly study, reliable and authoritative.</citation>\n" +
            "                  </ref>\n" +
            "                  <ref>\n" +
            "                     <citation citation-type=\"booksimple\" xlink:type=\"simple\">Osborne, Robert. <italic>80 Years of the Oscar: The Official History of the Academy Awards</italic>. New York: Abbeyville, 2008. A comprehensive and complete history by an expert.</citation>\n" +
            "                  </ref>\n" +
            "                  <ref>\n" +
            "                     <citation citation-type=\"booksimple\" xlink:type=\"simple\">Wiley, Mason, Damien Bona, and Gail MacColl. <italic>Inside Oscar: The Unofficial History of the Academy Awards</italic>. 5th ed. New York: Ballantine, 1996. An entertaining, informative, and irreverent account.</citation>\n" +
            "                  </ref>\n" +
            "               </ref-list>\n" +
            "            </sec>\n" +
            "            <sec sec-type=\"rs_citation\" indexed=\"true\">\n" +
            "               <title/>\n" +
            "               <ref-list>\n" +
            "                  <ref>\n" +
            "                     <citation citation-type=\"original\" xlink:type=\"simple\">\n" +
            "                        <article-title>Academy Awards</article-title>\n" +
            "                        <italic>The Twenties in America</italic>\n" +
            "                        <person-group person-group-type=\"editor\">\n" +
            "                           <name name-style=\"western\">\n" +
            "                              <given-names>Carl</given-names>\n" +
            "                              <x xml:space=\"preserve\"> </x>\n" +
            "                              <surname>Rollyson</surname>\n" +
            "                           </name>\n" +
            "                        </person-group>\n" +
            "                        <year>2012</year>\n" +
            "                        <publisher-name>Salem Press</publisher-name>\n" +
            "                     </citation>\n" +
            "                  </ref>\n" +
            "               </ref-list>\n" +
            "            </sec>\n" +
            "         </body>\n" +
            "      </book-part>\n" +
            "   </body>\n" +
            "</book>";
}
