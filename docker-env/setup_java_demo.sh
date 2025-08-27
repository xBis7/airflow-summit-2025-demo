#!/usr/bin/env bash

source "./lib.sh"

set -e

abs_path=$1
start_env=$2


cd "$abs_path"/"$CURRENT_PROJECT"/docker-env
./handle_env.sh "$abs_path" "stop"

cd "$abs_path"/"$CURRENT_PROJECT"/demo

mvn clean install

if [[ "$start_env" == "true" ]]; then
  cd "$abs_path"/"$CURRENT_PROJECT"/docker-env
  ./handle_env.sh "$abs_path"
fi

