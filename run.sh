#!/usr/bin/env bash
mvn compile exec:java -Dexec.args="$*"
