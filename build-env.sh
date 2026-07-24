#!/bin/bash
export JAVA_HOME=$HOME/.jdks/jdk-21.0.11+10
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$PATH
exec "$@"
