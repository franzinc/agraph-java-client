# Standard Franz make rules forward to ant targets.

default: FORCE
	ant clean-build

clean: dist-clean
	ant clean

prepush: FORCE
	ant prepush

build: FORCE
	ant build

tags: FORCE
	rm -f TAGS
	find . -name '*.java' -print0 | xargs -0 etags -a

###############################################################################
## distribution building

## this is brittle, clean it up post-m2.

VERSION = 1.0m3
SERVER_VERSION = 4.0m3


TUTORIAL_FILES = *.ntriples *.rdf *.txt *TutorialExamples.java

ifdef CUSTOMER_DIST
DISTDIR = agraph-client-java-$(SERVER_VERSION)
DIST = DIST/$(DISTDIR)
TARNAME = DIST/$(DISTDIR).tar.gz
TAROPTS = --owner=root --group=root 
else
DISTDIR = .
DIST = DIST
TARNAME = agraph-client-java-$(SERVER_VERSION).tar.gz
TAROPTS = 
endif

dist: clean build
	rm -fr DIST
	mkdir -p $(DIST)
	cp LICENSE $(DIST)
	sed 's|SERVER_VERSION|$(SERVER_VERSION)|g' templates/.project > $(DIST)/.project
	sed 's|agraph.jar|agraph-$(SERVER_VERSION).jar|g' templates/.classpath > $(DIST)/.classpath
	mkdir -p $(DIST)/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f $(DIST)/src/tutorial; \
	done
	mkdir -p $(DIST)/lib
	cp agraph.jar $(DIST)/lib/agraph-$(SERVER_VERSION).jar
	mkdir -p $(DIST)/lib/sesame-2.2.4
	cp lib/sesame-2.2.4/*.jar $(DIST)/lib/sesame-2.2.4
	mkdir -p $(DIST)/lib/jena-2.6.0
	cp lib/jena-2.6.0/*.jar $(DIST)/lib/jena-2.6.0
	mkdir -p $(DIST)/doc
	cp src/tutorial/java-tutorial-40.html $(DIST)/doc
	cp src/tutorial/jena-tutorial-40.html $(DIST)/doc
	cp src/tutorial/*.jpg $(DIST)/doc
	tar -c -h -z $(TAROPTS) -f $(TARNAME) -C DIST $(DISTDIR)
ifdef DESTDIR
	cp -p $(TARNAME) $(DESTDIR)
endif

dist-clean: FORCE
	rm -fr DIST *.tar.gz

FORCE:
