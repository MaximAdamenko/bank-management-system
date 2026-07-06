#!/usr/bin/env bash
# Compile and run the Bank Management System, then clean up .class files.
set -e
cd "$(dirname "$0")"

cleanup() {
    find . -name "*.class" -delete
}
trap cleanup EXIT

javac -cp .:src $(find . -name "*.java")
java -cp .:src:postgresql-42.7.7.jar Main
