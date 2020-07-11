stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("${env.manifest}")}
timeout(5){util_init.load_wiwigaga()}
timeout(10){util_init.checkout_by_commit("dtu_backend", "${env.backend_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}

timeout(10){util_init.fetch_change_patch("dtu_backend", "${env.backend_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}

timeout(5){util_init.main_commit_get()}

timeout(10){util_make.copy_install_kmd_deb('$build_job_name', '$build_ID')}
timeout(20){util_make.copy_install_sdk_deb('$build_job_name', '$build_ID')}
timeout(120){util_make.copy_install_tf_whl('$build_job_name', '$build_ID', 'tf114')}
try{
     // moba_test.py need about 40 min+
    timeout(60){util_test.op_python_test("tf114_release_op_env_setup_1c8s.sh", "operator_ci_daily_test_non_bf16_list.txt")}

    timeout(50){util_test.op_python_test("tf114_release_op_env_setup_1c8s.sh", "operator_ci_test_non_bf16_list.txt")}
    timeout(30){util_test.op_python_test("tf114_release_op_bf16_env_setup_1c8s.sh", "operator_ci_test_bf16_list.txt")}
    timeout(30){util_test.op_python_test("tf114_release_op_rnn_env_setup_1c8s.sh", "operator_ci_test_rnn_list.txt")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_make.uninstall_kmd_deb()}
    timeout(10){util_log.get_log()}
}
