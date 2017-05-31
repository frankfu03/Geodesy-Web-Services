#!/usr/bin/env nix-shell
#!nix-shell ../shell.nix -i bash

set -e

# A local installation of maven prefers to run the global installation, if available.
sudo rm -f /etc/mavenrc

mvn -U --settings ./travis/maven-settings.xml compile

if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    mvn --settings ./travis/maven-settings.xml package -pl gws-system-test -DskipTests
    mvn --settings ./travis/maven-settings.xml deploy -pl '!gws-system-test'
    mvn --settings ./travis/maven-settings.xml site-deploy -DskipTests -pl gws-core
    aws configure set aws_access_key_id "${TRAVIS_AWS_ACCESS_KEY_ID}" --profile geodesy
    aws configure set aws_secret_access_key "${TRAVIS_AWS_SECRET_KEY_ID}" --profile geodesy
    aws configure set region ap-southeast-2 --profile geodesy
    aws configure set output json --profile geodesy
    case "${TRAVIS_BRANCH}" in
        "next")
            ./aws/codedeploy-WebServices/deploy.sh dev
        ;;
        "master")
            ./aws/codedeploy-WebServices/deploy.sh test
        ;;
    esac
else
    mvn --settings ./travis/maven-settings.xml verify -pl '!gws-system-test'
fi
