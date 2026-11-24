#!/bin/bash

# Parse command line parameters
usage() {
  echo "Usage: $0 --quarkus-cxf-dir <path> [options]"
  echo ""
  echo "Required parameters:"
  echo "  --quarkus-cxf-dir                   Path to the checked-out Quarkus CXF repository"
  echo ""
  echo "Optional parameters:"
  echo "  --maven-repo-local                  Path to a prepopulated local Maven repository"
  echo "                                        (if omitted, resolve the platform BOM through Maven)"
  echo "  --quarkus-platform-version          Platform version to test against"
  echo "                                        (required without --maven-repo-local; otherwise derived from its platform BOM directory)"
  echo "  --quarkus-platform-group-id         Platform group ID"
  echo "                                        (default: io.quarkus.platform)"
  echo "  --quarkus-platform-artifact-id      Platform artifact ID"
  echo "                                        (default: quarkus-bom)"
  echo "  --quarkus-cxf-platform-artifact-id  Quarkus CXF platform artifact ID"
  echo "                                        (default: quarkus-cxf-bom)"
  echo "  --quarkus-cxf-platform-group-id     Quarkus CXF platform group ID"
  echo "                                        (default: value of --quarkus-platform-group-id)"
  echo "  --quarkus-cxf-platform-version      Quarkus CXF platform version"
  echo "                                        (default: value of --quarkus-platform-version)"
  echo "  --quarkus-version                   Quarkus version"
  echo "                                        (required without --maven-repo-local; otherwise derived from its quarkus-core directory)"
  echo "  --quarkus-enforcer-rules-version     Quarkus enforcer rules version"
  echo "                                        (default: value of --quarkus-version)"
  echo "  --settings-xml                      Path to Maven settings.xml"
  echo "  --mvn-extra                         Extra arguments to pass to every mvn invocation"
  echo "  --dep-tree <file>                   Run dependency:tree after each mvn invocation and append output to <file>"
  echo "  --native                            Use verify instead of test phase and add native build args to integration tests"
  echo "                                        Adds: -Pnative -Dtest=!* -Dsurefire.failIfNoSpecifiedTests=false -Dquarkus.native.container-build=true"
  echo "                                        (configure NATIVE_PROFILE and NATIVE_ARGS variables in the script to customize)"
  echo "  --test-modules                      Comma-separated list of modules to test (adds -pl <modules> to integration tests)"
  echo "  --test-classes                      Comma-separated list of test classes to run (adds -Dtest=<classes> or -Dit.test=<classes> for native)"
  echo "  --prepare-only                      Only run preparation steps such as (BOM/POM setup, support modules), skip integration tests"
  echo "  --verbose                           Enable verbose output"
  echo "  --check-errors                      Check Maven output for error strings and use distinct exit codes"
  echo "                                        Exit 0 = success, 1 = test failures only (UNSTABLE), 2 = build errors (FAILURE)"
  echo "  --debug                             Enable command tracing (set -x)"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case $1 in
    --quarkus-cxf-dir)
      QUARKUS_CXF_DIR="$2"
      shift 2
      ;;
    --maven-repo-local)
      MAVEN_REPO_LOCAL="$2"
      shift 2
      ;;
    --quarkus-platform-version)
      QUARKUS_PLATFORM_VERSION="$2"
      shift 2
      ;;
    --quarkus-platform-group-id)
      QUARKUS_PLATFORM_GROUP_ID="$2"
      shift 2
      ;;
    --quarkus-platform-artifact-id)
      QUARKUS_PLATFORM_ARTIFACT_ID="$2"
      shift 2
      ;;
    --quarkus-version)
      QUARKUS_VERSION="$2"
      shift 2
      ;;
    --quarkus-enforcer-rules-version)
      QUARKUS_ENFORCER_RULES_VERSION="$2"
      shift 2
      ;;
    --settings-xml)
      SETTINGS_XML="$2"
      shift 2
      ;;
    --quarkus-cxf-platform-artifact-id)
      QUARKUS_CXF_PLATFORM_ARTIFACT_ID="$2"
      shift 2
      ;;
    --quarkus-cxf-platform-group-id)
      QUARKUS_CXF_PLATFORM_GROUP_ID="$2"
      shift 2
      ;;
    --quarkus-cxf-platform-version)
      QUARKUS_CXF_PLATFORM_VERSION="$2"
      shift 2
      ;;
    --mvn-extra)
      MVN_EXTRA="$2"
      shift 2
      ;;
    --dep-tree)
      DEP_TREE_FILE="$2"
      shift 2
      ;;
    --native)
      NATIVE=true
      shift
      ;;
    --test-modules)
      TEST_MODULES="$2"
      shift 2
      ;;
    --test-classes)
      TEST_CLASSES="$2"
      shift 2
      ;;
    --prepare-only)
      PREPARE_ONLY=true
      shift
      ;;
    --check-errors)
      CHECK_ERRORS=true
      shift
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --debug)
      set -x
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Unknown parameter: $1"
      usage
      ;;
  esac
done

# Validate required parameters
if [[ -z "$QUARKUS_CXF_DIR" ]]; then
  echo "Error: --quarkus-cxf-dir is required."
  usage
fi
if [[ -z "$MAVEN_REPO_LOCAL" && ( -z "$QUARKUS_PLATFORM_VERSION" || -z "$QUARKUS_VERSION" ) ]]; then
  echo "Error: --quarkus-platform-version and --quarkus-version are required without --maven-repo-local."
  usage
fi

QUARKUS_PLATFORM_GROUP_ID="${QUARKUS_PLATFORM_GROUP_ID:-io.quarkus.platform}"
QUARKUS_PLATFORM_ARTIFACT_ID="${QUARKUS_PLATFORM_ARTIFACT_ID:-quarkus-bom}"
QUARKUS_CXF_PLATFORM_ARTIFACT_ID="${QUARKUS_CXF_PLATFORM_ARTIFACT_ID:-quarkus-cxf-bom}"

# Only add -s flag if settings.xml is specified
if [[ -n "$SETTINGS_XML" ]]; then
  SETTINGS_XML_ARG="-s ${SETTINGS_XML}"
else
  SETTINGS_XML_ARG=""
fi

# Only add -Dmaven.repo.local if a local repository is specified
if [[ -n "$MAVEN_REPO_LOCAL" ]]; then
  MAVEN_REPO_LOCAL_ARG="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"
else
  MAVEN_REPO_LOCAL_ARG=""
fi

NATIVE_PROFILE="-Pnative"
NATIVE_ARGS="-Dtest=!* -Dsurefire.failIfNoSpecifiedTests=false  -Dquarkus.native.container-build=true"

if [[ "$NATIVE" == "true" ]]; then
  INTEGRATION_TEST_PHASE="verify"
  NATIVE_MVN_ARGS="$NATIVE_PROFILE $NATIVE_ARGS"
else
  INTEGRATION_TEST_PHASE="test"
  NATIVE_MVN_ARGS=""
fi

if [[ -n "$TEST_MODULES" ]]; then
  TEST_MODULES_ARG="-pl $TEST_MODULES"
else
  TEST_MODULES_ARG=""
fi

if [[ -n "$TEST_CLASSES" ]]; then
  if [[ "$NATIVE" == "true" ]]; then
    TEST_CLASSES_ARG="-Dit.test=$TEST_CLASSES"
  else
    TEST_CLASSES_ARG="-Dtest=$TEST_CLASSES"
  fi
else
  TEST_CLASSES_ARG=""
fi

if [[ -n "$DEP_TREE_FILE" ]]; then
  DEP_TREE_ARG="dependency:tree -DoutputFile=$DEP_TREE_FILE -DappendOutput=true"
else
  DEP_TREE_ARG=""
fi

log() {
  if [[ "$VERBOSE" == "true" ]]; then
    echo "$@"
  fi
}

cd "$QUARKUS_CXF_DIR" || { echo "Error: cannot cd to $QUARKUS_CXF_DIR"; exit 1; }

