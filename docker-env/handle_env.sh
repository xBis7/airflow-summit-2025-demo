#!/usr/bin/env bash

source "./lib.sh"

set -e

abs_path=$1
action=${2:-"start"}

cd "$abs_path"/"$CURRENT_PROJECT"/docker-env/compose

# The env variable is enough for docker compose to use the files.
export COMPOSE_FILE="docker-compose.yaml:observability.yaml:testers.yaml"
export CURR_UID=$(id -u)
export CURR_GID=$(id -g)

if [[ "$action" == "start" ]]; then
  docker compose up -d
elif [[ "$action" == "stop" ]]; then
  docker compose down
elif [[ "$action" == "restart" ]]; then
  docker compose down
  docker compose up -d
else
  echo "Unknown action -- '$action'. Try one of the following: 'start', 'stop', 'restart'."
fi

