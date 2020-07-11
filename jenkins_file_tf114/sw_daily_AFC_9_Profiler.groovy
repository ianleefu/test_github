stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("${env.manifest}")}
timeout(5){util_init.load_wiwigaga()}
timeout(10){util_init.checkout_by_commit("benchmark", "${env.benchmark_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}

timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(20){util_make.copy_install_sdk_deb('$build_job_name', '$build_ID')}
timeout(120){util_make.copy_install_tf_whl('$build_job_name', '$build_ID', 'tf114')}
currentBuild.description+="<br>build: ${build_job_name};"
currentBuild.description+="<br>build: ${build_ID};"
timeout(200){util_init.check_or_load_dataset("${env.WORKSPACE}","${dataset}")}

try{
    timeout(20){util_make.load_kmd_new("${no_profiler_asic_config}")}
    util_test.resnet_train_test("${batch}", "${epoch}", "${step}", "${dtype}", "${data_format}", "${test_env}", "${model_name}", "${dataset}")
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
try{
    timeout(20){util_make.load_kmd_new("${asic_config}")}
    currentBuild.description+="<br>asic:${asic_config};batch:${batch};epoch:${epoch};format:${data_format};type:${dtype};"
    // profiler test need dtupp to enable/disable profiler
    timeout(10){util_make.copy_install_dtu_pp('$build_job_name', '$build_ID')}
    util_test.profiler_test("${batch}", "${epoch}", "${step}", "${dtype}", "${data_format}", "${test_env}", "${model_name}", "${dataset}")
    util_log.profiler_check()
    // second round profiler test, limit steps for dtupp
    util_test.profiler_test("${batch}", "${epoch}", "${step}", "${dtype}", "${data_format}", "${test_env}", "${model_name}", "${dataset}")
    // timeout(60){util_test.dtu_pp_run_test()}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}