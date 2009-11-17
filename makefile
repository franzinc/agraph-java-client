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

VERSION = 1.0m1

DISTDIR = agraph-4.0m1-client-java-$(VERSION)

TARNAME = $(DISTDIR).tar.gz

TUTORIAL_FILES = kennedy.ntriples relative_rules.txt \
		 vc-db-1.rdf lesmis.rdf TutorialExamples.java

dist: FORCE
	rm -f *.tar.gz
	rm -fr $(DISTDIR)
	mkdir -p $(DISTDIR)
	cp -r dist/* dist/.[a-z]* $(DISTDIR)
	mkdir -p $(DISTDIR)/src/tutorial
	for f in $(TUTORIAL_FILES); do \
	    echo copying src/tutorial/$$f...; \
	    cp src/tutorial/$$f $(DISTDIR)/src/tutorial; \
	done
	mkdir -p $(DISTDIR)/doc
	cp src/tutorial/java-tutorial-40.html $(DISTDIR)/doc
	cp src/tutorial/*.jpg $(DISTDIR)/doc
	tar -c -h -z --owner=root --group=root -f $(TARNAME) $(DISTDIR)
#	rm -fr $(DISTDIR)

FORCE:
