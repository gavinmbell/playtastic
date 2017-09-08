#------------------------------------
# Vars...
#------------------------------------
AUTHOR := Gavin M. Bell
PROJECT := playtastic
VERSION := 0.1.0-SNAPSHOT
RELEASE_NAME := flatbush
REPO_SITE = https://github.com/gavinmbell/$(PROJECT)
ORGANIZATION := Hubrick
DATA_DIR ?= resources
LEIN := ./bin/lein
PROFILE := user

#------------------------------------

SOURCE_PATHS := src
ETC_PATH := etc
TARGET_PATH := target
CLJ_FILES := $(shell find $(SOURCE_PATHS) -name '*.clj')
DOC_FILES := $(shell find . -name '*.md')
MAIN := $(PROJECT).core
MAIN_CLASS_FILE := $(TARGET_PATH)/classes/$(PROJECT)/core__init.class
JAR_FILE := $(TARGET_PATH)/$(PROJECT)-$(VERSION)-standalone.jar

BRANCH := $(shell git branch | grep '*' | sed -n 's/\*[ ]*//p' | xargs)
COMMIT := $(shell git log -p -n1 | sed -n -e 's/^commit \(.*\)/\1/p'   | xargs)
COMMIT_DATE :=   $(shell git log -p -n1 | sed -n -e 's/^Date: \(.*\)/\1/p'   | xargs)
COMMIT_AUTHOR := $(shell git log -p -n1 | sed -n -e 's/^Author: \(.*\)/\1/p' | xargs)
BUILD_DATE := $(shell date)

TEMPLATE := $(ETC_PATH)/project.clj.tmpl

#------------------------------------
# Targets...
#------------------------------------

all: clean-all dist doc

#-------
# Generates clojure's project.clj file from a template file
# Here (https://github.com/technomancy/leiningen/blob/master/sample.project.clj)
# is the full project.clj file with all the default values explicitly set
#-------
project.clj: $(TEMPLATE)
	@echo "Generating project.clj from template..."
	@sed -e 's|@@ORGANIZATION@@|$(ORGANIZATION)|g' \
	     -e 's|@@AUTHOR@@|$(AUTHOR)|g' \
	     -e 's|@@REPO_SITE@@|$(REPO_SITE)|g' \
	     -e 's|@@PROJECT@@|$(PROJECT)|g' \
	     -e 's|@@VERSION@@|$(VERSION)|g' \
	     -e 's|@@CD_PACKAGE_VERSION_STAMP@@|$(CD_PACKAGE_VERSION_STAMP)|g' \
	     -e 's|@@MODULE@@|$(MODULE)|g' \
	     -e 's|@@BRANCH@@|$(BRANCH)|g' \
	     -e 's|@@COMMIT@@|$(COMMIT)|g' \
	     -e 's|@@COMMIT_AUTHOR@@|$(COMMIT_AUTHOR)|g' \
	     -e 's|@@COMMIT_DATE@@|$(COMMIT_DATE)|g' \
	     -e 's|@@RESOURCE_PATHS@@|$(RESOURCE_PATHS)|g' \
	     -e 's|@@SOURCE_PATHS@@|$(SOURCE_PATHS)|g' \
	     -e 's|@@TEST_PATHS@@|$(TEST_PATHS)|g' \
	     -e 's|@@COMPILE_PATH@@|$(COMPILE_PATH)|g' \
	     -e 's|@@MAIN@@|$(MAIN)|g' \
	     -e 's|@@BUILD_DATE@@|$(BUILD_DATE)|g' \
	     -e 's|@@PROFILE@@|$(PROFILE)|g' \
	     -e 's|@@RELEASE_NAME@@|$(RELEASE_NAME)|g' $(TEMPLATE) > $(shell sed -n 's|.*/\(.*\).tmpl|\1|p' <<< $(TEMPLATE))


$(MAIN_CLASS_FILE): project.clj $(CLJ_FILES)
	$(LEIN) compile
	@touch $@

compile: $(MAIN_CLASS_FILE)

clean: project.clj
	$(LEIN) clean

clean-all: clean
	$(shell ls *.csv >& /dev/null && rm -v *.csv >& /dev/null)

$(JAR_FILE): $(MAIN_CLASS_FILE)
	LEIN_SNAPSHOTS_IN_RELEASE=1 ${LEIN} with-profile $(PROFILE) uberjar

doc: project.clj $(CLJ_FILES) $(DOC_FILES)
	$(LEIN) codox

dist: $(JAR_FILE)

inspect-manifest: $(JAR_FILE)
	@unzip -p $(JAR_FILE) META-INF/MANIFEST.MF

run: $(JAR_FILE)
	java -jar $(JAR_FILE) $(DATA_DIR)

.PHONY: test all clean clean-all compile doc dist run
