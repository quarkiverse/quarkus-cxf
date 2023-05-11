#!/bin/bash
#
# A script for editting .github/project.yml file, committing the changes and pushing the brach
# to git@github.com:quarkiverse/quarkus-cxf.git
# The invocation would normally look like the following:
#
# ./release.sh 1.2.3 1.2.4-SNAPSHOT
#
# After the script pushes to git@github.com:quarkiverse/quarkus-cxf.git
# you need to create a pull request against the appropriate release branch, such as main or 1.5
# You do not need to wait for the CI, just merge the PR straight away.
# That should trigger the release job defined in .github/workflows/release.yml.
# You can find the job under https://github.com/quarkiverse/quarkus-cxf/actions
# and watch whether it runs smoothly.

set -x
set -e

releaseVersion="$1"
nextVersion="$2"

topicBranch=trigger-release-$releaseVersion
git checkout -b $topicBranch

sed -i -e 's|  current-version:.*|  current-version: '$releaseVersion'|' .github/project.yml
sed -i -e 's|  next-version:.*|  next-version: '$nextVersion'|' .github/project.yml

# current-major-minor-version is only used in maintenance branches like 1.5 to set the versio in antora.yml
releaseVersionMajorMinor=$(echo $releaseVersion | sed 's|.[0-9][0-9]*$||')
sed -i -e 's|  current-major-minor-version:.*|  current-major-minor-version: '$releaseVersionMajorMinor'|' .github/project.yml

git add -A
git commit -m "Trigger release $releaseVersion"
git push git@github.com:quarkiverse/quarkus-cxf.git $topicBranch
