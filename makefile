# Standard Franz make rules forward to ant targets.

USRFIANT = $(shell if test -d /usr/fi/ant; then echo yes; fi)
ifeq ($(USRFIANT),yes)
export ANT_HOME = /usr/fi/ant
endif

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

test-broken: FORCE
	ant test-broken

test-stress: FORCE
	ant test-stress

test-temp: FORCE
	ant test-temp

test-stress-events: FORCE
	ant test-stress-events

lubm-prolog: FORCE
	ant lubm-prolog

lubm-sparql: FORCE
	ant lubm-sparql

tutorial: FORCE
	ant tutorial

jena-tutorial: FORCE
	ant jena-tutorial

sparql-query-tests: FORCE
	ant sparql-query-tests

sparql-update-tests: FORCE
	ant sparql-update-tests

repository-connection-tests: FORCE
	ant repository-connection-tests

repository-tests: FORCE
	ant repository-tests

jena-compliance-tests: FORCE
	ant jena-compliance-tests

callimachus-tests: FORCE
	ant  callimachus-tests

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
	cp lib/json.jar $(DIST)/lib/
	cp lib/commons-io-2.4.jar $(DIST)/lib/
	cp lib/commons-pool-1.5.6.jar $(DIST)/lib/
	mkdir -p $(DIST)/lib/logging
	cp lib/logging/*.jar $(DIST)/lib/logging/
	mkdir -p $(DIST)/lib/sesame
	cp lib/sesame/commons-*.jar $(DIST)/lib/sesame/
	cp lib/sesame/commons-*.zip $(DIST)/lib/sesame/
	cp lib/sesame/openrdf-sesame-2.7.11-onejar*.jar $(DIST)/lib/sesame/
	mkdir -p $(DIST)/lib/jena/
	cp lib/jena/*.jar $(DIST)/lib/jena/
	rm $(DIST)/lib/jena/*-tests.jar
	rm $(DIST)/lib/jena/*-test-sources.jar
	mkdir -p $(DIST)/doc
	cp src/tutorial/java-tutorial.html $(DIST)/doc/
	cp src/tutorial/jena-tutorial.html $(DIST)/doc/
	cp src/tutorial/*.jpg $(DIST)/doc/
	cp -r doc $(DIST)/javadoc
	tar -c -h -z $(TAROPTS) -f $(TARNAME) -C DIST $(DISTDIR)
ifdef DESTDIR
	cp -p $(TARNAME) $(DESTDIR)
endif

dist-clean: FORCE
	rm -fr DIST *.tar.gz

FORCE:
