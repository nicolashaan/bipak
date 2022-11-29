Pod::Spec.new do |spec|
    spec.name                     = 'bipak_sample_shared'
    spec.version                  = '1.0'
    spec.homepage                 = 'https://github.com/nicolashaan/BiPaK'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'BiPaK'
    spec.vendored_frameworks      = 'build/cocoapods/framework/SampleCommon.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target = '13'
    spec.osx.deployment_target = '11'
                
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':samples:bipak-sample-shared',
        'PRODUCT_MODULE_NAME' => 'SampleCommon',
    }
                
    spec.script_phases = [
        {
            :name => 'Build bipak_sample_shared',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end