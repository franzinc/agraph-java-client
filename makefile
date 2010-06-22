# Standard Franz make rules forward to ant targets.

default: FORCE
	ant clean-build

clean: dist-clean
	ant clean

prepush: FORCE
	ant prepush

build: FORCE
ifndef VERSION
	ant build
else
	ant -Denv.version=$(VERSION) build
endif

javadoc: FORCE
	ant -f javadoc.xml

tags: FORCE
	rm -f TAGS
	find . -name '*.java' -print0 | xargs -0 etags -a

###############################################################################
## distribution building

## this is brittle, clean it up post-m2.

TUTORIAL_FILES = *.ntriples *.rdf *.txt *TutorialExamples.java

ifdef CUSTOMER_DIST
DISTDIR = agraph-$(VERSION)-client-java
DIST = DIST/$(DISTDIR)
TARNAME = DIST/$(DISTDIR).tar.gz
TAROPTS = --owner=root --group=root 
else
DISTDIR = .
DIST = DIST
TARNAME = agraph-$(VERSION)-client-java.tar.gz
TAROPTS = 
endif

dist: clean build
ifndef VERSION
	@echo VERSION is not defined.
	@exit 1
endif
	rm -fr DIST
	mkdir -p $(DIST)
	cp LICENSE $(DIST)
	sed 's|SERVER_VERSION|$(VERSION)|g' templates/.project > $(DIST)/.project
	sed 's|agraph.jar|agraph-$(VERSION).jar|g' templates/.classpath > $(DIST)/.classpath
	mkdir -p $(DIST)/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f $(DIST)/src/tutorial; \
	done
	mkdir -p $(DIST)/lib
	cp agraph.jar $(DIST)/lib/agraph-$(VERSION).jar
	cp lib/json.jar $(DIST)/lib/json.jar
	mkdir -p $(DIST)/lib/sesame-2.3.1
	cp lib/sesame-2.3.1/*.jar $(DIST)/lib/sesame-2.3.1
	mkdir -p $(DIST)/lib/jena-2.6.2
	cp lib/jena-2.6.2/*.jar $(DIST)/lib/jena-2.6.2
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
