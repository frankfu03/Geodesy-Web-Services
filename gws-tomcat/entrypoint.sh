#!/usr/bin/env bash

set -e

. ./env.sh

mkdir -p "${TOMCAT_HOME}"/webapps/
if [ -d /webapps ]; then
    rsync --ignore-missing-args -ur /webapps/ "${TOMCAT_HOME}"/webapps/
fi

set +e

exec "$@"
