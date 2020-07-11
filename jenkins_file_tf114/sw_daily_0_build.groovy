timestamps{
    node("$execNode"){
        try{
            currentBuild.description="${env.NODE_NAME};${backend_Commit};"
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
            // generate version
            def version="0.0"
            if("${env.tensorflow_Commit}"){
                version="${env.tensorflow_Commit}.${env.BUILD_NUMBER}"
            }
            
            timeout(10){util_init.get_repo("${env.manifest}")}
            timeout(10){util_init.checkout_by_commit("tensorflow", "${env.tensorflow_Commit}")}
            timeout(10){util_init.checkout_by_commit("dtu_backend", "${env.backend_Commit}")}
            timeout(10){util_init.checkout_by_commit("dtu_sdk", "${env.sdk_Commit}")}
            timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
            timeout(10){util_init.checkout_by_commit_eccl("${env.eccl_Commit}")}
            timeout(10){
                util_init.checkout_by_commit("infra", "${env.infra_Commit}")
                util_init.individual_commit_get("infra")
            }
            
            timeout(10){util_init.fetch_change_patch("tensorflow", "${env.tensorflow_refID}")}
            timeout(10){util_init.fetch_change_patch("dtu_backend", "${env.backend_refID}")}
            timeout(10){util_init.fetch_change_patch("dtu_sdk", "${env.sdk_refID}")}
            timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
            timeout(10){util_init.fetch_change_patch("eccl", "${env.eccl_refID}")}
            parallel(
                "tf, sdk, kmd, eccl package build":{
                    def myImage = docker.image('535862119440')
                    myImage.inside("-tid --user root --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host -e PATH=$PATH"){
                        timeout(30){util_make.build_install_sdk("false")}
                        timeout(120){util_make.build_install_tensorflow("false", "tf114", "true")}
                        // build eccl in docker as the image already contains needed tools
                        timeout(30){util_make.build_eccl_deb()}
                        if("${env.backend_Commit}"){
                            dir('./tensorflow/tf_pkg'){
                                sh"cp tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.${version}.whl"
                                archiveArtifacts allowEmptyArchive: true, artifacts: '*'
                            }
                        }
                    }//docker
                    timeout(5){
                        util_init.main_commit_get()
                        util_init.individual_commit_get("eccl")
                    }
            
                    //build kmd rpm package in tlinux machine
                    node("sse_lab_CI_006"){
                        cleanWs()
                        timeout(10){util_init.get_repo("${env.manifest}")}
                        timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
                        timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
                        timeout(10){util_make.build_kmd_rpm(env.BUILD_NUMBER)}
                    }
                    // build kmd deb package
                    timeout(10){util_make.build_kmd_deb(env.BUILD_NUMBER)}
                }, // parallel stream tf, sdk, kmd, eccl package build
            
                "dtu_kmd ubuntu 1804 build":{
                    //build kmd deb package in ubuntu 1804 machine
                    node("sse_lab_CI_011 "){
                        cleanWs()
                        timeout(10){util_init.get_repo("${env.manifest}")}
                        timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
                        timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
                        timeout(10){util_make.build_kmd_deb(env.BUILD_NUMBER)}
                    }
                },
            
                "dtupp package build":{
                    // build dtupp and artifact
                    node("software-build-01"){
                        cleanWs()
                        timeout(10){util_init.get_repo("${env.manifest}")}
                        timeout(10){util_init.checkout_by_commit("dtu_pp", "${env.dtu_pp_Commit}")}
                        timeout(10){util_init.checkout_by_commit("dtu_prometheus_monitor", "${env.dtu_monitor_Commit}")}
                        timeout(10){util_init.fetch_change_patch("dtu_pp", "${env.dtu_pp_refID}")}
                        timeout(10){util_init.fetch_change_patch("dtu_prometheus_monitor", "${env.dtu_monitor_refID}")}
                        timeout(5){
                            util_init.individual_commit_get("dtu_pp")
                            util_init.individual_commit_get("dtu_prometheus_monitor")
                        }
                        timeout(10){util_make.build_dtu_pp()}
                        timeout(10){util_make.build_dtu_prometheus_monitor()}
                    }
                }// parallel stream dtupp package build
            )
            
            timeout(10){util_make.generatePackage(version, currentBuild.projectName, "$currentBuild.number", "${env.dtu_models_commit}", "${env.dtu_models_refID}")}

            emailext (
                subject: "SUCCESSED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """<p>SUCCESSED: Job '${env.JOB_NAME}' ['${env.BUILD_NUMBER}']:</p>
                        <p>Check console output at "<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
                to: "sw_qa@enflame-tech.com"
            )
        }
        catch(e){
            currentBuild.result = 'FAIL'
            result = "FAIL"
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                        <p>Check console output at "<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
                to: "sw_qa@enflame-tech.com"
            )
        }//catch
    }//node
    if(currentBuild.resultIsBetterOrEqualTo("SUCCESS") && "${env.auto_trigger_test}"=="YES"){
        build job: "${env.trigger_job_name}", parameters: [
            string(name: 'build_job_name', value: "${env.JOB_NAME}"),
            string(name: 'build_ID', value: "${env.BUILD_NUMBER}"),
            string(name: 'tensorflow_refID', value: "${env.tensorflow_refID}"),
            string(name: 'backend_refID', value: "${env.backend_refID}"),
            string(name: 'sdk_refID', value: "${env.sdk_refID}"),
            string(name: 'kmd_refID', value: "${env.kmd_refID}"),
            string(name: 'benchmark_refID', value: "${env.benchmark_refID}"),
            string(name: 'umd_refID', value: "${env.umd_refID}"),
            string(name: 'eccl_refID', value: "${env.eccl_refID}"),
            string(name: 'umd_Commit', value: "${env.umd_Commit}"),
            string(name: 'kmd_Commit', value: "${env.kmd_Commit}"),
            string(name: 'sdk_Commit', value: "${env.sdk_Commit}"),
            string(name: 'tensorflow_Commit', value: "${env.tensorflow_Commit}"),
            string(name: 'backend_Commit', value: "${env.backend_Commit}"),
            string(name: 'benchmark_Commit', value: "${env.benchmark_Commit}"),
            string(name: 'infra_Commit', value: "${env.infra_Commit}"),
            string(name: 'eccl_Commit', value: "${env.eccl_Commit}"),
            string(name: 'manifest', value: "${env.manifest}"),
            string(name: 'performance_node', value: "${env.performance_node}"),
            string(name: 'infra_refID', value: "${env.infra_refID}")]
    }
}