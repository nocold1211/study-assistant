#!/bin/bash

# Java 다운로드
curl -L https://aka.ms/download-jdk/microsoft-jdk-17-linux-x64.tar.gz -o java.tar.gz

# java-home 디렉토리 생성
mkdir -p java-home

# 압축 해제
tar -xzf java.tar.gz -C java-home --strip-components=1

# Java 환경 변수 설정
export JAVA_HOME=$PWD/java-home
export PATH=$JAVA_HOME/bin:$PATH

echo "Check java-home"
ls -al $JAVA_HOME/
echo "Check java-home/bin"
ls -al $JAVA_HOME/bin/

# Java 버전 확인 (절대 경로 사용)
$JAVA_HOME/bin/java -version

# Java 버전 확인
java -version

# 프로젝트 빌드
curl -Lo sbt https://raw.githubusercontent.com/sbt/sbt/v1.9.3/sbt
chmod +x ./sbt
npm install -g yarn
cd modules/frontend
yarn install
cd ../..
./sbt -D-Xmx2G frontend/fullLinkJS
cd modules/frontend
yarn build
