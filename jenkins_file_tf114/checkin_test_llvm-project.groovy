stage('--load common util--'){
    util_init = load "jenkinsfile/common_util/util_init.groovy"
    util_make = load "jenkinsfile/common_util/util_make.groovy"
    util_test = load "jenkinsfile/common_util/util_test.groovy"
    util_log = load "jenkinsfile/common_util/util_log.groovy"
}
timeout(10){
    // here use beijing repo instead of shanghai one, to accelerate speed as build node uses beijing one
    stage('--get repo---'){
        dir('./'){
            sh"""
                repo init -u git@gitbj.enflame.cn:sw/manifest.git -m checkin_test_llvm-project.xml --repo-url http://git.enflame.cn/licheng.xu/git-repo.git --repo-branch master --no-repo-verify
                repo sync --force-sync
            """
        }
    }
}
timeout(5){util_init.update_repo_by_gerrit()}

timeout(30){
    stage('--llvm build--'){
        dir('llvm-project')
        {
            sh '''#!/bin/bash
                mkdir build
                cd build
                cmake -DLLVM_ENABLE_PROJECTS='clang;llvm;lld;libdturt' -DLLVM_TARGETS_TO_BUILD=DTU -DBUILD_SHARED_LIBS=ON ../llvm
                make -j32
            '''
        }
    }
}

timeout(60){
    stage('--compiler test--'){
        dir('compiler_test')
        {
            sh '''#!/bin/bash
                export PATH=$WORKSPACE/llvm-project/build/bin:$PATH
                ./run_test
            '''
        }
    }
}