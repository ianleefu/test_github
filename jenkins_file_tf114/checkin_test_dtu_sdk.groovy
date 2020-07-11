stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo('checkin_test_dtu_sdk_tf114.xml')}
timeout(5){util_init.update_repo_by_gerrit()}
timeout(5){util_init.load_wiwigaga()}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(5){util_init.bazel_clean('dtu_sdk')}
timeout(30){util_make.build_install_sdk()}
timeout(120){util_make.build_install_tensorflow("false", "tf114", "true")}
try{
    timeout(20){util_make.load_kmd_new('4c32s')}
    timeout(20){util_test.resnet_train_test("64", "1", "10", "bf16", "CHNW", "tf114_daily_env_setup_4c32s.sh")}
    timeout(20){util_test.resnet_train_test("8", "1", "10", "fp32", "NHWC", "tf114_daily_env_setup_4c32s_googlenet_alexnet.sh", "alexnet")}
    timeout(20){util_test.resnet_train_test("2", "1", "10", "fp32", "NHWC", "tf114_daily_env_setup_4c32s_googlenet_alexnet.sh", "googlenet")}
    timeout(20){util_test.resnet_train_test("2", "1", "1", "fp32", "NHWC", "tf114_daily_env_setup_4c32s_vgg.sh", "vgg")}

    timeout(20){util_test.object_detection_train_test("16", "1", "4", "bf16", "CHNW", "tf114_daily_env_setup_4c32s_yolo.sh","yolo_v2","voc2007")}

    timeout(5){util_test.primo_sdk_test()}

    timeout(20){util_make.load_kmd_new('1c8s')}
    timeout(60){util_test.sdk_bazel_24_test("${sdk_test_plan}")}
    timeout(60){util_test.xla_test_tf114("CLUSTER_AS_DEVICE=false")}
    timeout(100){util_test.compiler_tests_test("checkin_tf114_compiler_tests_enable.txt", "ENABLE_SDK_STREAM_CACHE=false")}
    timeout(60){util_test.op_python_test("tf114_daily_op_env_setup_1c8s.sh", "operator_ci_test_non_bf16_list.txt")}
    timeout(30){util_test.op_python_test("tf114_daily_op_bf16_env_setup_1c8s.sh", "operator_ci_test_bf16_list.txt")}
    timeout(30){util_test.op_python_test("tf114_daily_op_rnn_env_setup_1c8s.sh", "operator_ci_test_rnn_list.txt")}
    load "jenkinsfile/integration_util/module_sv_model.groovy"
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}
