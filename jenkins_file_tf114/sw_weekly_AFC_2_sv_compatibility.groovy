stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){util_init.get_repo("$manifest")}
timeout(5){util_init.load_wiwigaga()}
timeout(10){util_init.checkout_by_commit("benchmark", "${env.benchmark_Commit}")}
timeout(10){util_init.checkout_by_commit("dtu_kmd", "${env.kmd_Commit}")}
timeout(10){util_init.fetch_change_patch("benchmark", "${env.benchmark_refID}")}
timeout(10){util_init.fetch_change_patch("dtu_kmd", "${env.kmd_refID}")}
timeout(5){util_init.main_commit_get()}
timeout(10){util_make.copy_install_kmd_rpm('$build_job_name', '$build_ID')}
currentBuild.description+="<br>build_job_name: ${build_job_name}<br>build_ID: ${build_ID};"

def image4c = docker.image("fe11a4aba1fa")
image4c.inside('--user root --network host --privileged -v /data/:/home/ -v /root/.ssh:/root/.ssh/ --device=/dev/dtu0:/dev/dtu0'){
    timeout(20){util_make.copy_install_sdk_deb('$build_job_name', '$build_ID')}
    timeout(120){util_make.copy_install_tf_whl('$build_job_name', '$build_ID', 'tf114')}
    try{
        timeout(600){util_test.sv_model_test("sv_4c")}
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        timeout(10){util_log.get_log()}
    }
}

timeout(20){util_make.change_kmd_rpm_layout("1c8s")}
def image1c = docker.image("fe11a4aba1fa")
image1c.inside('--user root --network host --privileged -v /data/:/home/ -v /root/.ssh:/root/.ssh/ --device=/dev/dtu0:/dev/dtu0'){
    timeout(20){util_make.copy_install_sdk_deb('$build_job_name', '$build_ID')}
    timeout(120){util_make.copy_install_tf_whl('$build_job_name', '$build_ID', 'tf114')}
    try{
        timeout(200){util_test.sv_model_test("sv")}
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        timeout(10){util_log.get_log()}
    }
}