log "QUARKUS_CXF_DIR = $QUARKUS_CXF_DIR"
log "MAVEN_REPO_LOCAL = $MAVEN_REPO_LOCAL"
log "SETTINGS_XML_ARG = $SETTINGS_XML_ARG"
log "MVN_EXTRA = $MVN_EXTRA"

if [[ -z "$QUARKUS_PLATFORM_VERSION" ]]; then
  QUARKUS_PLATFORM_VERSION="$(ls "${MAVEN_REPO_LOCAL}/${QUARKUS_PLATFORM_GROUP_ID//.//}/${QUARKUS_PLATFORM_ARTIFACT_ID}/")" || exit 2
  log "QUARKUS_PLATFORM_VERSION derived from MAVEN_REPO_LOCAL"
fi
log "QUARKUS_PLATFORM_GROUP_ID = $QUARKUS_PLATFORM_GROUP_ID"
log "QUARKUS_PLATFORM_ARTIFACT_ID = $QUARKUS_PLATFORM_ARTIFACT_ID"
log "QUARKUS_PLATFORM_VERSION = $QUARKUS_PLATFORM_VERSION"

QUARKUS_CXF_PLATFORM_GROUP_ID="${QUARKUS_CXF_PLATFORM_GROUP_ID:-$QUARKUS_PLATFORM_GROUP_ID}"
log "QUARKUS_CXF_PLATFORM_GROUP_ID = $QUARKUS_CXF_PLATFORM_GROUP_ID"

log "QUARKUS_CXF_PLATFORM_ARTIFACT_ID = $QUARKUS_CXF_PLATFORM_ARTIFACT_ID"

QUARKUS_CXF_PLATFORM_VERSION="${QUARKUS_CXF_PLATFORM_VERSION:-$QUARKUS_PLATFORM_VERSION}"
log "QUARKUS_CXF_PLATFORM_VERSION = $QUARKUS_CXF_PLATFORM_VERSION"

if [[ -z "$QUARKUS_VERSION" ]]; then
  QUARKUS_VERSION="$(ls "${MAVEN_REPO_LOCAL}/io/quarkus/quarkus-core/")" || exit 2
  log "QUARKUS_VERSION derived from MAVEN_REPO_LOCAL"
fi
log "QUARKUS_VERSION = $QUARKUS_VERSION"

if [[ -z "$QUARKUS_PLATFORM_VERSION" || -z "$QUARKUS_VERSION" ]]; then
  echo "Error: could not determine platform and Quarkus versions from the local repository."
  exit 2
fi
QUARKUS_ENFORCER_RULES_VERSION="${QUARKUS_ENFORCER_RULES_VERSION:-$QUARKUS_VERSION}"
log "QUARKUS_ENFORCER_RULES_VERSION = $QUARKUS_ENFORCER_RULES_VERSION"

if [[ -n "$MAVEN_REPO_LOCAL" ]]; then
  PLATFORM_BOM="${MAVEN_REPO_LOCAL}/${QUARKUS_CXF_PLATFORM_GROUP_ID//.//}/${QUARKUS_CXF_PLATFORM_ARTIFACT_ID}/${QUARKUS_CXF_PLATFORM_VERSION}/${QUARKUS_CXF_PLATFORM_ARTIFACT_ID}-${QUARKUS_CXF_PLATFORM_VERSION}.pom"
else
  # Resolve the BOM independently of the Quarkus CXF checkout's POMs.
  BOM_RESOLUTION_DIR="$(mktemp -d)" || exit 2
  trap 'rm -rf "$BOM_RESOLUTION_DIR"' EXIT
  cat > "$BOM_RESOLUTION_DIR/pom.xml" <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.quarkiverse.cxf</groupId>
  <artifactId>platform-bom-resolution</artifactId>
  <version>1.0</version>
</project>
EOF
  mvn -V -B -ntp $SETTINGS_XML_ARG $MVN_EXTRA \
    -f "$BOM_RESOLUTION_DIR/pom.xml" \
    org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy \
    -Dartifact="${QUARKUS_CXF_PLATFORM_GROUP_ID}:${QUARKUS_CXF_PLATFORM_ARTIFACT_ID}:${QUARKUS_CXF_PLATFORM_VERSION}:pom" \
    -DoutputDirectory="$BOM_RESOLUTION_DIR" -Dmdep.stripVersion=true || exit 2
  PLATFORM_BOM="$BOM_RESOLUTION_DIR/${QUARKUS_CXF_PLATFORM_ARTIFACT_ID}.pom"
fi
if [[ ! -f "$PLATFORM_BOM" ]]; then
  echo "Error: platform BOM not found: $PLATFORM_BOM"
  exit 2
fi

# Override poms/bom/pom.xml with the Platform BOM, so that we have the right versions everywhere
log "Copying Platform BOM to bom/pom.xml"
log "  src: $PLATFORM_BOM"
log "  dst: bom/pom.xml"
cp "$PLATFORM_BOM" bom/pom.xml || exit 2

# Override bom/pom.xml with the Platform BOM,
# so that we have the right versions everywhere
SNAPSHOT_VERSION="$(xmllint --xpath "/*[local-name()=\"project\"]/*[local-name()=\"version\"]/text()" pom.xml)"
log "SNAPSHOT_VERSION = $SNAPSHOT_VERSION"

log "Copying Platform BOM to bom/pom.xml (second pass)"
cp "$PLATFORM_BOM" bom/pom.xml || exit 2
if [[ -z "$MAVEN_REPO_LOCAL" ]]; then
  rm -rf "$BOM_RESOLUTION_DIR"
  trap - EXIT
fi

log "Setting groupId to io.quarkiverse.cxf in bom/pom.xml"
printf 'cd /*[local-name()=\"project\"]/*[local-name()=\"groupId\"]\nset %s\nsave' 'io.quarkiverse.cxf' | xmllint --shell bom/pom.xml

log "Setting version to $SNAPSHOT_VERSION in bom/pom.xml"
printf 'cd /*[local-name()=\"project\"]/*[local-name()=\"version\"]\nset %s\nsave' $SNAPSHOT_VERSION | xmllint --shell bom/pom.xml

log "Removing quarkus.native.compression from application.properties"
FILE="test-util-parent/test-ws-rm-server-native/src/main/resources/application.properties"
sed '/^quarkus.native.compression/d' "$FILE" > "${FILE}.tmp" && mv "${FILE}.tmp" "$FILE"

# Build some ancillary modules we need for testing
supportModules=("-N" "-f bom/pom.xml" "-f bom-test/pom.xml" "-f test-util-parent/pom.xml $NATIVE_MVN_ARGS")
for module in "${supportModules[@]}"; do
    log "+ mvn -V clean install -DskipTests -B -ntp $MAVEN_REPO_LOCAL_ARG $SETTINGS_XML_ARG -Dformatter.skip -Dimpsort.skip -Denforcer.skip -DnoVirtualDependencies -Dquarkus.platform.group-id=$QUARKUS_PLATFORM_GROUP_ID -Dquarkus.platform.artifact-id=$QUARKUS_PLATFORM_ARTIFACT_ID -Dquarkus.platform.version=$QUARKUS_PLATFORM_VERSION -Dquarkus-cxf.platform.group-id=$QUARKUS_CXF_PLATFORM_GROUP_ID -Dquarkus-cxf.platform.artifact-id=$QUARKUS_CXF_PLATFORM_ARTIFACT_ID -Dquarkus-cxf.platform.version=$QUARKUS_CXF_PLATFORM_VERSION -Dquarkus.version=$QUARKUS_VERSION -Dquarkus-enforcer-rules.version=$QUARKUS_ENFORCER_RULES_VERSION $MVN_EXTRA $module"
    mvn -V clean install -DskipTests -B -ntp \
      $MAVEN_REPO_LOCAL_ARG \
      $SETTINGS_XML_ARG \
      -Dformatter.skip -Dimpsort.skip -Denforcer.skip \
      -DnoVirtualDependencies \
      -Dquarkus.platform.group-id=$QUARKUS_PLATFORM_GROUP_ID \
      -Dquarkus.platform.artifact-id=$QUARKUS_PLATFORM_ARTIFACT_ID \
      -Dquarkus.platform.version=$QUARKUS_PLATFORM_VERSION \
      -Dquarkus-cxf.platform.group-id=$QUARKUS_CXF_PLATFORM_GROUP_ID \
      -Dquarkus-cxf.platform.artifact-id=$QUARKUS_CXF_PLATFORM_ARTIFACT_ID \
      -Dquarkus-cxf.platform.version=$QUARKUS_CXF_PLATFORM_VERSION \
      -Dquarkus.version=$QUARKUS_VERSION \
      -Dquarkus-enforcer-rules.version=$QUARKUS_ENFORCER_RULES_VERSION \
      $MVN_EXTRA \
      $module
done

# Exit if prepare-only mode
if [[ "$PREPARE_ONLY" == "true" ]]; then
  log "=== Preparation completed successfully ==="
  exit 0
fi

# Run the integration tests
MVN_CMD="$(echo "-V clean $INTEGRATION_TEST_PHASE $DEP_TREE_ARG -fae -B -ntp \
  $MAVEN_REPO_LOCAL_ARG \
  $SETTINGS_XML_ARG \
  -Dformatter.skip -Dimpsort.skip -Denforcer.skip -DbuildMetaData.skip \
  -DnoVirtualDependencies \
  -Dquarkus.platform.group-id=$QUARKUS_PLATFORM_GROUP_ID \
  -Dquarkus.platform.artifact-id=$QUARKUS_PLATFORM_ARTIFACT_ID \
  -Dquarkus.platform.version=$QUARKUS_PLATFORM_VERSION \
  -Dquarkus-cxf.platform.group-id=$QUARKUS_CXF_PLATFORM_GROUP_ID \
  -Dquarkus-cxf.platform.artifact-id=$QUARKUS_CXF_PLATFORM_ARTIFACT_ID \
  -Dquarkus-cxf.platform.version=$QUARKUS_CXF_PLATFORM_VERSION \
  -Dquarkus.version=$QUARKUS_VERSION \
  -Dquarkus-enforcer-rules.version=$QUARKUS_ENFORCER_RULES_VERSION \
  $NATIVE_MVN_ARGS \
  $MVN_EXTRA \
  $TEST_MODULES_ARG \
  $TEST_CLASSES_ARG \
  -f integration-tests/pom.xml" | tr -s ' ')"

# Run the integration tests
log "=== Running integration tests ==="
log "+ mvn $MVN_CMD"

if [[ "$CHECK_ERRORS" == "true" ]]; then
  # Create temporary file for Maven output
  MVN_OUTPUT_FILE=$(mktemp)
  trap "rm -f $MVN_OUTPUT_FILE" EXIT

  # Run Maven and capture output
  mvn $MVN_CMD 2>&1 | tee "$MVN_OUTPUT_FILE"
  MVN_EXIT_CODE=${PIPESTATUS[0]}

  log "=== Integration tests finished with Maven exit code: $MVN_EXIT_CODE ==="

  # Check for error strings in Maven output
  ERROR_STRINGS=("Compilation failure" "Could not resolve dependencies" "problems were encountered")

  for ERROR_STR in "${ERROR_STRINGS[@]}"; do
    if grep -q "$ERROR_STR" "$MVN_OUTPUT_FILE"; then
      log "ERROR: Maven output contains string '$ERROR_STR', failing build with exit code 2"
      exit 2
    fi
  done

  # No error strings found, but Maven might have failed with test failures
  if [[ $MVN_EXIT_CODE -ne 0 ]]; then
    log "Maven tests failed but no error strings found, exiting with code 1"
    # Exit code 1 = test failures only (Jenkins will mark as UNSTABLE - orange)
    exit 1
  fi

  log "All tests passed successfully"
  # Exit code 0 = all OK
  exit 0
else
  # Run Maven normally without error checking
  mvn $MVN_CMD
  EXIT_CODE=$?
  log "=== Integration tests finished with exit code: $EXIT_CODE ==="
  exit $EXIT_CODE
fi
