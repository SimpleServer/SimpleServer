#!/bin/sh

#Set commit version
git log -n1 --format=%h > "$1"
