def sdkFileName="dtu_sdk_1.0_amd64.deb"
def submit_time=''
def kmd_commit=''
def benchmark_commit=''
def sdk_commit=''
def backend_commit=''
def tensorflow_114_commit=''

timestamps{
    node("sse_build_sh"){
        currentBuild.description="build node: ${env.NODE_NAME}"
        stage('--clean ws--'){
            dir('./'){
                cleanWs()
                withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
                    withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                        sh"git clone ssh://jenkins@gerrit.enflame.cn:29418/jenkinsfile"
                    }
                }
            }
        }
        stage('--load common util--'){
            util_init = load "jenkinsfile/common_util/util_init.groovy"
            util_make = load "jenkinsfile/common_util/util_make.groovy"
            util_test = load "jenkinsfile/common_util/util_test.groovy"
            util_log = load "jenkinsfile/common_util/util_log.groovy"
        }
        timeout(10){util_init.get_repo('Leo_tf114_develop.xml')}
        //get current gerrit patchset submit time
        submit_time=timeout(5){util_log.get_submit_time("$GERRIT_PATCHSET_REVISION")}
        //get specify time commit id
        kmd_commit=timeout(10){util_init.get_specify_time_commit_id("dtu_kmd", submit_time)}
        benchmark_commit=timeout(10){util_init.get_specify_time_commit_id("benchmark", submit_time)}
        sdk_commit=timeout(10){util_init.get_specify_time_commit_id("dtu_sdk", submit_time)}
        backend_commit=timeout(10){util_init.get_specify_time_commit_id("dtu_backend", submit_time)}
        tensorflow_114_commit=timeout(10){util_init.get_specify_time_commit_id("tensorflow_114", submit_time)}

        //we need checkout all repo which affect build package
        timeout(5){util_init.checkout_by_commit("dtu_sdk", sdk_commit)}
        timeout(5){util_init.checkout_by_commit("dtu_backend", backend_commit)}
        timeout(5){util_init.checkout_by_commit("tensorflow_114", tensorflow_114_commit)}

        def myImage = docker.image('535862119440')
        myImage.inside("-tid --user root --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host -e PATH=$PATH"){
            timeout(30){sdkFileName=util_make.build_install_sdk("false")}
            stash includes: 'dtu_sdk/dtu_sdk*.deb', name: 'sdk_deb'
            timeout(120){util_make.build_install_tensorflow("false", "tf114", "false")}
            stash includes: 'tensorflow/tf_pkg/**', name: 'tf_pkg'
        }//docker
    }

    node("performance_test_asic"){
        currentBuild.description+="<br>------------------------------------------------<br>"
        currentBuild.description+="performance test node: ${env.NODE_NAME}"
        stage('--clean ws--'){
            cleanWs()
        }
        timeout(5){util_init.load_wiwigaga()}
        timeout(10){util_init.get_repo('Leo_tf114_develop.xml')}

        timeout(5){util_init.checkout_by_commit("dtu_kmd", kmd_commit)}
        timeout(5){util_init.checkout_by_commit("benchmark", benchmark_commit)}
        timeout(5){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}

        timeout(10){util_make.build_kmd()}
        timeout(10){util_make.load_kmd_new('1c8s')}
        try{
            timeout(10){util_test.resnet_train_test_zebu("64", "2", "0", "bf16", "CHNW", "tf114_daily_env_setup_1c8s.sh")}
            timeout(10){util_make.load_kmd_new('4c32s')}
            timeout(50){util_test.resnet_train_test_zebu("64", "2", "0", "bf16", "CHNW", "tf114_daily_env_setup_4c32s.sh")}
        }
        catch(e){
            currentBuild.result = 'FAIL'
            result = "FAIL"
        }
        finally{
            timeout(10){util_log.get_log()}
            timeout(5){util_log.performance_check()}
        }
    }

    node("convergence_test_asic"){
        currentBuild.description+="<br>------------------------------------------------<br>"
        currentBuild.description+="convergence test node: ${env.NODE_NAME}"
        stage('--clean ws--'){
            cleanWs()
        }
        timeout(5){util_init.load_wiwigaga()}
        timeout(10){util_init.get_repo('Leo_tf114_develop.xml')}

        timeout(5){util_init.checkout_by_commit("dtu_kmd", kmd_commit)}
        timeout(5){util_init.checkout_by_commit("benchmark", benchmark_commit)}
        timeout(5){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}

        timeout(10){util_make.build_kmd()}
        timeout(10){util_make.load_kmd_new("$asic_config")}
        try{
            timeout(200){util_test.convergence_regression_test("$batch", "$epoch", "$step", "$dtype", "$data_format", "$training_test_env", "$model_name")}
            //if(model_name=="resnet"){
            //    util_log.convergence_check("${base_event}")
            //}
        }
        catch(e){
            currentBuild.result = 'FAIL'
            result = "FAIL"
        }
        finally{
            timeout(10){util_log.get_log()}
        }
    }

}
