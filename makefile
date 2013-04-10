# Standard Franz make rules forward to ant targets.

# AGVERSION is supposed to be the branch name without the leading "v".
AGVERSION ?= $(shell (git symbolic-ref -q HEAD 2>/dev/null || echo unknown) | sed 's,refs/heads/v,,')

ifeq ($(AGVERSION),unknown)
$(error AGVERSION not defined)
endif

default: build

clean: dist-clean
	ant clean

prepush: FORCE
	ant prepush

test-bigger: FORCE
	ant test-bigger

build: FORCE
	ant build

javadoc: FORCE
	ant javadoc

srcjar: FORCE
	ant srcjar

tags: FORCE
	rm -f TAGS
	find . -name '*.java' -print0 | xargs -0 etags -a

###############################################################################
## distribution building

## this is brittle, clean it up post-m2.

TUTORIAL_FILES = *.ntriples *.rdf *.txt *TutorialExamples.java

ifdef CUSTOMER_DIST
DISTDIR = agraph-$(AGVERSION)-client-java
DIST = DIST/$(DISTDIR)
TARNAME = DIST/$(DISTDIR).tar.gz
TAROPTS = --owner=root --group=root 
else
DISTDIR = .
DIST = DIST
TARNAME = agraph-$(AGVERSION)-client-java.tar.gz
TAROPTS = 
endif

dist: FORCE
	ant -DDIST=$(DIST) dist-init
	sed 's|SERVER_VERSION|$(AGVERSION)|g' templates/.project > $(DIST)/.project
	sed 's|agraph-VERSION|agraph-$(AGVERSION)|g' templates/.classpath > $(DIST)/.classpath
	mkdir -p $(DIST)/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f $(DIST)/src/tutorial; \
	done
	mkdir -p $(DIST)/lib
	cp agraph.jar $(DIST)/lib/agraph-$(AGVERSION).jar
	cp agraph-src.jar $(DIST)/lib/agraph-$(AGVERSION)-src.jar
	cp lib/json.jar $(DIST)/lib/json.jar
	cp lib/commons-pool-1.5.6.jar $(DIST)/lib/commons-pool-1.5.6.jar
	mkdir -p $(DIST)/lib/logging
	cp lib/logging/*.jar $(DIST)/lib/logging
	mkdir -p $(DIST)/lib/sesame
	cp lib/sesame/commons-*.jar $(DIST)/lib/sesame
	cp lib/sesame/commons-*.zip $(DIST)/lib/sesame
	cp lib/sesame/openrdf-sesame-2.6.8-onejar*.jar $(DIST)/lib/sesame
	mkdir -p $(DIST)/lib/jena
	cp lib/jena/*.jar $(DIST)/lib/jena
	rm $(DIST)/lib/jena/*-tests.jar
	rm $(DIST)/lib/jena/*-test-sources.jar
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
