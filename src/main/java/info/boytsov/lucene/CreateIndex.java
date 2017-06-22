/**
 * This file is based on IndexDump, see Daniel Lemire's repo:
 * 
 * https://github.com/lemire/IndexWikipedia
 * 
 * This code is released under the
 * Apache License Version 2.0 http://www.apache.org/licenses/.
 * 
 */
package info.boytsov.lucene;

import java.io.File;
import java.util.Properties;

import me.flaviomartins.lucene.analysis.EnglishWikipediaAnalyzer;
import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import info.boytsov.lucene.parsers.ClueWeb09ContentSource;
import info.boytsov.lucene.parsers.TrecContentSource;
import info.boytsov.lucene.parsers.EnwikiContentSource;


/**
 * A simple utility to index files in different formats using Lucene.
 * Supported formats:
 *    
 *    1) Gov2
 *    2) ClueWeb09
 *    3) Wikipedia
 * 
 * @author Leonid Boytsov
 * @author Daniel Lemire
 * 
 */
public class CreateIndex {

  public static void main(String[] args) throws Exception {
    if (args.length != 3 && args.length != 4) {
      printUsage();
      System.exit(1);
    }
    String indexType = args[0];
    String indexSource = args[1];
    int commitInterval = 1000000;
    
    if (args.length >= 4) {
      commitInterval = Integer.parseInt(args[3]);
    }
    
    System.out.println("Commiting after indexing " + commitInterval + " docs");

    File outputDir = new File(args[2]);
    if (!outputDir.exists()) {
      if (!outputDir.mkdirs()) {
        System.out.println("couldn't create " + outputDir.getAbsolutePath());
        return;
      }
    }
    if (!outputDir.isDirectory()) {
      System.out.println(outputDir.getAbsolutePath() + " is not a directory!");
      return;
    }
    if (!outputDir.canWrite()) {
      System.out.println("Can't write to " + outputDir.getAbsolutePath());
      return;
    }

    FSDirectory dir = FSDirectory.open(outputDir);

    EnglishWikipediaAnalyzer analyzer = new EnglishWikipediaAnalyzer(
            Version.LUCENE_46);// default
                               // stop
                               // words
    IndexWriterConfig config = new IndexWriterConfig(
            Version.LUCENE_46, analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);// overwrites
                                                          // if
                                                          // needed
    IndexWriter indexWriter = new IndexWriter(dir, config);

    DocMaker docMaker = new DocMaker();
    Properties properties = new Properties();
    properties.setProperty("content.source.forever", "false"); // will
                                                               // parse
                                                               // each
                                                               // document
                                                               // only
                                                               // once
    properties.setProperty("doc.index.props", "true");
    // We want to store small-size fields like URL or even title  ...
    properties.setProperty("doc.stored", "true");
    // but not the large one (great savings, 3x reduction in space)!
    properties.setProperty("doc.body.stored", "false");
   
    ContentSource source = CreateSource(indexType, indexSource, properties);
    
    if (source == null) {
      System.err.println("Failed to create a source: " + 
                         indexType + "(" + indexSource + ")");
      printUsage();
      System.exit(1);
    }
    
    Config c = new Config(properties);    
    source.setConfig(c);
    source.resetInputs();// though this does not seem needed, it is
                         // (gets the file opened?)
    docMaker.setConfig(c, source);
    int count = 0;
    System.out.println("Starting Indexing of " + indexType + " source " + indexSource);
    
    long start = System.currentTimeMillis();
    Document doc;
    try {
      while ((doc = docMaker.makeDocument()) != null) {
        indexWriter.addDocument(doc);
        ++count;
        if (count % 5000 == 0) {
          System.out.println("Indexed " + count + " documents in "
                              + (System.currentTimeMillis() - start) + " ms");
        } 
        if (count % commitInterval == 0) {
          indexWriter.commit();
          System.out.println("Committed");
        }
      }
    } catch (org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException nmd) {
       System.out.println("Caught NoMoreDataException! -- Finishing"); // All done
    }
    long finish = System.currentTimeMillis();
    System.out.println("Indexing " + count + " documents took "
            + (finish - start) + " ms");
    System.out.println("Total data processed: "
            + source.getTotalBytesCount() + " bytes");
    System.out.println("Index should be located at "
            + dir.getDirectory().getAbsolutePath());
    docMaker.close();
    indexWriter.commit();
    indexWriter.close();

  }

  private static ContentSource CreateSource(String indexType,
      String indexSource, Properties properties) {
    String typeLC = indexType.toUpperCase();
    
    if (typeLC.equals("WIKIPEDIA")) {  
      File wikipediafile = new File(indexSource);
      if (!wikipediafile.exists()) {
        System.out.println("Can't find " + wikipediafile.getAbsolutePath());
        return null;
      }
      if (!wikipediafile.canRead()) {
        System.out.println("Can't read " + wikipediafile.getAbsolutePath());
        return null;
      }
     
      properties.setProperty("docs.file", wikipediafile.getAbsolutePath());
      properties.setProperty("keep.image.only.docs", "false");

      return new EnwikiContentSource();
    } else if (typeLC.startsWith("TREC:")){
      typeLC = typeLC.substring("TREC:".length());

      if (typeLC.equals("GOV2")) {        
        String parserTREC = "info.boytsov.lucene.parsers.TrecGov2Parser";
        
        
        properties.setProperty("html.parser", 
                               "info.boytsov.lucene.parsers.DemoHTMLParser");
        properties.setProperty("trec.doc.parser", parserTREC);
        properties.setProperty("docs.dir", indexSource);
        properties.setProperty("work.dir", "/tmp");
        
        return new TrecContentSource();        
      }
      if (typeLC.equals("CLUEWEB09")) {
      // Demo HTML parser fails on this collection
        properties.setProperty("html.parser", 
                               "info.boytsov.lucene.parsers.LeoHTMLParser");
        properties.setProperty("docs.dir", indexSource);
        properties.setProperty("work.dir", "/tmp");

        return new ClueWeb09ContentSource();
      }
      
      System.err.println("Unsupported TREC collection: " + typeLC);  
      
    } else {
      System.err.println("Unsupported index type: " + indexType);
    }
    
    return null;
  }

  private static void printUsage() {
    System.out.println("mvn exec:java " + 
                       " -Dexec.mainClass=info.boytsov.lucene.CreateIndex " +
                       " -Dexec.args=\"" + 
                       " <index type> <input dir/file> <output dir>" + 
                       " <# of docs before commit> \"");
  }
}
