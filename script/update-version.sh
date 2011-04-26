#!/bin/sh

#Set commit version
git log -n1 --format=%h > "$1"

#Set build version
if [ -f script/BUILDVERSION ]; then
  BUILDNUMBER=$(cat script/BUILDVERSION)
  let BUILDNUMBER++
  echo $BUILDNUMBER > script/BUILDVERSION
else
  echo 0 > script/BUILDVERSION
fi
cp script/BUILDVERSION "$2"
