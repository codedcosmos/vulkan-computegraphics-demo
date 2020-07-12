#!/bin/bash
cd "$(dirname "$0")"

echo Total Lines
echo $(find source/src -name '*.java' | xargs wc -l | tail -1) | grep -Eo '[0-9]{1,4}'