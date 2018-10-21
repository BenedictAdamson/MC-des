# Dockerfile for the use in the Jenkinsfile for the MC-des project,
# to set up the build environment for Jenkins to use.

# © Copyright Benedict Adamson 2018.
# 
# This file is part of MC-des.
#
# MC-des is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# MC-des is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
#

FROM ubuntu:18.04
RUN apt-get -y update && apt-get install \
   maven \
   openjdk-11-jdk-headless