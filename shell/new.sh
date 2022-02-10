#!/usr/bin/env bash

#MDOC_HOME="~/Library/Containers/com.coderforart.MWeb3/Data/Library/Application Support/MWebLibrary"

if [ -z "${MDOC_HOME}" ]; then
  echo "Please set MDOC_HOME first!"
  exit 1
fi

MDOC_HOME=$(eval "echo ${MDOC_HOME}")

newFilename=$(date +%s)
newFilename=${newFilename}0000.md

touch "$MDOC_HOME/docs/$newFilename"
echo "{query}" >"$MDOC_HOME/docs/$newFilename"

open -a /Applications/MWeb\ Pro.app "${MDOC_HOME}/docs/$newFilename"