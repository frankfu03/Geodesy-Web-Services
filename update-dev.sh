#!/bin/bash
export AWS_PROFILE=geodesy
PATH=~/.local/bin:$PATH
ENV=D GEODESY_WEB_SERVICES_VERSION=1.0.0-SNAPSHOT make clean restack
