# This reports important environment settigns whenever the makefile is evaluated
$(info $(shell mvn --version))

# Client version, not AG version.  This is extracted from pom.xml
override VERSION = $(shell ./version.sh)

export AGRAPH_HOST ?= $(shell echo $${AGRAPH_HOST:-localhost})
# Export LABEL, if label is specified prepend a dash to it otherwise leave it blank
export LABEL = $(if $(label),-$(label))
export AGRAPH_PORT ?= $(shell echo $${AGRAPH_PORT-`cat ../agraph/lisp/agraph$(LABEL).port 2> /dev/null || echo 10035`})
export MAVEN_REPO_URL ?= https://repo1.maven.org/maven2/

# For tutorial
example ?= all

SETTING_XML=$(abspath settings.xml)

# Arguments to use for all mvn invocations
MVN_ARGS=-s $(SETTING_XML)

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

export MAVEN_OPTS = --add-opens java.base/java.util=ALL-UNNAMED

default: build

clean: dist-clean
	mvn $(MVN_ARGS) clean

prepush: clean all-tutorials test-prepush test-unicode javadoc

test-bigger: test-prepush test-stress test-stress-events test-xa

test-unicode:
# Force Java to use ASCII (i.e. not UTF-8) as the default encoding.
	env LC_ALL=C mvn $(MVN_ARGS) test -Dtest=test.suites.UnicodeTests

test-prepush:
	mvn $(MVN_ARGS) test -Dtest=test.suites.PrepushTests

.PHONY: test-broken
test-broken:
	mvn $(MVN_ARGS) test -Dtest=test.suites.BrokenTests

.PHONY: test-stress
test-stress:
	mvn $(MVN_ARGS) test -Dtest=test.suites.StressTests

.PHONY: test-stress-events
test-stress-events:
	$(EXEC_JAVA) -Dexec.mainClass=test.stress.Events -Dexec.args="--catalog java-catalog --load 2 --query 2 --time 1 --size 100000"

.PHONY: test-xa
test-xa:
	mvn $(MVN_ARGS) test -Dtest=XAAtomikosTests

.PHONY: lubm-prolog
lubm-prolog:
	mvn $(MVN_ARGS) test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmProlog

.PHONY: lubm-sparql
lubm-sparql:
	mvn $(MVN_ARGS) test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmSparql -Dexample=$(example)

all-tutorials: tutorial jena-tutorial attributes-tutorial 2pc-tutorial agq-tutorial

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


agq-tutorial: local-deploy
	cd tutorials/agq && \
	mvn $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	mvn $(MVN_ARGS) test -Dmaven.repo.local=$(REPO)

.PHONY: test-release
test-release:
	python3 test-release/make-pom.py > test-release/pom.xml
	cd test-release && mvn $(MVN_ARGS) test -Dtest=test.suites.PrepushTests
	cd test-release && mvn $(MVN_ARGS) test -Dtest=test.suites.StressTests

.PHONY: local-deploy
local-deploy:
	mvn $(MVN_ARGS) install -DskipTests=true -Dmaven.repo.local=$(REPO)

.PHONY: build
build:
	mvn $(MVN_ARGS) compile

.PHONY: javadoc
javadoc:
    # Note: if we do not call 'validate' explicitly, the plugin that
    # computes the current year will run too late.
	mvn $(MVN_ARGS) validate javadoc:javadoc
	rm -rf doc
	cp -r target/reports/apidocs doc
####### HACK HACK HACK HACK HACK HACK HACK HACK HACK...
# The javadoc generated HTML is bogus.  There is no definition for the
# "#com.franz" bookmark.  Since we don't control javadoc we have to
# remove the link.  It's useless anyway, since there's no way to
# map a list of classes (what the "*")
	sed -i -E 's,<a href="#com.franz">com.franz.*</a>,com.franz.*,' doc/constant-values.html
####### ...HACK HACK HACK HACK HACK HACK HACK HACK HACK

.PHONY: checkstyle
checkstyle:
	mvn $(MVN_ARGS) checkstyle:check

.PHONY: srcjar
srcjar:
	mvn $(MVN_ARGS) source:jar

.PHONY: tags
tags:
	rm -f TAGS
	find . -name '*.java' -print0 | xargs -0 etags -a

###############################################################################
## distribution building

# It is pretty hard to convince Maven to use another name.
PACKAGE_NAME = agraph-java-client-$(VERSION)
DIST_DIR=DIST
TARNAME = $(DIST_DIR)/$(PACKAGE_NAME).tar.gz

.PHONY: deploy
deploy: stage release-staged

.PHONY: stage
stage:
	env AG_SKIP_TESTS=xxx ./deploy.sh

.PHONY: release-staged
release-staged:
	mvn nexus-staging:release $(MVN_STAGED_OPTS)

.PHONY: dist
dist:
	mvn -DskipTests=true package
# Note that maven creates target/$(PACKAGE_NAME)/$(PACKAGE_NAME) for
# some reason.
	rm -fr $(DIST_DIR)
	mkdir -p $(DIST_DIR)
	tar -c -h -z -f $(TARNAME) -C target/$(PACKAGE_NAME) $(PACKAGE_NAME)

publish-dist: dist
	cp -p $(TARNAME) RELEASE-HISTORY.md /fi/ftp/pub/agraph/java-client/

# This is used to delete a partially staged release
.PHONY: drop-staged
drop-staged:
	mvn nexus-staging:drop $(MVN_STAGED_OPTS)

.PHONY: list-staged
list-staged:
	@mvn nexus-staging:rc-list | grep franz || echo 'No staged releases found'.

.PHONY: dist-clean
dist-clean:
	rm -fr target
