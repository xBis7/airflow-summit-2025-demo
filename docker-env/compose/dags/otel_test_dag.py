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

from opentelemetry import trace

from airflow.sdk import chain, dag, task
from airflow.traces import otel_tracer
from airflow.traces.tracer import Trace

from pprint import pformat

logger = logging.getLogger("airflow.otel_test_dag")

@task
def task1(ti):
    logger.info("Starting Task_1.")

    context_carrier = ti.context_carrier

    # The difference between the task tracer and the one used for the dag, is the type of the SpanProcessor.
    # The general tracer uses a BatchSpanProcessor, while the task tracer uses a SimpleSpanProcessor.
    # If the `simple` isn't used, the dag might finish and the executing process will be cleaned up
    # before there is a chance to export the spans, which means that they will be lost.
    otel_task_tracer = otel_tracer.get_otel_tracer_for_task(Trace)
    tracer_provider = otel_task_tracer.get_otel_tracer_provider()

    if context_carrier is not None:
        logger.info("Found ti.context_carrier: %s.", context_carrier)
        logger.info("Extracting the span context from the context_carrier.")
        parent_context = otel_task_tracer.extract(context_carrier)
        with otel_task_tracer.start_child_span(
            span_name="part1_with_parent_ctx",
            parent_context=parent_context,
            component="dag",
        ) as p1_with_ctx_s:
            p1_with_ctx_s.set_attribute("using_parent_ctx", "true")
            # Some work.
            logger.info("From part1_with_parent_ctx.")

            with otel_task_tracer.start_child_span("sub_span_without_setting_parent") as sub1_s:
                sub1_s.set_attribute("get_parent_ctx_from_curr", "true")
                # Some work.
                logger.info("From sub_span_without_setting_parent.")

                otel_tracer_provider = otel_task_tracer.get_otel_tracer_provider()

                # To use library instrumentation we have to hook up the tracer_provider.
                # The instrumentation library must already be installed.
                from opentelemetry.instrumentation.requests import RequestsInstrumentor
                RequestsInstrumentor().instrument(tracer_provider=otel_tracer_provider)

                # If we don't set the parent context, it will get it like so
                # trace.get_current_span().get_span_context()
                # and then start_as_current_span()
                # tracer.start_as_current_span(name="")
                with otel_task_tracer.start_child_span(span_name="get_repos_auto_instrumentation") as auto_instr_s:
                    # Some remote request.
                    response = requests.get("https://api.github.com/users/xBis7/repos")
                    logger.info("Response: %s", response.json())

                    auto_instr_s.set_attribute("test.repos_response", pformat(response.json()))

                tracer = trace.get_tracer("trace_test.tracer", tracer_provider=tracer_provider)
                with tracer.start_as_current_span(name="sub_span_start_as_current") as sub_curr_s:
                    sub_curr_s.set_attribute("start_as_current", "true")
                    # Some work.
                    logger.info("From sub_span_start_as_current.")

        with otel_task_tracer.start_child_span(
            span_name="part2_with_parent_ctx",
            parent_context=parent_context,
            component="dag",
        ) as p2_with_ctx_s:
            p1_with_ctx_s.set_attribute("using_parent_ctx", "true")
            # Some work.
            logger.info("From part2_with_parent_ctx.")

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
