# airflow-summit-2025-demo

## Airflow docker setup

https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html

## Airflow webserver

| Type | Value |
| ----------- | ----------- |
| Url | http://0.0.0.0:8080 |
| Username | `airflow` |
| Password | `airflow` |

## Jaeger

| Type | Value |
| ----------- | ----------- |
| Url | http://localhost:16686/search |

## Run the demo

Note that `/path/to/projects` is the path to the parent of this repo.

```bash
cd docker-env/compose

# Cleanup the env.
docker compose down --volumes --remove-orphans

# Go to 'docker-env'.
cd ../

# This will build the java project and start the docker environment.
./setup_java_demo.sh /path/to/projects true

# or to just start the environment, run
# ./handle_env.sh /path/to/projects

-> Trigger the DagRun from the airflow UI or run
./test.sh

-> check the airflow UI
    |_ wait for the dag_run to finish. Spans will be exported afterwards.
-> check the Jaeger UI
    |_ Service: Airflow
    |_ Operation: otel_test_dag
```

