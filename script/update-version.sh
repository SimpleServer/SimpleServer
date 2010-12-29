#!/bin/sh

git log -n1 --format=%h > "$1"
