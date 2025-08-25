#!/usr/bin/env bash

set -e

abs_path=$1
prepare_env=$2


if [[ "$prepare_env" == "true" ]]; then
  # Cleanup old data.
  docker compose down --volumes --remove-orphans

  # Initialize the db.
  docker compose up airflow-init

  ./handle_env.sh "$abs_path"
fi

docker exec -it compose-airflow-worker-1 bash -c "airflow dags unpause otel_test_dag"
docker exec -it compose-airflow-worker-1 bash -c "airflow dags trigger otel_test_dag"
