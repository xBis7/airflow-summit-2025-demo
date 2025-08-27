#!/usr/bin/env bash

source "./lib.sh"

set -e

abs_path=$1

cd "$abs_path"/"$CURRENT_PROJECT"/demo

mvn clean install

cd "$abs_path"/"$CURRENT_PROJECT"/docker-env/compose

docker compose -f testers.yaml down
docker compose -f testers.yaml up -d
