#!/bin/bash
set -ev

for i in staging/*.html staging/*.js; do
  cp "$i" .
  git add "$(basename "$i")"
done

for i in staging/css/*; do
  cp "$i" css
  git add "css/$(basename "$i")"
done
