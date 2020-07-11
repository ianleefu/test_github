stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("$manifest")}
timeout(5){util_init.load_wiwigaga()}
timeout(10){util_init.checkout_by_commit("benchmark", "${env.benchmark_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
currentBuild.description+="<br>build_job_name: ${build_job_name}<br>build_ID: ${build_ID};"

timeout(20){util_make.copy_install_sdk_deb("$build_job_name", "$build_ID")}
timeout(120){util_make.copy_install_tf_whl("$build_job_name", "$build_ID", "tf114")}
timeout(5){util_init.update_sv_steps("$max_steps")}
if(run_choice=='1c8s' || run_choice=='both'){
    try{
        timeout(20){util_make.load_kmd_new('1c8s')}
        timeout(1440){util_test.sv_model_test("sv")}
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}
else{
    try{
        timeout(20){util_make.load_kmd_new('4c32s')}
        timeout(1440){util_test.sv_model_test("sv_4c")}
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}
timeout(10){util_log.get_log()}
