#!/bin/sh
curl -Lo sbt https://raw.githubusercontent.com/sbt/sbt/v1.9.3/sbt
chmod +x ./sbt
npm install -g yarn
cd modules/frontend
yarn install
cd ../..
./sbt -D-Xmx2G frontend/fullLinkJS
cd modules/frontend
yarn build
