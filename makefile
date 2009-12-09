# Standard Franz make rules forward to ant targets.

default: FORCE
	ant clean-build

clean: dist-clean
	ant clean

prepush: FORCE
	ant prepush

build: FORCE
	ant build

###############################################################################
## distribution building

## this is brittle, clean it up post-m2.

VERSION = 1.0m2
SERVER_VERSION = 4.0m2

DISTDIR = agraph-$(SERVER_VERSION)-client-java-$(VERSION)

TARNAME = $(DISTDIR).tar.gz

TUTORIAL_FILES = *.ntriples *.rdf *.txt TutorialExamples.java

dist: clean build
	mkdir -p DIST
	sed 's|SERVER_VERSION|$(SERVER_VERSION)|g' templates/.project > DIST/.project
	sed 's|agraph.jar|agraph-$(SERVER_VERSION).jar|g' templates/.classpath > DIST/.classpath
	mkdir -p DIST/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f DIST/src/tutorial; \
	done
	mkdir -p DIST/lib
	cp agraph.jar DIST/lib/agraph-$(SERVER_VERSION).jar
	cp lib/*.jar DIST/lib
	mkdir -p DIST/doc
	cp src/tutorial/java-tutorial-40.html DIST/doc
	cp src/tutorial/*.jpg DIST/doc
	tar -c -h -z -f $(TARNAME) -C DIST .

dist-clean: FORCE
	rm -fr DIST $(TARNAME)
	
ifdef DESTDIR
	cp -p $(TARNAME) $(DESTDIR)
endif

FORCE:
