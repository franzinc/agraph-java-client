# Standard Franz make rules forward to ant targets.

default: FORCE
	ant clean-build

clean: dist-clean
	ant clean

prepush: FORCE
	ant prepush

test-bigger: FORCE
	ant test-bigger

build: FORCE
ifndef VERSION
	ant build
else
	ant -Denv.version=$(VERSION) build
endif

javadoc: FORCE
ifndef VERSION
	ant javadoc
else
	ant -Denv.version=$(VERSION) javadoc
endif

srcjar: FORCE
ifndef VERSION
	ant srcjar
else
	ant -Denv.version=$(VERSION) srcjar
endif

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

dist: FORCE
ifndef VERSION
	@echo VERSION is not defined.
	@exit 1
endif
	ant -Denv.version=$(VERSION) -DDIST=$(DIST) dist-init
	sed 's|SERVER_VERSION|$(VERSION)|g' templates/.project > $(DIST)/.project
	sed 's|agraph-VERSION|agraph-$(VERSION)|g' templates/.classpath > $(DIST)/.classpath
	mkdir -p $(DIST)/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f $(DIST)/src/tutorial; \
	done
	mkdir -p $(DIST)/lib
	cp agraph.jar $(DIST)/lib/agraph-$(VERSION).jar
	cp agraph-src.jar $(DIST)/lib/agraph-$(VERSION)-src.jar
	cp lib/json.jar $(DIST)/lib/json.jar
	mkdir -p $(DIST)/lib/sesame-2.3.2
	cp lib/sesame-2.3.2/*.jar $(DIST)/lib/sesame-2.3.2
	mkdir -p $(DIST)/lib/jena-2.6.2
	cp lib/jena-2.6.2/*.jar $(DIST)/lib/jena-2.6.2
	mkdir -p $(DIST)/doc
	cp src/tutorial/java-tutorial-40.html $(DIST)/doc
	cp src/tutorial/jena-tutorial-40.html $(DIST)/doc
	cp src/tutorial/*.jpg $(DIST)/doc
	cp -r doc $(DIST)/javadoc
	tar -c -h -z $(TAROPTS) -f $(TARNAME) -C DIST $(DISTDIR)
ifdef DESTDIR
	cp -p $(TARNAME) $(DESTDIR)
endif

dist-clean: FORCE
	rm -fr DIST *.tar.gz

FORCE:
