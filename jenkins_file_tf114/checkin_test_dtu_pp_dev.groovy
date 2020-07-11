// stage('--load common util--'){
//     util_init = load "jenkinsfile/common_util/util_init.groovy"
//     util_make = load "jenkinsfile/common_util/util_make.groovy"
//     util_test = load "jenkinsfile/common_util/util_test.groovy"
//     util_log = load "jenkinsfile/common_util/util_log.groovy"
// }

stage('<--get source code-->'){
    dir ('./dtu_pp'){
        checkout([$class: 'GitSCM',
            branches: [[name: "$GERRIT_REFSPEC"]],
            doGenerateSubmoduleConfigurations: false,
            extensions:[],
            submoduleCfg: [],
            userRemoteConfigs: [[
                credentialsId: 'gerrit_user_jenkins',
                refspec: 'refs/changes/*:refs/changes/*',
                url: "ssh://jenkins@gerrit.enflame.cn:29418/${GERRIT_PROJECT}",
                name: "${GERRIT_PROJECT}"
            ]]
        ])
    }
}

stage('<--main commit get-->'){
    dir('./dtu_pp'){
        dtu_pp=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
        currentBuild.description+="<br>dtu_pp_commit:${dtu_pp};"
    }
}//stage

stage('<--build profiler-->'){
    try {
        dir('./dtu_pp'){
            sh'''
                sudo update-alternatives --set gcc "/usr/bin/gcc-7"
                gcc --version
                bazelisk clean --expunge
                bazelisk build //lib:rel_pkg
                bazelisk test //tests:all
            '''
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        sh '''
            sudo update-alternatives --set gcc "/usr/bin/gcc-5"
        '''
    }

    // archiveArtifacts allowEmptyArchive: true, artifacts: '*.deb'
}