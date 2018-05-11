package com.franz.versionrewriter;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Main {
    // Used to compile XPath expressions
    private static final XPathFactory xpathFactory = XPathFactory.instance();

    /**
     * Reads the text of the first XML element found by an XPath expression.
     *
     * @param path Path to an XML file.
     * @param xpath An XPath expression.
     * @return Text of the first element found.
     */
    private static String read(final File path, final String xpath) {
        final SAXBuilder builder = new SAXBuilder();
        final Document doc;

        try {
            doc = builder.build(path);
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }
        final XPathExpression<Element> expr = xpathFactory.compile(xpath, Filters.element());
        final List<Element> result = expr.evaluate(doc);
        if (result.isEmpty()) {
            throw new RuntimeException("XPath expression " + expr + " does not match anything.");
        }
        return result.get(0).getText();
    }

    /**
     * Modifies an XML document by transforming the text of all XML elements matching
     * a given XPath expression.
     *
     * @param doc Parsed XML document.
     * @param xpath XPath expression matching elements to be modified.
     * @param func Function used to transform matching elements' text.
     */
    private static void transform(final Document doc,
                                  final String xpath,
                                  final Function<? super String, String> func) {
        final XPathExpression<Element> expr = xpathFactory.compile(xpath, Filters.element());
        for (final Element elt : expr.evaluate(doc)) {
            elt.setText(func.apply(elt.getText()));
        }
    }

    /**
     * Modifies an XML document by transforming the text of all XML elements matching
     * a given XPath expression.
     *
     * @param path Path to an XML file.
     * @param xpath XPath expression matching elements to be modified.
     * @param func Function used to transform matching elements' text.
     */
    private static void transform(final File path,
                                  final String xpath,
                                  final Function<? super String, String> func) {
        final SAXBuilder builder = new SAXBuilder();
        final Document doc;

        try {
            doc = builder.build(path);
        } catch (IOException |JDOMException e) {
            throw new RuntimeException(e);
        }

        transform(doc, xpath, func);

        final XMLOutputter outputter = new XMLOutputter(Format.getRawFormat());
        outputter.getFormat().setLineSeparator(LineSeparator.UNIX);
        try (final FileOutputStream out = new FileOutputStream(path)) {
        outputter.output(doc, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads file contents into a string.
     *
     * @param file Path to a text file.
     * @param charset Character encoding used by the file.
     * @return File's contents.
     */
    private static String slurp(final File file, final Charset charset) {
        final StringBuilder builder = new StringBuilder((int)file.length());
        final char[] buffer = new char[(int)file.length()];
        int read;

        try (final InputStream input = new FileInputStream(file);
             final Reader reader = new InputStreamReader(input, charset)) {
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }

    /**
     * Reads file contents into a string.
     *
     * The file must be encoded in UTF-8.
     *
     * @param file Path to a text file.
     * @return File's contents.
     */
    private static String slurp(final File file) {
        return slurp(file, StandardCharsets.UTF_8);
    }

    /**
     * Writes a string to a file, replacing current content if the file exists.
     *
     * @param file Path to a file.
     * @param newText New file content.
     * @param charset Character encoding to use.
     */
    private static void rewrite(final File file, final String newText, final Charset charset) {
        try (final OutputStream output = new FileOutputStream(file);
             final Writer writer = new OutputStreamWriter(output, charset)) {
        writer.write(newText);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a string to a file, replacing current content if the file exists.
     *
     * Uses UTF-8 to encode the text.
     *
     * @param file Path to a file.
     * @param newText New file content.
     */
    private static void rewrite(final File file, final String newText) {
        rewrite(file, newText, StandardCharsets.UTF_8);
    }

    /**
     * Returns paths to pom.xml files for the tutorials.
     *
     * @param agraphDir Path to the source directory.
     *
     * @return A list of paths to pom.xml files.
     */
    private static File[] findTutorials(final File agraphDir) {
        final List<File> pomFiles = new ArrayList<>();

        final FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("pom.xml")) {
                    pomFiles.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(agraphDir.toPath().resolve("tutorials"), visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pomFiles.toArray(new File[pomFiles.size()]);
    }

    /**
     * Performs a regex replacement on a file (in place).
     *
     * @param file Path to the file.
     * @param regex Expression to be replaced.
     * @param replacement Replacement text.
     */
    private static void replaceInFile(final File file, final String regex, final String replacement) {
        rewrite(file, slurp(file).replaceAll(regex, replacement));
    }

    /**
     * Creates a string transformer that always returns the same value.
     *
     * @param text Value to be returned by the transformer.
     *
     * @return A function object that always returns 'text'.
     */
    private static Function<Object, String> replaceWith(final String text) {
        return (x) -> text;
    }

    /**
     * Removes all occurrences of '-SNAPSHOT' from a string.
     *
     * @param version String to be modified.
     *
     * @return A new string with -SNAPSHOT removed.
     */
    private static String stripSnapshot(final String version) {
        return version.replace("-SNAPSHOT", "");
    }

    /**
     * Parses a version number (X.Y.Z-whatever) and increments 
     * the last component by one.
     *
     * @param version Version number.
     *
     * @return Modified version number.
     */
    private static String incrementVersion(final String version) {
        final String[] components = version.split("\\.");
        final int last = components.length - 1;
        final String[] lastComponents = components[last].split("-");
        final int lastValue = Integer.parseInt(lastComponents[0]);
        lastComponents[0] = Integer.toString(lastValue + 1); 
        components[last] = String.join("-", (CharSequence[])lastComponents);
        return String.join(".", (CharSequence[]) components);
    }

    /**
     * Update the version in all POM files.
     *
     * @param baseDir Path to the top-level directory of this project.
     * @param version New version string.
     */     
    private static void changePoms(final File baseDir, final String version) {
        final File agraphDir = new File(baseDir, "..");
        final File pom = new File(agraphDir, "pom.xml");
        final File tutorialDir = new File(agraphDir, "tutorials");
        final File tutorialParent = new File(tutorialDir, "pom.xml");

        transform(pom, "/project/version", replaceWith(version));

        transform(tutorialParent, "/project/version", replaceWith(version));
        
        for (final File tutorialPom : findTutorials(agraphDir)) {
            transform(tutorialPom, "/project/parent/version", replaceWith(version));
        }            
    }
    
    /**
     * Removes SNAPSHOT from versions in all POM files and updates the version number
     * in README.
     *
     * @param baseDir Path to the top-level directory of this project.
     * @param snapshotVersion Current client version.
     */
    private static void prepareRelease(final File baseDir, final String snapshotVersion) {
        final String releaseVersion = stripSnapshot(snapshotVersion);
        System.out.println(snapshotVersion + " -> " + releaseVersion);

        final File agraphDir = new File(baseDir, "..");
        final File readme = new File(agraphDir, "README.adoc");
        changePoms(baseDir, releaseVersion);
        replaceInFile(readme, ":version:\\s*[0-9.]*", ":version: " + releaseVersion);
    }

    /**
     * Changes the version to a specified snapshot version.
     *
     * The current version can be either a snapshot or a release.
     *
     * @param baseDir Path to the top-level directory of this project.
     * @param oldVersion Current client version.
     * @param newVersion New version, without the -SNAPSHOT qualifier.
     */
    private static void prepareSnapshot(final File baseDir,
                                        final String oldVersion,
                                        final String newVersion) {
        final String snapshotVersion = 
            newVersion.endsWith("-SNAPSHOT") ? newVersion : newVersion + "-SNAPSHOT";
        System.out.println(oldVersion + " -> " + snapshotVersion);
        changePoms(baseDir, snapshotVersion);
    }

    /**
     * Changes the version from a release to the next snapshot (increments the patchlevel).
     *
     * @param baseDir Path to the top-level directory of this project.
     * @param oldVersion Current client version.
     */
    private static void prepareNextSnapshot(final File baseDir, final String oldVersion) {
        prepareSnapshot(baseDir, oldVersion, incrementVersion(oldVersion));
    }

    /**
     * Main entry point.
     * @param args Command-line arguments.
     */
    public static void main(final String... args) {
        final File baseDir;
        final String newVersion;

        if (args.length > 0) {
            baseDir = new File(args[0]);
        } else {
            baseDir = new File(System.getProperty("user.dir"));
        }

        if (args.length > 1) {
            newVersion = args[1];
        } else {
            newVersion = null;
        }

        final String version = read(new File(baseDir, "../pom.xml"),
                                             "/project/version");
        if (newVersion != null) {
            if (newVersion.equalsIgnoreCase("next-snapshot")) {
                prepareNextSnapshot(baseDir, version);
            } else {
                prepareSnapshot(baseDir, version, newVersion);
            }
        } else if (version.endsWith("-SNAPSHOT")) {
            prepareRelease(baseDir, version);
        } else {
            prepareNextSnapshot(baseDir, version);
        }
    }
}
