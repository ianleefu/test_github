stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("${env.manifest}")}
timeout(5){util_init.load_wiwigaga()}
timeout(10){util_init.checkout_by_commit("dtu_sdk", "${env.sdk_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}

timeout(10){util_init.fetch_change_patch("dtu_sdk", "${env.sdk_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}

timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(20){util_make.load_kmd_new('1c8s')}
timeout(10){util_init.bazel_clean("dtu_sdk")}
try{
    timeout(60){util_test.sdk_bazel_24_test("${sdk_test_plan}")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}