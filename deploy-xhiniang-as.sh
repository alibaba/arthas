#!/bin/bash

scp -r boot/target/arthas-boot-jar-with-dependencies.jar xhinliang.com:/www/https-statics/files/arthas-boot.jar
scp -r packaging/target/*.zip xhinliang.com:/www/https-statics/files/arthas.zip

