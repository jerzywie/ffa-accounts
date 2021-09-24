#!/bin/bash

if [ -z $1 ]; then
    echo "Please supply the destination directory."
    exit
fi

echo "'prod' build to " $1

rm -rf target/public

clojure -M:fig:min

cp -r resources/public/css $1/css/
cp -r resources/public/images $1/images/
cp resources/public/index.html $1
cp target/public/cljs-out/dev-main.js $1/cljs-out/
