@echo off
set JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.17_10
set PATH=%JAVA_HOME%\bin;%PATH%
cd C:\Program Files\sonarqube-10.7.0.96327\bin\windows-x86-64\bin
start StartSonar.bat
