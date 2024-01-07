#!/usr/bin/env bash

cd ../common/lib/

version='1.12.0' 

curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/native-osx-aarch64/${version}/native-osx-aarch64-${version}.jar"
curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/native-osx-x86_64/${version}/native-osx-x86_64-${version}.jar"
curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/native-windows-x86_64/${version}/native-windows-x86_64-${version}.jar"
curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/native-linux-x86_64/${version}/native-linux-x86_64-${version}.jar"
curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/native-linux-aarch64/${version}/native-linux-aarch64-${version}.jar"
curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/native-linux-armv7/${version}/native-linux-armv7-${version}.jar"
