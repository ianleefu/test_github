stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo('checkin_test_primo.xml')}
timeout(5){util_init.update_repo_by_gerrit()}
timeout(5){util_init.load_wiwigaga()}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(20){util_make.load_kmd_new('4c32s')}
try{
    timeout(30){util_test.primo_test("${test_scope}", "${cqm_repeat_flag}")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}