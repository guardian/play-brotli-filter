#!/usr/bin/env bash

cd ../common/lib/

version='1.16.0'

for platform in 'native-osx-aarch64' \
                'native-osx-x86_64' \
                'native-windows-x86_64' \
                'native-windows-aarch64' \
                'native-linux-x86_64' \
                'native-linux-aarch64' \
                'native-linux-armv7' \
                'native-linux-s390x' \
                'native-linux-riscv64' \
		'native-linux-ppc64le'	
do
   curl  -OJ -# "https://repo1.maven.org/maven2/com/aayushatharva/brotli4j/${platform}/${version}/${platform}-${version}.jar"
done
