# Standard Franz make rules forward to Maven.

# Client version, not AG version.  This is extracted from pom.xml
override VERSION = $(shell ./version.sh)

export AGRAPH_HOST ?= $(shell echo $${AGRAPH_HOST:-localhost})
# Export LABEL, if label is specified prepend a dash to it otherwise leave it blank
export LABEL = $(if $(label),-$(label))
export AGRAPH_PORT ?= $(shell echo $${AGRAPH_PORT-`cat ../agraph/lisp/agraph$(LABEL).port 2> /dev/null || echo 10035`})

# For tutorial
example ?= all

# Arguments to use for all mvn invocations
MVN_ARGS=

# Enable batch mode for CI
ifdef batch
	MVN_ARGS := $(MVN_ARGS) --batch-mode
endif

# Common arguments used with mvn exec:java
MVN_EXEC_ARGS = $(MVN_ARGS) -Dexec.cleanupDaemonThreads=false -Dexec.classpathScope=test

# Shortcut to run mvn exec:java
EXEC_JAVA = mvn $(MVN_ARGS) exec:java $(MVN_EXEC_ARGS)

# Repo directory used to deploy the artifact locally, for use by the tutorials
REPO = $(abspath repo)

# Options used by release-staged and drop-staged
ifdef STAGING_ID
	MVN_STAGED_OPTS=-DstagingRepository.id=$(STAGING_ID)
else
	MVN_STAGED_OPTS=
endif

default: build

clean: dist-clean
	mvn $(MVN_ARGS) clean

prepush: clean all-tutorials agq-tests jena-compliance-tests javadoc
	mvn $(MVN_ARGS) test -Dtests.include=test.TestSuites\$$Prepush
	# Force Java to use ASCII (i.e. not UTF-8) as the default encoding.
	env LC_ALL=C mvn $(MVN_ARGS) test -Dtests.include=test.TestSuites\$$Unicode

test-bigger: test-stress test-stress-events test-xa

test-broken: FORCE
	mvn $(MVN_ARGS) test -Dtests.include=test.TestSuites\$$Broken

test-stress: FORCE
	mvn $(MVN_ARGS) test -Dtests.include=test.TestSuites\$$Prepush,test.TestSuites\$$Stress

test-stress-events: FORCE
	$(EXEC_JAVA) -Dexec.mainClass=test.stress.Events -Dexec.args="--catalog java-catalog --load 2 --query 2 --time 1 --size 100000"

test-xa: FORCE
	mvn $(MVN_ARGS) test -Dtest=XAAtomikosTests

lubm-prolog: FORCE
	mvn $(MVN_ARGS) test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmProlog

lubm-sparql: FORCE
	mvn $(MVN_ARGS) test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmSparql -Dexample=$(example)

all-tutorials: tutorial jena-tutorial attributes-tutorial 2pc-tutorial

tutorial: local-deploy
	cd tutorials/rdf4j && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO) -Dexec.args=$(example)

jena-tutorial: local-deploy
	cd tutorials/jena && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO) -Dexec.args=$(example)

attributes-tutorial: local-deploy
	cd tutorials/attributes && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO)

2pc-tutorial: local-deploy
	cd tutorials/2pc && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO)

failures-tutorial: local-deploy
	cd tutorials/failures && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO)


agq-tests: local-deploy
	cd tutorials/agq && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) test -Dmaven.repo.local=$(REPO)

sparql-query-tests: FORCE
	mvn $(MVN_ARGS) test -Dtests.include=test.openrdf.AGSparqlQueryTest

sparql-update-tests: FORCE
	mvn $(MVN_ARGS) test -Dtests.include=test.openrdf.AGSparqlUpdateTest,test.openrdf.AGSparqlUpdateConformanceTest

repository-connection-tests: FORCE
	mvn $(MVN_ARGS) test -Dtests.include=test.openrdf.repository.AGRepositoryConnectionTest

repository-tests: FORCE
	mvn $(MVN_ARGS) test -Dtests.include=test.openrdf.repository.AGAllRepositoryTests

jena-compliance-tests: FORCE
	mvn $(MVN_ARGS) test -Dtests.include="test.AGGraphMakerTest,\
			test.AGGraphTest,\
			test.AGModelTest,\
			test.AGPrefixMappingTest,\
			test.AGResultSetTest,\
			test.AGReifierTest"

test-release: FORCE
	python test-release/make-pom.py > test-release/pom.xml
	cd test-release && mvn $(MVN_ARGS) test -Dtest=test.TestSuites\$$Prepush,test.TestSuites\$$Stress

local-deploy: FORCE
	mvn $(MVN_ARGS) install -DskipTests=true -Dmaven.repo.local=$(REPO)

build: FORCE
	mvn $(MVN_ARGS) compile

javadoc: FORCE
    # Note: if we do not call 'validate' explicitly, the plugin that
    # computes the current year will run too late.
	mkdir -p target   # make sure the output directory exists...
	mvn $(MVN_ARGS) validate javadoc:javadoc | tee target/javadoc.log
	@if grep -q ^"\[WARNING\]" target/javadoc.log; then \
            echo "[ERROR] Javadoc warnings found,"; \
            exit 1; \
        fi

checkstyle: FORCE
	mvn $(MVN_ARGS) checkstyle:check

srcjar: FORCE
	mvn $(MVN_ARGS) source:jar

tags: FORCE
	rm -f TAGS
	find . -name '*.java' -print0 | xargs -0 etags -a

###############################################################################
## distribution building

# It is pretty hard to convince Maven to use another name.
PACKAGE_NAME = agraph-java-client-$(VERSION)
DIST_DIR=DIST
TARNAME = $(DIST_DIR)/$(PACKAGE_NAME).tar.gz

stage: FORCE
	./deploy.sh

deploy: stage release-staged

release-staged: FORCE
	mvn nexus-staging:release $(MVN_STAGED_OPTS)

drop-staged: FORCE
	mvn nexus-staging:drop $(MVN_STAGED_OPTS)

list-staged: FORCE
	@mvn nexus-staging:rc-list | grep franz || echo 'No staged releases found'.

dist: FORCE
	mvn -DskipTests=true package
	@# Note that maven creates target/$(PACKAGE_NAME)/$(PACKAGE_NAME) for some reason.
	rm -fr $(DIST_DIR)
	mkdir -p $(DIST_DIR)
	tar -c -h -z --owner=root --group=root -f $(TARNAME) -C target/$(PACKAGE_NAME) $(PACKAGE_NAME)

publish-dist: dist
	cp $(TARNAME) RELEASE-HISTORY.md /fi/ftp/pub/agraph/java-client/

prepare-release: FORCE
	./unsnapshot.sh

post-release: FORCE
	./increment-version.sh
	git push gerrit HEAD:refs/for/master%submit

dist-clean: FORCE
	rm -fr target

FORCE:
