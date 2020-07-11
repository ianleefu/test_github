stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("Leo_develop.xml")}
timeout(10){util_init.checkout_by_commit("tensorflow", "${env.tensorflow_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_backend", "${env.backend_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_sdk", "${env.sdk_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}

timeout(10){util_init.fetch_change_patch("tensorflow", "${env.tensorflow_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_backend", "${env.backend_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_sdk", "${env.sdk_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}

timeout(5){util_init.main_commit_get()}


try{
    def myImage = docker.image('8c3c0f3c1c35')
    myImage.inside("-tid --user root --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host -e PATH=$PATH"){
        timeout(30){util_make.build_install_sdk("false")}
        if("${env.sdk_commit}"){
            dir('./dtu_sdk'){
                sh"cp dtu_sdk_1.0_amd64.deb dtu_sdk_1.0_amd64.${sdk_commit}.deb"
                archiveArtifacts allowEmptyArchive: true, artifacts: "dtu_sdk_1.0_amd64.${sdk_commit}.deb"
            }
        }

        timeout(120){util_make.build_install_tensorflow("false", "tf18", "false")}
        if("${env.backend_Commit}"){
            dir('./tensorflow/tf_pkg'){
                sh"cp tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.${backend_Commit}.whl"
                archiveArtifacts allowEmptyArchive: true, artifacts: '*'
            }
        }
    }//docker
}//try
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}