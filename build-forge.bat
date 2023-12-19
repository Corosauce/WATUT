@echo off
del build.gradle
copy build_forge.gradle build.gradle
start gradlew build