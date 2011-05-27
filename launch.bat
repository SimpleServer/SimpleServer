@echo off
path=%path%;%ProgramFiles(x86)%\java\jre6\bin;%ProgramFiles%\java\jre6\bin
java -Xmx256m -Xms32m -jar SimpleServer.jar
pause