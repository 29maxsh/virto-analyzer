#!/bin/bash

mvn clean
mvn package
set currentDate=`date +"%m_%d_%Y"`
set dayOfMonth=`date +"%d"`
java -Djava.util.Arrays.useLegacyMergeSort=true -Djava.net.preferIPv4Stack=true -Djava.io.tmpdir=/tmp -Xmx2G -Dfile.encoding=utf-8 -jar target/VirtaMarketAnalyzer-jar-with-dependencies.jar
