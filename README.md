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

## Execute the demo

```bash

cd docker-env

./handle_env.sh /path/to/projects
```

## Useful commands

```bash
# Cleanup the env.
docker compose down --volumes --remove-orphans

# Initialize the db.
docker compose up airflow-init


```

