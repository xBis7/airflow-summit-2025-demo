# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
from __future__ import annotations

import logging

import pendulum

import requests

from opentelemetry import trace, context
from opentelemetry.propagate import extract

from airflow.sdk import chain, dag, task

from airflow.traces import otel_tracer
from airflow.traces.tracer import Trace

from pprint import pformat

logger = logging.getLogger("airflow.otel_test_dag")

airflow_otel_tracer = otel_tracer.get_otel_tracer(Trace)
tracer_provider = airflow_otel_tracer.get_otel_tracer_provider()

# We are importing from airflow, because we need to use the same tracer_provider
# that was used for creating the dag_run and task spans.
# In Airflow 3.2+, this won't be necessary anymore.
tracer = trace.get_tracer(__name__, tracer_provider=tracer_provider)

@task
def task1(ti):
    logger.info("Starting Task_1.")

    parent_ctx = extract(ti.context_carrier)
    # Make the task span current so that all sub-spans will be linked as children.
    token = context.attach(parent_ctx)

    try:
        with tracer.start_as_current_span("task1.sub_span1") as s1:
            s1.set_attribute("test_span", "true")
            # Some work.
            logger.info("From task1.sub_span1.")

            with tracer.start_as_current_span("task1.sub_span1.sub_span1") as s11:
                s11.set_attribute("test_span", "true")
                # Some work.
                logger.info("From task1.sub_span1.sub_span1.")

                # To use library instrumentation we have to hook up the tracer_provider.
                # The instrumentation library must already be installed.
                from opentelemetry.instrumentation.requests import RequestsInstrumentor
                RequestsInstrumentor().instrument(tracer_provider=tracer_provider)

                with tracer.start_as_current_span("get_repos_auto_instrumentation") as auto_instr_s:
                    # Some remote request.
                    response = requests.get("https://api.github.com/users/xBis7/repos")
                    logger.info("Response: %s", response.json())

                    auto_instr_s.set_attribute("test.repos_response", pformat(response.json()))

        with tracer.start_as_current_span(name="task1.sub_span2",) as s2:
            s2.set_attribute("test_span", "true")
            # Some work.
            logger.info("From task1.sub_span2.")
    finally:
        # Detach.
        context.detach(token)

    logger.info("Task_1 finished.")


@task
def task2(ti):
    logger.info("Starting Task_2.")

    context_carrier = ti.context_carrier
    logger.info("Injected headers: " + str(context_carrier))

    # Some remote request with the context injected into the headers.
    res = requests.get("http://java-tester:7777/api/work", headers=context_carrier, timeout=25)

    logger.info("\n\tStatus: " + str(res.status_code) + "\n\tBody: " + str(res.text))
    
    logger.info("Task_2 finished.")


@dag(
    schedule=None,
    start_date=pendulum.datetime(2025, 8, 30, tz="UTC"),
    catchup=False,
)
def otel_test_dag():
    chain(task1(), task2())

otel_test_dag()
