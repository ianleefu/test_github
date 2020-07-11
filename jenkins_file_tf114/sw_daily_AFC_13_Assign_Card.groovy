stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}

timeout(10){util_init.get_repo("${manifest}")}
stage('--wiwigaga--'){
    dir('/root/efvs'){
        sh"""
            rmmod leo || true
            rmmod dtu || true
            rmmod dtu_kcl || true
            ./efsmt -pcie reset hot -d dtu.*
            ./wiwigaga5.sh ${wwgg} 0
            ./wiwigaga5.sh ${wwgg} 1
            ./wiwigaga5.sh ${wwgg} 2
            ./wiwigaga5.sh ${wwgg} 3
            ./wiwigaga5.sh ${wwgg} 4
            ./wiwigaga5.sh ${wwgg} 5
            ./wiwigaga5.sh ${wwgg} 6
            ./wiwigaga5.sh ${wwgg} 7
        """
    }
}
timeout(10){util_init.checkout_by_commit("benchmark", "${env.benchmark_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.build_kmd()}
timeout(30){util_make.load_kmd_new("${asic_config}")}
timeout(20){util_make.copy_install_sdk_deb('$build_job_name', '$build_ID')}
timeout(30){util_make.copy_install_tf_whl('$build_job_name', '$build_ID', 'tf114')}
timeout(50){util_make.copy_install_eccl_horovod('$build_job_name', '$build_ID')}
currentBuild.description+="<br>build: ${build_job_name};"
currentBuild.description+="<br>build: ${build_ID};"

timeout(200){util_init.check_or_load_dataset("${env.WORKSPACE}","${dataset}")}
stage('--benchmark assign card test --'){
    try{
        util_test.resnet_assign_card_test("${batch}", "${epoch}", "${step}", "${dtype}", "${data_format}", "${test_env}", "${model_name}", "${dataset}", "${cardNum}", "${cardId}")
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        timeout(10){util_log.get_log()}
    }
}