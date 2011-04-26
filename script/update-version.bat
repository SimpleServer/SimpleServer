@echo off
setlocal EnableDelayedExpansion

REM Set commit version
git log -n1 --format=%%h > "%1"

REM Set build version
if exist script\BUILDVERSION (
  set /p in=<script\BUILDVERSION
  set /a new=!in!+1
  echo !new! > script\BUILDVERSION
) else (
  echo 0 > script\BUILDVERSION
)
copy /Y script\BUILDVERSION "%2"
