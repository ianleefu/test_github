stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo('Leo_develop.xml')}

timeout(10){util_init.checkout_by_commit("dtu_sdk", "${env.sdk_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_backend", "${env.backend_Commit}")}
timeout(10){util_init.checkout_by_commit("tensorflow", "${env.tensorflow_Commit}")}

timeout(10){util_init.fetch_change_patch("dtu_sdk", "${env.sdk_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_backend", "${env.backend_refID}")}
timeout(10){util_init.fetch_change_patch("tensorflow", "${env.tensorflow_refID}")}

timeout(5){util_init.main_commit_get()}

timeout(20){util_make.build_install_mock_sdk("${env.sdk_Commit}")}
timeout(120){util_make.build_install_mock_tensorflow()}
try{
    timeout(100){util_test.op_mock_test()}
    timeout(100){util_test.mock_resnet_train_test("32", "1", "true", "bf16", "CHNW")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}
