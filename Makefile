VERSION := 0.9
RELEASE := 0.5

UNAME := $(shell uname -o)

ifeq ($(UNAME),Cygwin)
CLASSPATH = '$(shell cygpath -wsp .:$(JARS))'
PWD := $(shell cygpath -d $(shell pwd))\\
LAUNCH4J := launch4jc
INSTALL_DIR := '$(shell cygpath -u "C:\Program Files\ECUxPlot")'
else
CLASSPATH = .:$(JARS)
PWD := $(shell pwd)/
LAUNCH4J := /usr/local/launch4j/launch4j
INSTALL_DIR := /usr/local/ecuxplot
endif
SCP := scp

MP_SOURCES= HexValue.java Map.java Parser.java Parse.java \
	    ParserException.java Project.java MapData.java

LF_SOURCES= Dataset.java CSVFileFilter.java CSVRow.java

UT_SOURCES= ExitListener.java WindowUtilities.java Cursors.java \
	    WaitCursor.java MMapFile.java \
	    MenuListener.java SubActionListener.java \
	    GenericFileFilter.java Unsigned.java DoubleArray.java \
	    MovingAverageSmoothing.java

VM_SOURCES= LinearSmoothing.java SavitzkyGolaySmoothing.java

EX_SOURCES= ECUxPlot.java ECUxChartFactory.java ECUxDataset.java ECUxChartPanel.java AxisMenu.java FileMenu.java OptionsMenu.java PreferencesEditor.java Env.java Filter.java FilterEditor.java Constants.java ConstantsEditor.java PID.java PIDEditor.java Fueling.java FuelingEditor.java Units.java

LF_CLASSES=$(LF_SOURCES:%.java=org/nyet/logfile/%.class)
UT_CLASSES=$(UT_SOURCES:%.java=org/nyet/util/%.class)
VM_CLASSES=$(VM_SOURCES:%.java=vec_math/%.class)
MP_CLASSES=$(MP_SOURCES:%.java=org/nyet/mappack/%.class)

EX_CLASSES=$(EX_SOURCES:%.java=org/nyet/ecuxplot/%.class)

TARGETS=mapdump.class $(EX_CLASSES)
REFERENCE=data/4Z7907551R.kp

JARFILES:=jcommon-1.0.12.jar jfreechart-1.0.9.jar opencsv-1.8.jar applib.jar AppleJavaExtensions.jar
JARS:=jcommon-1.0.12.jar:jfreechart-1.0.9.jar:opencsv-1.8.jar:applib.jar:AppleJavaExtensions.jar

JFLAGS=-classpath $(CLASSPATH) -Xlint:deprecation -Xlint:unchecked -target 1.5
TARGET=ECUxPlot-$(VERSION)r$(RELEASE)

ZIPS=$(TARGET).zip $(TARGET).MacOS.zip
all: $(TARGETS) .classpath version.txt
jar: $(TARGET).jar
zip: $(ZIPS)
exe: ECUxPlot.exe
scp: $(ZIPS)
	$(SCP) $^ nyet.org:public_html/cars/files/

clean:
	rm -f ECUxPlot.exe ECUxPlot*.zip ECUxPlot.jar ECUxPlot-$(VERSION)r$(RELEASE).jar ECUxPlot.xml version.txt .classpath
	rm -f *.class
	rm -rf ECUxPlot.app
	find org -name \*.class | xargs rm
	find vec_math -name \*.class | xargs rm

%.csv: %.kp mapdump
	./mapdump -r $(REFERENCE) $< > $@

.classpath: Makefile
	echo "export CLASSPATH=$(CLASSPATH)" > .classpath

version.txt: Makefile
	@rm -f version.txt
	echo $(VERSION)r$(RELEASE) > $@

mapdump.class: mapdump.java $(MP_CLASSES) $(UT_CLASSES)
$(MP_CLASSES): $(LF_CLASSES) $(UT_CLASSES)
$(EX_CLASSES): $(LF_CLASSES) $(UT_CLASSES) $(VM_CLASSES)

include Windows.mk
include MacOS.mk

INSTALL_FILES = ECUxPlot.exe ECUxPlot-$(VERSION)r$(RELEASE).jar ECUxPlot.sh \
		$(subst :, ,$(JARS)) version.txt

ECUxPlot-$(VERSION)r$(RELEASE).zip: $(INSTALL_FILES)
	@rm -f $@
	zip $@ $(INSTALL_FILES)

install: $(INSTALL_FILES)
	mkdir -p $(INSTALL_DIR)
	cp -avp $(INSTALL_FILES) $(INSTALL_DIR)/

%.class: %.java
	javac $(JFLAGS) $<
