timestamps{
    node("sse_build_sh"){
        currentBuild.description="${env.NODE_NAME};"
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
        if (GERRIT_PROJECT in ["dtu_backend", "dtu_sdk", "tensorflow_114"]){
            timeout(5){util_init.checkout_by_commit("$GERRIT_PROJECT", "$GERRIT_PATCHSET_REVISION")}
        }
        timeout(5){util_init.main_commit_get()}
        def myImage = docker.image('535862119440')
        myImage.inside("-tid --user root --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host -e PATH=$PATH"){
            timeout(30){util_make.build_install_sdk("false")}
            stash includes: 'dtu_sdk/dtu_sdk*.deb', name: 'sdk_deb'
            timeout(120){util_make.build_install_tensorflow("false", "tf114", "false")}
            stash includes: 'tensorflow/tf_pkg/**', name: 'tf_pkg'
        }//docker
    }
    node("convergence_test_asic"){
        stage('--clean ws--'){
            cleanWs()
        }
        timeout(5){util_init.load_wiwigaga()}
        timeout(10){util_init.get_repo('Leo_tf114_develop.xml')}
        if (GERRIT_PROJECT in ["dtu_kmd", "benchmark"]){
            timeout(5){util_init.checkout_by_commit("$GERRIT_PROJECT", "$GERRIT_PATCHSET_REVISION")}
        }
        timeout(10){util_make.build_kmd()}
        timeout(10){util_make.load_kmd_new("$asic_config")}
        try{
            timeout(200){util_test.resnet_train_test_zebu("$batch", "$epoch", "$step", "$dtype", "$data_format", "$training_test_env", "$model_name")}
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
