@echo off
setlocal EnableDelayedExpansion
REM Set commit version
git log -n1 --format=%%h > "%1"
