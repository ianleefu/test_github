stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo('checkin_test_benchmark_tf114.xml')}
timeout(5){util_init.update_repo_by_gerrit()}
timeout(5){util_init.load_wiwigaga()}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(20){util_make.copy_install_sdk_deb("$build_job_name", "$build_ID")}
timeout(120){util_make.copy_install_tf_whl("$build_job_name", "$build_ID", "tf114")}
timeout(20){util_make.load_kmd_new('1c8s')}
try{
    timeout(20){util_test.sv_model_test("sv")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
try{
    timeout(80){util_test.apd_test("1c8s")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
try{
    timeout(20){util_make.load_kmd_new('4c32s')}
    timeout(40){util_test.sv_model_test("sv_4c")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}
