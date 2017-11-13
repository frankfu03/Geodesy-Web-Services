#!/usr/bin/env bash

set -e

java_link="$(which java)"
java=$(readlink -f "${java_link}")
java_bin=$(dirname "${java}")
JAVA_HOME=$(dirname "${java_bin}")
export JAVA_HOME

catalina_link="$(which catalina.sh)"
catalina=$(readlink -f "${catalina_link}")
tomcat_bin=$(dirname "${catalina}")
TOMCAT_HOME=$(dirname "${tomcat_bin}")
export TOMCAT_HOME

# Wait until Tomcat startup has finished
function wait_for_tomcat {
    until [ "`curl --silent --connect-timeout 1 -I http://localhost:8080 | grep 'Coyote'`" != "" ];
    do
        sleep 10
    done
}

set +e
