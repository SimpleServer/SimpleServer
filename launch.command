#!/bin/sh
cd $(dirname ${0})

java -Xmx256m -Xms32m -jar SimpleServer.jar

echo Press Enter to continue
read nothing
