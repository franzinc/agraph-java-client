# Standard Franz make rules forward to ant targets.

default: FORCE
	ant clean-build

clean: FORCE
	ant clean

prepush: FORCE
	ant prepush

build: FORCE
	ant build

###############################################################################
## distribution building

VERSION = 1.0m1a
SERVER_VERSION = 4.0m1a

DISTDIR = agraph-$(SERVER_VERSION)-client-java-$(VERSION)

TARNAME = $(DISTDIR).tar.gz

TUTORIAL_FILES = kennedy.ntriples relative_rules.txt \
		 vc-db-1.rdf lesmis.rdf TutorialExamples.java

dist: FORCE
	rm -fr DIST
	mkdir -p DIST/$(DISTDIR)
	cp -r dist/* dist/.[a-z]* DIST/$(DISTDIR)
	mkdir -p DIST/$(DISTDIR)/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f DIST/$(DISTDIR)/src/tutorial; \
	done
	mkdir -p DIST/$(DISTDIR)/doc
	cp src/tutorial/java-tutorial-40.html DIST/$(DISTDIR)/doc
	cp src/tutorial/*.jpg DIST/$(DISTDIR)/doc
	tar -c -h -z --owner=root --group=root -f DIST/$(TARNAME) -C DIST $(DISTDIR)
ifdef DESTDIR
	cp -p DIST/$(TARNAME) $(DESTDIR)
endif

FORCE:
