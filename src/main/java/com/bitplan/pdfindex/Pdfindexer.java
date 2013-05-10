/**
 * Copyright (C) 2012 BITPlan GmbH
 *
 * Pater-Delp-Str. 1
 * D-47877 Willich-Schiefbahn
 *
 * http://www.bitplan.com
 *
 */
package com.bitplan.pdfindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * indexer for PDF files
 * http://kalanir.blogspot.de/2008/08/indexing-pdf-documents-with-lucene.html
 * 
 * @author wf
 * 
 */
public class Pdfindexer {
	/**
	 * current Version of the Pdfindexer tool
	 */
	public static final String VERSION = "0.0.1";

	@Option(name = "-f", aliases = { "--src" }, usage = "source directory/or file")
	private String source;

	@Option(name = "-l", aliases = { "--sourceFileList" }, usage = "file with source file names")
	private String sourceFileList;

	@Option(name = "-w", aliases = { "--searchWordList" }, usage = "file with search words")
	private String searchWordList;

	@Option(name = "-i", aliases = { "--idxfile" }, usage = "index file", required = true)
	private String indexFile;

	@Option(name = "-o", aliases = { "--outputfile" }, usage = "output file")
	private String outputFile;

	@Option(name = "-s", aliases = { "--search" }, usage = "search")
	private String search;

	private static Pdfindexer indexer;
	private CmdLineParser parser;
	static int exitCode;
	public static boolean testMode = false;
	public List<Document> documents = new ArrayList<Document>();
	protected IndexWriter writer;
	protected IndexSearcher searcher = null;

	/**
	 * @return the search
	 */
	public String getSearch() {
		return search;
	}

	/**
	 * @param search
	 *          the search to set
	 */
	public void setSearch(String search) {
		this.search = search;
	}

	/**
	 * @return the outputFile
	 */
	public String getOutputFile() {
		return outputFile;
	}

	/**
	 * @param outputFile
	 *          the outputFile to set
	 */
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @return the indexFile
	 */
	public String getIndexFile() {
		return indexFile;
	}

	/**
	 * @param indexFile
	 *          the indexFile to set
	 */
	public void setIndexFile(String indexFile) {
		this.indexFile = indexFile;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param sourceDirectory
	 *          the sourceDirectory to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the sourceFileList
	 */
	public String getSourceFileList() {
		return sourceFileList;
	}

	/**
	 * @param sourceFileList
	 *          the sourceFileList to set
	 */
	public void setSourceFileList(String sourceFileList) {
		this.sourceFileList = sourceFileList;
	}

	/**
	 * @return the searchWordList
	 */
	public String getSearchWordList() {
		return searchWordList;
	}

	/**
	 * @param searchWordList
	 *          the searchWordList to set
	 */
	public void setSearchWordList(String searchWordList) {
		this.searchWordList = searchWordList;
	}

	/**
	 * get an Index Writer
	 * 
	 * @return
	 * @throws Exception
	 */
	public IndexWriter getIndexWriter() throws Exception {
		if (writer == null) {

			StandardAnalyzer analyzer = new StandardAnalyzer();
			System.out.println("indexing with Lucene Version ?");
			writer = new IndexWriter(this.indexFile, analyzer, true,
					MaxFieldLength.UNLIMITED);
		}
		return writer;
	}

	/**
	 * add the given file to the index
	 * 
	 * @param file
	 * @throws Exception
	 */
	private void addToIndex(File file) throws Exception {
		PDDocument pddDocument = PDDocument.load(file);
		PDFTextStripper textStripper = new PDFTextStripper();
		for (int pageNo = 1; pageNo <= pddDocument.getNumberOfPages(); pageNo++) {
			textStripper.setStartPage(pageNo);
			textStripper.setEndPage(pageNo);
			String pageContent = textStripper.getText(pddDocument);
			// System.out.println(pageContent);
			Document doc = new Document();

			// Add the pagenumber
			doc.add(new Field("pagenumber", Integer.toString(pageNo),
					Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("content", pageContent, Field.Store.NO,
					Field.Index.ANALYZED));
			doc.add(new Field("FILE", file.getAbsolutePath(), Field.Store.YES,
					Field.Index.ANALYZED));
			documents.add(doc);
			getIndexWriter().addDocument(doc);
		}
		;
	}

	private void close() throws Exception {
		getIndexWriter().optimize();
		getIndexWriter().close();
	}

	/**
	 * get the files to index
	 * 
	 * @return
	 */
	public List<File> getFilesToIndex(String pSource) {
		List<File> result = new ArrayList<File>();
		if (pSource != null) {
			File sourceFile = new File(pSource);
			if (sourceFile.isFile()) {
				result.add(sourceFile);
			} else if (sourceFile.isDirectory()) {
				for (final File file : sourceFile.listFiles()) {
					if (file.isDirectory()) {
						result.addAll(getFilesToIndex(file.getAbsolutePath()));
					}
					if (file.isFile()) {
						if (file.getAbsolutePath().toLowerCase().endsWith(".pdf")) {
							result.add(file);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * get the files to Index
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<File> getFilesToIndex() throws IOException {
		List<File> result = new ArrayList<File>();
		result.addAll(getFilesToIndex(source));
		if (sourceFileList != null) {
			List<String> lines = FileUtils.readLines(new File(sourceFileList));
			for (String aFilepath : lines) {
				result.addAll(getFilesToIndex(aFilepath));
			}
		}
		return result;
	}

	/**
	 * create an index
	 * 
	 * @throws Exception
	 */
	private void createIndex() throws Exception {
		List<File> files = getFilesToIndex();
		for (File file : files)
			addToIndex(file);
		close();
	}

	/**
	 * http://lucene.472066.n3.nabble.com/search-trough-single-pdf-document-return
	 * -page-number-td598698.html
	 * http://tarikguelzim.wordpress.com/2008/09/13/creating
	 * -a-search-engine-to-parse-index-and-search-pdf-file-content/ search the
	 * 
	 * @return hits for the query
	 * @param qString
	 * @param idxField
	 * @param limit
	 * @return
	 */
	protected TopDocs searchIndex(String qString, String idxField, int limit) {
		TopDocs topDocs = null;
		Directory fsDir = null;
		Query query = null;
		QueryParser qParser = null;

		try {
			// query index
			System.out.println("Searching for " + qString + " in " + indexFile
					+ " — field: " + idxField);

			// create index searcher
			fsDir = FSDirectory.getDirectory(indexFile);
			searcher = new IndexSearcher(fsDir);
			// parse query
			qParser = new QueryParser(idxField, new StandardAnalyzer());
			query = qParser.parse(qString);
			// query = QueryParser.parse(qString, idxField, new StandardAnalyzer());
			System.out.println(query.toString());
			// search
			topDocs = searcher.search(query, limit);

		} catch (IOException ex) {
			System.err.println("Index file" + indexFile + " doesn’t exist");
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
		} catch (ParseException ex) {
			System.err.println("Error while parsing the index.");
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
		return topDocs;
	}

	/**
	 * display the index
	 * 
	 * @param topDocs
	 * @param output
	 */
	private void displayIndex(TopDocs topDocs, PrintWriter output, String word) {
		try {
			output.println("<li>" + word + ":" + topDocs.totalHits);
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				String page = doc.get("pagenumber");
				output.println("<a href='" + doc.get("FILE") + "#page=" + page + "'>"
						+ page + "</a>");
			}
			output.println("</li>");
		} catch (IOException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * get Output
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	public PrintWriter getOutput() throws FileNotFoundException {
		PrintWriter output = new PrintWriter(System.out);
		if (this.outputFile != null) {
			output = new PrintWriter(new File(outputFile));
		}
		return output;
	}

	/**
	 * get the search words
	 * @return
	 * @throws Exception
	 */
	public List<String> getSearchWords() throws Exception {
		List<String> result = new ArrayList<String>();
		if (this.search != null) {
			for (String word : search.split(" ")) {
				result.add(word);
			}
		}
		if (this.searchWordList != null) {
			result.addAll(FileUtils.readLines(new File(this.searchWordList)));
		}
		return result;
	}

	/**
	 * create and search the given Index
	 * 
	 * @throws Exception
	 */
	protected void work() throws Exception {
		if ((this.getSource() != null) || (this.getSourceFileList() != null))
			createIndex();
		PrintWriter output = getOutput();
		output.println("<html>\n  <body>\n    <ul>");
		List<String> words = getSearchWords();
		for (String word : words) {
			TopDocs topDocs = searchIndex(word, "content", 1000);
			displayIndex(topDocs, output, word);
		}
		output.println("    </ul>\n  </body>\n</html>\n");
		output.flush();
	}

	/**
	 * handle the given Throwable
	 * 
	 * @param t
	 */
	public void handle(Throwable t) {
		t.printStackTrace();
		usage(t.getMessage());
	}

	/**
	 * display usage
	 * 
	 * @param msg
	 *          - a message to be displayed (if any)
	 */
	public void usage(String msg) {
		System.err.println(msg);
		System.err.println("usage: java com.bitplan.pdfindexer.Pdfindexer");
		parser.printUsage(System.err);
		exitCode = 1;
	}

	/**
	 * main routine
	 * 
	 * @param args
	 */
	public int maininstance(String[] args) {
		parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			work();
			exitCode = 0;
		} catch (CmdLineException e) {
			// handling of wrong arguments
			usage(e.getMessage());
		} catch (Exception e) {
			handle(e);
			// System.exit(1);
			exitCode = 1;
		}
		return exitCode;
	}

	/**
	 * main routine
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		indexer = new Pdfindexer();
		int result = indexer.maininstance(args);
		if (!testMode)
			System.exit(result);
	}

}