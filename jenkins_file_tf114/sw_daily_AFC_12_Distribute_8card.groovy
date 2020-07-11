stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}

timeout(10){util_init.get_repo("${manifest}")}
timeout(10){util_init.load_wiwigaga_distributed()}

timeout(10){util_init.checkout_by_commit("benchmark", "${env.benchmark_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
timeout(5){util_init.main_commit_get()}

timeout(10){util_make.build_kmd()}
timeout(30){util_make.load_kmd_new("${asic_config}")}
currentBuild.description+="<br>build: ${build_job_name};"
currentBuild.description+="<br>build: ${build_ID};"

if( "${model_name}" == "resnet" ){
    timeout(200){util_init.check_or_load_dataset("${env.WORKSPACE}","${dataset}")}
}

stage('--benchmark distribute test --'){
    try{
        if(( "${docker_test}" == "true" ) && ( "${model_name}" == "resnet" )){
            def myImage = docker.image("${docker_imgage}")
            myImage.inside("-tid -d -u 0:0 --user root --privileged --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host --device=/dev/dtu0:/dev/dtu0 -e PATH=$PATH"){
                echo "docker test resnet"
                timeout(20){util_make.copy_install_sdk_deb("${build_job_name}", "${build_ID}")}
                timeout(30){util_make.copy_install_tf_whl("${build_job_name}", "${build_ID}", 'tf114')}
                timeout(50){util_make.copy_install_eccl_horovod("${build_job_name}", "${build_ID}")}
                util_test.distribute_resnet_test("${batch}", "${epoch}", "${step}", "${dtype}", "${data_format}", "${training_test_env}", "${model_name}", "${dataset}", "${hostfile}", "${np}")
            }//docker run
        }//if
        else if(( "${docker_test}" == "false" ) && ( "${model_name}" == "resnet" )){
                timeout(20){util_make.copy_install_sdk_deb("${build_job_name}", "${build_ID}")}
                timeout(30){util_make.copy_install_tf_whl("${build_job_name}", "${build_ID}", 'tf114')}
                timeout(50){util_make.copy_install_eccl_horovod("${build_job_name}", "${build_ID}")}
                util_test.distribute_resnet_test("${batch}", "${epoch}", "${step}", "${dtype}", "${data_format}", "${training_test_env}", "${model_name}", "${dataset}", "${hostfile}", "${np}")
        }
        else if(( "${docker_test}" == "true" ) && ( "${model_name}" == "X2" )){
        def myImage = docker.image("${docker_imgage}")
            myImage.inside("-tid -d -u 0:0 --user root --privileged --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host --device=/dev/dtu0:/dev/dtu0 -e PATH=$PATH"){
                timeout(20){util_make.copy_install_sdk_deb("${build_job_name}", "${build_ID}")}
                timeout(30){util_make.copy_install_tf_whl("${build_job_name}", "${build_ID}", 'tf114')}
                timeout(50){util_make.copy_install_eccl_horovod("${build_job_name}", "${build_ID}")}
                util_test.distribute_X2_test("${training_test_env}", "${X2_steps}", "${hostfile}", "${np}")
            }
        }
        else if(( "${docker_test}" == "false" ) && ( "${model_name}" == "X2" )){
                timeout(20){util_make.copy_install_sdk_deb("${build_job_name}", "${build_ID}")}
                timeout(30){util_make.copy_install_tf_whl("${build_job_name}", "${build_ID}", 'tf114')}
                timeout(50){util_make.copy_install_eccl_horovod("${build_job_name}", "${build_ID}")}
                util_test.distribute_X2_test("${training_test_env}", "${X2_steps}", "${hostfile}", "${np}")
        }
    }//try
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        timeout(10){util_log.get_log()}
    }
}//stage