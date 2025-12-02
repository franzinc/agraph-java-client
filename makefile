# This reports important environment settigns whenever the makefile is evaluated

JAVA_HOME = /usr/lib/jvm/java-21
MVN = env JAVA_HOME=$(JAVA_HOME) mvn

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
EXEC_JAVA = $(MVN) $(MVN_ARGS) exec:java $(MVN_EXEC_ARGS)

# Repo directory used to deploy the artifact locally, for use by the tutorials
REPO = $(abspath repo)

export MAVEN_OPTS = --add-opens java.base/java.util=ALL-UNNAMED

default: build

clean: dist-clean clean-3rd-party-javadoc
	$(MVN) $(MVN_ARGS) clean

prepush: clean all-tutorials test-prepush test-unicode javadoc

test-bigger: test-prepush test-stress test-stress-events test-xa

test-unicode:
# Force Java to use ASCII (i.e. not UTF-8) as the default encoding.
	env LC_ALL=C $(MVN) $(MVN_ARGS) test -Dtest=test.suites.UnicodeTests

test-prepush:
	$(MVN) $(MVN_ARGS) test -Dtest=test.suites.PrepushTests

.PHONY: test-broken
test-broken:
	$(MVN) $(MVN_ARGS) test -Dtest=test.suites.BrokenTests

.PHONY: test-stress
test-stress:
	$(MVN) $(MVN_ARGS) test -Dtest=test.suites.StressTests

.PHONY: test-stress-events
test-stress-events:
	$(EXEC_JAVA) -Dexec.mainClass=test.stress.Events -Dexec.args="--catalog java-catalog --load 2 --query 2 --time 1 --size 100000"

.PHONY: test-xa
test-xa:
	$(MVN) $(MVN_ARGS) test -Dtest=XAAtomikosTests

.PHONY: lubm-prolog
lubm-prolog:
	$(MVN) $(MVN_ARGS) test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmProlog

.PHONY: lubm-sparql
lubm-sparql:
	$(MVN) $(MVN_ARGS) test-compile
	$(EXEC_JAVA) -Dexec.mainClass=test.lubm.AGLubmSparql -Dexample=$(example)

all-tutorials: tutorial jena-tutorial attributes-tutorial 2pc-tutorial agq-tutorial

tutorial: local-deploy
	cd tutorials/rdf4j && \
	$(MVN) $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	$(MVN) $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO) -Dexec.args=$(example)

jena-tutorial: local-deploy
	cd tutorials/jena && \
	$(MVN) $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	$(MVN) $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO) -Dexec.args=$(example)

attributes-tutorial: local-deploy
	cd tutorials/attributes && \
	$(MVN) $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	$(MVN) $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO)

2pc-tutorial: local-deploy
	cd tutorials/2pc && \
	$(MVN) $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	$(MVN) $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO)

failures-tutorial: local-deploy
	cd tutorials/failures && \
	$(MVN) $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	$(MVN) $(MVN_ARGS) exec:java -Dmaven.repo.local=$(REPO)


agq-tutorial: local-deploy
	cd tutorials/agq && \
	$(MVN) $(MVN_ARGS) compile -Dmaven.repo.local=$(REPO) && \
	$(MVN) $(MVN_ARGS) test -Dmaven.repo.local=$(REPO)

.PHONY: test-release
test-release:
	python3 test-release/make-pom.py > test-release/pom.xml
	cd test-release && $(MVN) $(MVN_ARGS) test -Dtest=test.suites.PrepushTests
	cd test-release && $(MVN) $(MVN_ARGS) test -Dtest=test.suites.StressTests

.PHONY: local-deploy
local-deploy:
	$(MVN) $(MVN_ARGS) install -DskipTests=true -Dmaven.repo.local=$(REPO)

.PHONY: build
build:
	$(MVN) $(MVN_ARGS) compile

clean-3rd-party-javadoc:
	rm -f json-20240205-javadoc.jar apache.httpcomponents.javadoc.jar com.atomikos.javadoc.jar
	rm -rf target/reports/apidocs/third-party-javadoc

target/reports/apidocs/third-party-javadoc:
	curl -o json-20240205-javadoc.jar $(MAVEN_REPO_URL)org/json/json/20240205/json-20240205-javadoc.jar
	curl -o apache.httpcomponents.javadoc.jar $(MAVEN_REPO_URL)org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13-javadoc.jar
	curl -o com.atomikos.javadoc.jar $(MAVEN_REPO_URL)com/atomikos/transactions-jta/4.0.6/transactions-jta-4.0.6-javadoc.jar
	mkdir -p target/reports/apidocs/third-party-javadoc/org/json
	mkdir -p target/reports/apidocs/third-party-javadoc/org/apache/httpcomponents
	mkdir -p target/reports/apidocs/third-party-javadoc/com/atomikos
	unzip -q -o json-20240205-javadoc.jar -d target/reports/apidocs/third-party-javadoc/org/json
	unzip -q -o apache.httpcomponents.javadoc.jar -d target/reports/apidocs/third-party-javadoc/org/apache/httpcomponents
	unzip -q -o com.atomikos.javadoc.jar -d target/reports/apidocs/third-party-javadoc/com/atomikos

.PHONY: javadoc
javadoc: target/reports/apidocs/third-party-javadoc
    # Note: if we do not call 'validate' explicitly, the plugin that
    # computes the current year will run too late.
	$(MVN) $(MVN_ARGS) validate javadoc:javadoc
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
	$(MVN) $(MVN_ARGS) checkstyle:check

.PHONY: srcjar
srcjar:
	$(MVN) $(MVN_ARGS) source:jar

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
deploy:
	$(MVN) -Prelease --batch-mode -DskipTests=true clean deploy

.PHONY: dist
dist:
	$(MVN) -Prelease --batch-mode -DskipTests=true clean package
# Note that maven creates target/$(PACKAGE_NAME)/$(PACKAGE_NAME) for
# some reason.
	rm -fr $(DIST_DIR)
	mkdir -p $(DIST_DIR)
	tar -c -h -z -f $(TARNAME) -C target/$(PACKAGE_NAME) $(PACKAGE_NAME)

# The updateweb call will only work for layer (or other SA people)
publish-dist: dist
	cp -p $(TARNAME) RELEASE-HISTORY.md /fi/ftp/pub/agraph/java-client/
	-ssh -n cobweb updateweb --ftp

.PHONY: dist-clean
dist-clean:
	rm -fr target
