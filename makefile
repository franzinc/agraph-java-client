# Standard Franz make rules forward to Maven.

# Client version, not AG version. It must match the POM.
override VERSION = $(shell ./version.sh)

RELEASE_VERSION = $(VERSION:-SNAPSHOT=)

export AGRAPH_HOST ?= $(shell echo $${AGRAPH_HOST:-localhost})
export AGRAPH_PORT ?= $(shell echo $${AGRAPH_PORT-`cat ../agraph/lisp/agraph.port 2> /dev/null || echo 10035`})

# For tutorial
example ?= all

# Common arguments used with mvn exec:java
MVN_EXEC_ARGS = -Dexec.cleanupDaemonThreads=false -Dexec.classpathScope=test

# Shortcut to run mvn exec:java
EXEC_JAVA = mvn exec:java $(MVN_EXEC_ARGS) 

# Repo directory used to deploy the artifact locally, for use by the tutorials
REPO = $(abspath repo)

default: build

clean: dist-clean
	mvn clean

prepush: tutorial jena-tutorial attributes-tutorial
	mvn test -Dtests.include=test.TestSuites\$$Prepush
	# Force Java to use ASCII (i.e. not UTF-8) as the default encoding.
	env LC_ALL=C mvn test -Dtests.include=test.TestSuites\$$Unicode

test-bigger: test-stress test-stress-events

test-broken: FORCE
	mvn test -Dtests.include=test.TestSuites\$$Broken

test-stress: FORCE
	mvn test -Dtests.include=test.TestSuites\$$Prepush,test.TestSuites\$$Stress

test-stress-events: FORCE
	$(EXEC_JAVA) -Dexec.mainClass=test.stress.Events -Dexec.args="--catalog java-catalog --load 2 --query 2 --time 1 --size 100000"

lubm-prolog: FORCE
	mvn test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmProlog

lubm-sparql: FORCE
	mvn test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmSparql -Dexample=$(example)

tutorial: local-deploy
	cd tutorials/sesame && \
	mvn compile -Dmaven.repo.local=$(REPO) && \
	mvn exec:java -Dmaven.repo.local=$(REPO) -Dexec.args=$(example)

jena-tutorial: local-deploy
	cd tutorials/jena && \
	mvn compile -Dmaven.repo.local=$(REPO) && \
	mvn exec:java -Dmaven.repo.local=$(REPO) -Dexec.args=$(example)

attributes-tutorial: local-deploy
	cd tutorials/attributes && \
	mvn compile -Dmaven.repo.local=$(REPO) && \
	mvn exec:java -Dmaven.repo.local=$(REPO)

sparql-query-tests: FORCE
	mvn test -Dtests.include=test.openrdf.AGSparqlQueryTest

sparql-update-tests: FORCE
	mvn test -Dtests.include=test.openrdf.AGSparqlUpdateTest,test.openrdf.AGSparqlUpdateConformanceTest

repository-connection-tests: FORCE
	mvn test -Dtests.include=test.openrdf.repository.AGRepositoryConnectionTest

repository-tests: FORCE
	mvn test -Dtests.include=test.openrdf.repository.AGAllRepositoryTests

jena-compliance-tests: FORCE
	mvn test -Dtests.include=test.AGGraphMakerTest,\
	  test.AGGraphTest,\
	  test.AGModelTest,\
	  test.AGPrefixMappingTest,\
	  test.AGResultSetTest,\
	  test.AGReifierTest

test-release: FORCE
	python test-release/make-pom.py > test-release/pom.xml
	cd test-release && mvn test -Dtest=test.TestSuites\$$Prepush,test.TestSuites\$$Stress

local-deploy: FORCE
	mvn install -DskipTests=true -Dmaven.repo.local=$(REPO)

build: FORCE
	mvn compile

javadoc: FORCE
    # Note: if we do not call 'validate' explicitly, the plugin that
    # computes the current year will run too late.
	mvn validate javadoc:javadoc

srcjar: FORCE
	mvn source:jar

tags: FORCE
	rm -f TAGS
	find . -name '*.java' -print0 | xargs -0 etags -a

###############################################################################
## distribution building

# It is pretty hard to convince Maven to use another name.
DIST_DIR = agraph-java-client-$(VERSION)
# But we want our old name, and we want the AG version.
TARGET_DIST_DIR = agraph-$(AGVERSION)-client-java
ifdef CUSTOMER_DIST
TARDIR = DIST
TAROPTS = --owner=root --group=root
else
TARDIR = .
TAROPTS =
endif
TARNAME = $(TARDIR)/$(TARGET_DIST_DIR).tar.gz

deploy: FORCE
	./deploy.sh

dist: FORCE
# Make sure we're not trying to make a release with a snaphost version.
ifdef CUSTOMER_DIST
	if [[ $(VERSION) == *-SNAPSHOT ]]; then \
	   echo "ERROR: Cannot make a public release of a snapshot ($(VERSION))" 1>&2 ; \
	   exit 1; \
	fi
endif
	mvn -DskipTests=true package
	mkdir -p DIST
# Note that maven creates target/$(DIST_DIR)/$(DIST_DIR) for some reason.
# Rename the directory
	rm -rf target/$(DIST_DIR)/$(TARGET_DIST_DIR)
	mv target/$(DIST_DIR)/$(DIST_DIR) target/$(DIST_DIR)/$(TARGET_DIST_DIR)
	tar  -c -h -z $(TAROPTS) -f $(TARNAME) -C target/$(DIST_DIR) $(TARGET_DIST_DIR)
# Copy the result if requested.
ifdef DESTDIR
ifneq "$(DESTDIR)" "$(TARDIR)"
	mkdir -p $(DESTDIR)
	cp -p $(TARNAME) $(DESTDIR)/
endif
endif

prepare-release: FORCE
	./unsnapshot.sh

post-release: FORCE
	./increment-version.sh
	git push origin HEAD

dist-clean: FORCE
	rm -fr target

FORCE:
