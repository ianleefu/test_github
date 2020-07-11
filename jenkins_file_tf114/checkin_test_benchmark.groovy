stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo('checkin_test_benchmark_tf114.xml')}
timeout(5){util_init.update_repo_by_gerrit()}
timeout(5){util_init.load_wiwigaga()}

timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}

timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(20){util_make.copy_install_sdk_deb("$build_job_name", "$build_ID")}
timeout(120){util_make.copy_install_tf_whl("$build_job_name", "$build_ID", "tf114")}
try{
    timeout(20){util_make.load_kmd_new('4c32s')}
    if ( "${env.GERRIT_CHANGE_SUBJECT}".toLowerCase().contains("object detection") ){
        timeout(20){util_test.object_detection_train_test("4", "1", "4", "bf16", "CHNW", "tf114_daily_env_setup_4c32s_yolo.sh","yolo_v2","voc2007")}
        timeout(20){util_test.object_detection_train_test("8", "1", "4", "bf16", "CHNW", "tf114_daily_env_setup_4c32s_yolo.sh","yolo_v2","voc2007")}
        timeout(20){util_test.object_detection_train_test("16", "1", "4", "bf16", "CHNW", "tf114_daily_env_setup_4c32s_yolo.sh","yolo_v2","voc2007")}
        timeout(20){util_test.object_detection_train_test("4", "1", "2", "bf16", "CHNW", "tf114_daily_env_setup_4c32s_yolo_v3.sh","yolo_v3","voc2012")}

    }
    else{
        timeout(20){util_test.resnet_train_test("4", "1", "10", "bf16", "CHNW", "tf114_daily_env_setup_4c32s.sh")}
        timeout(20){util_test.resnet_train_test("16", "1", "10", "bf16", "CHNW", "tf114_daily_env_setup_4c32s.sh")}
        timeout(20){util_test.resnet_train_test("32", "1", "10", "bf16", "CHNW", "tf114_daily_env_setup_4c32s.sh")}
        timeout(20){util_test.resnet_train_test("64", "1", "10", "bf16", "CHNW", "tf114_daily_env_setup_4c32s.sh")}

        timeout(20){util_test.resnet_train_test("8", "1", "10", "fp32", "NHWC", "tf114_daily_env_setup_4c32s_googlenet_alexnet.sh", "alexnet")}
        timeout(20){util_test.resnet_train_test("2", "1", "10", "fp32", "NHWC", "tf114_daily_env_setup_4c32s_googlenet_alexnet.sh", "googlenet")}
        timeout(20){util_test.resnet_train_test("2", "1", "1", "fp32", "NHWC", "tf114_daily_env_setup_4c32s_vgg.sh", "vgg")}
    }
    util_log.get_fps_result("${env.WORKSPACE}")

    //load sv test module
    load "jenkinsfile/integration_util/module_sv_model.groovy"
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}
finally{
    timeout(10){util_log.get_log()}
}
/* exclude test case
    train_no_profiler.sh vgg_test 4 dtu 1
    train_no_profiler.sh googlenet_test 4 dtu 1
    train_no_profiler.sh deepbench_test 4 dtu 1
*/
