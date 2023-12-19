@echo off
del build.gradle
copy build_fabric.gradle build.gradle
start gradlew build