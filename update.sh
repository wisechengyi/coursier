#!/bin/bash

cp -R ../coursier/web/target/scala-2.11/*.js \
  ../coursier/web/target/scala-2.11/classes/*.html \
  ../coursier/web/target/scala-2.11/classes/css \
  ../coursier/web/target/scala-2.11/classes/js \
  .

cp index-dev.html index.html
