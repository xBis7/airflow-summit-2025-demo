#!/usr/bin/env bash

set -e

print_ports=${1:-"false"}

if [[ "$print_ports" != "false" ]]; then
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
  docker ps --format "table {{.Names}}\t{{.Status}}"
fi

