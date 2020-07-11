/*
 * @Author: your name
 * @Date: 2020-05-21 14:42:01
 * @LastEditTime: 2020-06-05 13:10:17
 * @LastEditors: Please set LastEditors
 * @Description: prepare test envrionment for distribute. etc:get_repo, kmd sdk whl install, load dataset. exclude test execution.
                input: string of node name
 * @FilePath: /jenkinsfile/integration_util/util_distribute.groovy
 */

def distribute_install(String base_image){
        stage('--load common util--'){
        	util_init = load "jenkinsfile/common_util/util_init.groovy"
        	util_make = load "jenkinsfile/common_util/util_make.groovy"
        	util_test = load "jenkinsfile/common_util/util_test.groovy"
        	util_log = load "jenkinsfile/common_util/util_log.groovy"
            util_distribute = load "jenkinsfile/integration_util/util_distribute.groovy"
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
        
        timeout(200){util_init.check_or_load_dataset("${env.WORKSPACE}","${dataset}")}

        /*
        flag_image: for parallel sync semaphore
                container_unready: install not start
                container_ready: install success but not commit
                container_fail: install fail
                image_ready: image commit success, ready to run docker.inside("distribute_dtu:ci_test")
        */
        def flag_image = "container_unready"
        parallel(
            "docker install":{
                try{
                    stage('-- docker start test --'){
                        docker.image("${base_image}").inside {
                            timeout(20){util_make.copy_install_sdk_deb("JF_sw_daily_0_build", "428")}
                            timeout(30){util_make.copy_install_tf_whl("JF_sw_daily_0_build", "428", 'tf114')}
                            timeout(50){util_make.copy_install_eccl_horovod("JF_sw_daily_0_build", "428")}
                            // save container.id into dist_container_installed, transfer to "docker save"; maybe not robust as tail -n 1 not match some specific scenario
                            dist_container_installed=sh(script: "tail -n 1 /proc/self/cgroup|cut -d/ -f3|cut -c1-12",returnStdout: true).trim()
                            flag_image = "container_ready"
                            // use timer to wait for "docker save"
                            sleep time: 2, unit: 'MINUTES'
                        }
                    }
                }
                catch(e){
                    currentBuild.result = 'FAIL'
                    result = "FAIL"
                }
                finally{
                    //image install stat, set fail for jump out "docker save"'s waitUntil
                    if (flag_image == "container_unready"){
                        flag_image = "container_fail"
                    }
                    echo "[docker install]finally flag_image: ${flag_image}"
                }
            },//branch1

            "docker save":{
                try{
                    stage('-- docker save --'){
                        waitUntil {
                            (flag_image != "container_unready")
                        }
                        // execute if container ready, or dist_container_installed is not defined
                        if (flag_image == "container_ready"){
                            dir('./'){
                                echo "commit docker container: ${dist_container_installed}"
                                // define image repo:tag with constant 'distribute_dtu:ci_test'
                                sh"docker commit ${dist_container_installed} distribute_dtu:ci_test"
                                flag_image = "image_ready"
                            }
                        }
                        echo "the test image state: ${flag_image}"
                    }
                }
                catch(e){
                    currentBuild.result = 'FAIL'
                    result = "FAIL"
                }
                finally{
                    if (flag_image == "image_ready"){
                        def containerForTest = docker.image("distribute_dtu:ci_test").run("-tid -d -u 0:0 --user root --privileged --volume=/home:/home --volume=${env.WORKSPACE}/cache/:/root/.cache/ --volume=/root/.ssh/:/root/.ssh/  --network host --device=/dev/dtu0:/dev/dtu0 -e PATH=$PATH")
                        echo "container start for test: ${containerForTest.id}"
                    }
                }
            }//branch2
        )//parallel
}//distribute_install

/* TBD
stop and remove all container
remove the image updated and keep the base image
def distribute_container_stop(String dist_node){
        sh'''
            docker rm -f $(docker ps -aq)
            docker rmi distribute_dtu:ci_test --no-prune
        '''
 }//distribute_container_stop
*/

return this //required by groovy rule, don't delete.