stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
try{
    timeout(10){util_init.git_clone("${env.user_name}", "benchmark", "${env.benchmark_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "tensorflow_114", "${env.tensorflow_114_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_backend", "${env.dtu_backend_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_sdk", "${env.dtu_sdk_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_umd", "${env.dtu_umd_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_kmd", "${env.dtu_kmd_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_pp", "${env.dtu_pp_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_prometheus_monitor", "${env.dtu_prometheus_monitor_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "eccl", "${env.eccl_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "dtu_models", "${env.dtu_models_branch}")}
    timeout(10){util_init.git_clone("${env.user_name}", "infra", "${env.infra_branch}")}
    timeout(10){util_init.git_tag("${env.tag_name}", "${env.tag_msg}")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}