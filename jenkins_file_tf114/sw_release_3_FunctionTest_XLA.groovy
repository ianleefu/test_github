stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("${env.manifest}")}
timeout(5){util_init.load_wiwigaga()}
timeout(10){util_init.checkout_by_commit("tensorflow", "${env.tensorflow_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_backend", "${env.backend_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_sdk", "${env.sdk_Commit}")}

timeout(10){util_init.fetch_change_patch("tensorflow", "${env.tensorflow_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_backend", "${env.backend_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_sdk", "${env.sdk_refID}")}

timeout(5){util_init.main_commit_get()}

timeout(10){util_make.copy_install_kmd_deb('$build_job_name', '$build_ID')}
timeout(10){util_init.bazel_clean("dtu_sdk")}
timeout(30){util_make.build_install_sdk()}
timeout(120){util_make.build_install_tensorflow("true", "tf114", "true")}
try{
    timeout(120){util_test.xla_test_tf114("CLUSTER_AS_DEVICE=false")}
    timeout(100){util_test.compiler_tests_test("checkin_tf114_compiler_tests_enable.txt", "ENABLE_SDK_STREAM_CACHE=false")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_make.uninstall_kmd_deb()}
    timeout(10){util_log.get_log()}
}