#!/usr/bin/env bash

clojure -Spom
clojure -A:jar
mvn deploy:deploy-file -Dfile=lib.jar -DpomFile=pom.xml -DrepositoryId=clojars -Durl=https://clojars.org/repo/