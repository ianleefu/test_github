stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo('checkin_test_tensorflow.xml')}
timeout(5){util_init.update_repo_by_gerrit()}
timeout(5){util_init.load_wiwigaga()}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(20){util_make.build_install_sdk()}
timeout(120){util_make.build_install_tensorflow("true", "tf18", "true")}
try{
    timeout(20){util_make.load_kmd('4c32s')}
    timeout(20){util_test.resnet_train_test("4", "4", "true", "bf16", "CHNW", "env_setup_4c32_checkin.sh")}
    timeout(20){util_test.resnet_train_test("16", "4", "true", "bf16", "CHNW", "env_setup_4c32_checkin.sh")}
    timeout(20){util_test.resnet_train_test("32", "4", "true", "bf16", "CHNW", "env_setup_4c32_checkin.sh")}
    timeout(20){util_make.load_kmd('1c8s')}
    timeout(60){util_test.xla_test('env_setup_1c8s_f32_checkin.sh')}
    timeout(30){util_test.op_python_test('env_setup_4c32_checkin.sh', 'checkin_dev_bf16_enable.txt')}
    timeout(30){util_test.op_python_test('env_setup_1c8s_f32_checkin.sh', 'checkin_dev_test_fp32_enable.txt')}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}