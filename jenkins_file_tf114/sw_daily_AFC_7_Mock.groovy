stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("${env.manifest}")}

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
timeout(120){util_make.build_install_mock_tensorflow("tf114")}
try{
    timeout(150){util_test.mock_resnet_train_test("8", "1", "1", "bf16", "CHNW", "${test_env}")}
    timeout(10){util_log.mock_log_parse()}
    timeout(460){util_test.op_python_test("tf114_daily_op_env_setup_1c8s_mock.sh", "operator_ci_test_non_bf16_mock.txt")}
    timeout(10){util_log.mock_log_parse(true)}
    timeout(600){util_test.op_python_test("tf114_daily_op_bf16_env_setup_1c8s_mock.sh", "operator_ci_test_bf16_mock.txt")}
    timeout(10){util_log.mock_log_parse(true)}
    timeout(250){util_test.op_python_test("tf114_daily_op_rnn_env_setup_1c8s_mock.sh", "operator_ci_test_rnn_mock.txt")}
    timeout(10){util_log.mock_log_parse(true)}
    //7 hours for moba test
    // timeout(60){util_test.op_python_test("tf114_daily_op_env_setup_1c8s_mock.sh", "operator_ci_daily_test_non_bf16_mock.txt")}
    // timeout(10){util_log.mock_log_parse(true)}
    //7 hours bazelist call
    //timeout(500){util_test.op_mock_test()}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}