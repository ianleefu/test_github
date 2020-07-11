def build_install_sdk(String clean_cache='true'){
    stage('--build and install sdk--'){
        dir('./dtu_sdk'){
            if(clean_cache=='true'){
                sh"bazelisk clean --expunge"
            }
            sh'''
                bazelisk build  //lib:sdk_lib_deb
                ./scripts/change_deb_name.sh
                dpkg -r dtu_sdk || true
                dpkg -i bazel-bin/lib/dtu_sdk*.deb
                cp bazel-bin/lib/dtu_sdk*.deb ./
                mkdir rename
                cp bazel-bin/lib/dtu_sdk*.deb ./rename/dtu_sdk.deb
            '''
        }
        dir('./dtu_sdk'){
            archiveArtifacts allowEmptyArchive: true, artifacts: '*.deb'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'rename/*.deb'
        }
        // return file name of the sdk package. as each time the name is generated differently
        dir('./dtu_sdk'){
            files=findFiles(glob: 'dtu_sdk*.deb')
            fileName=files[0].name
        }
    }
    return fileName
}

def copy_install_sdk_deb(String build_job_name, String build_id){
    stage('--copy and install sdk--'){
        dir('./'){
            copyArtifacts filter: 'dtu_sdk*.deb', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
            sh'''
                dpkg -r dtu_sdk || true
                dpkg -i dtu_sdk*.deb
            '''
        }
    }
}

def build_install_mock_sdk(String commit_id, boolean clean_cache=true){
    stage('--mock sdk build and install--'){
        dir('./dtu_sdk'){
            if(clean_cache){
                sh"bazelisk clean --expunge"
            }
            sh"""
                bazelisk build --define dtu_mock=true //lib:sdk_lib_deb
                ./scripts/change_deb_name.sh
                cp bazel-bin/lib/dtu_sdk*.deb ./
                sudo dpkg -r dtu_sdk || true
                sudo dpkg -i dtu_sdk*.deb
                # mock_sipmodel is in sdk deb package, no longer need to copy for using
                rm -f ~/bin/mock_sipmodel || true
            """
            archiveArtifacts allowEmptyArchive: true, artifacts: '*.deb'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'bazel-bin/external/**/mock_sipmodel'
        }
    }
}

def build_kmd(){
    stage('--build kmd--'){
        dir('./dtu_kmd'){
            sh '''
                rmmod leo || true
                rmmod dtu || true
                rmmod dtu_kcl || true
                dkms remove dtu/1.0 --all || true
                git submodule update --init --recursive
                make clean
                make -j8
            '''
        }
    }
}

def build_kmd_rpm(String build_num_param){
    try{
        stage('--build kmd rpm--'){
            dir('./dtu_kmd'){
                sh """
                    #!/bin/bash
                    git submodule update --init --recursive
                    sudo make clean
                    sudo make -j8
                    sudo make rpm BUILD_NUMBER=${build_num_param} HIDE_SOURCE=1
                """
                // artifact the deb
                archiveArtifacts artifacts: 'build/*.rpm', fingerprint: true
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def build_kmd_deb(String build_num_param){
    try{
        stage('--build kmd deb--'){
            dir('./dtu_kmd'){
                sh """
                    #!/bin/bash
                    git submodule update --init --recursive
                    sudo make clean
                    sudo make -j8
                    sudo make deb BUILD_NUMBER=${build_num_param} HIDE_SOURCE=1
                """
                // artifact the deb
                archiveArtifacts artifacts: 'build/*.deb', fingerprint: true
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def copy_install_kmd_deb(String build_job_name, String build_id){
    stage('--copy and install kmd deb--'){
        dir('./'){
            copyArtifacts filter: '**/dtu-*.deb', excludes: '*sdk*', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
            sh'''
                dpkg -r dtu-dkms || true
                dpkg -i dtu-*.deb
                modprobe dtu
            '''
        }
    }
}

def copy_install_kmd_rpm(String build_job_name, String build_id){
    stage('--copy and install kmd rpm--'){
        dir('./'){
            copyArtifacts filter: '**/dtu-*.rpm', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
            sh'''
                rpm -e $(rpm -qa 'dtu*') || true
                rpm -i dtu-*.rpm
                modprobe dtu
            '''
        }
    }
}

def change_kmd_rpm_layout(String layout='1c8s'){
    stage('--change kmd rpm to 1c8s--'){
        sh"""
            rmmod leo || true
            rmmod dtu || true
            rmmod dtu_kcl || true
            if [ $layout == "1c8s" ];then
                modprobe dtu ip_mask=0x1fffff0000862e
            elif [ $layout == "4c32s" ];then
                modprobe dtu
            else
                echo "invalid parameter"
                exit 1
            fi
        """
    }
}

def uninstall_kmd_deb(){
    stage('--uninstall kmd deb--'){
        sh'''
            rmmod leo || true
            rmmod dtu || true
            rmmod dtu_kcl || true
            dpkg -r dtu-dkms || true
        '''
    }
}

def uninstall_kmd_rpm(){
    stage('--uninstall kmd rpm--'){
        sh'''
            rmmod leo || true
            rmmod dtu || true
            rmmod dtu_kcl || true
            rpm -e $(rpm -qa 'dtu*') || true
        '''
    }
}

def copy_install_eccl_horovod(String build_job_name, String build_id){
    stage('--copy and install eccl.deb horovod.whl--'){
        dir('./'){
            copyArtifacts filter: 'build/eccl*.deb,horovod/dist/horovod*.whl', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
            sh'''
                dpkg -r eccl || true
                python -m pip uninstall --yes horovod || true
                dpkg -i eccl*.deb
                pip install horovod*.whl
            '''
        }
    }
}

def build_eccl_deb(){
    stage('--build eccl deb--'){
        dir('./eccl'){
            sh '''
                git submodule update --init --recursive || true
                ./build_eccl_pkg.sh
            '''
            archiveArtifacts allowEmptyArchive: true, artifacts: 'build/eccl*.deb,horovod/dist/horovod*.whl'
        }
    }
}

def load_kmd_new(String type){
    stage('--load driver--'){
        dir('./'){
            sh'''
                cd /lib/firmware
                rm -rf ssm*
                rm -rf cqm*
            '''
        }
        dir('./dtu_kmd'){
            if (type in ['vdk', 'zebu', 'zebu_full', 'fpga', 'asic', 'cluster', 'asic_prof', 'cluster_prof', 'asic_dist']){
                sh"./scripts/kmd_util.sh config $type"
            }
            else if(type=='1c8s'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl release
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 1c8s
                '''
            }
            else if(type=='4c32s'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl release
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 4c32s
                '''
            }
            else if(type=='4c32s_dist'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl release
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 4c32s_dist
                '''
            }
            else if(type=='1c_performance'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl release
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 1c8s
                '''
            }
            else if(type=='4c_performance'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl release
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 4c32s
                '''
            }
            else if(type=='1c_profiler'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl debug
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 1c8s trace_enable=1 ip_ts_enable=1
                '''
            }
            else if(type=='4c_profiler'){
                sh'''
                    ./scripts/kmd_util.sh update cqm rtl debug
                    ./scripts/kmd_util.sh update ssm
                    ./scripts/kmd_util.sh load -l 4c32s trace_enable=1 ip_ts_enable=1
                '''
            }
            else{
                echo "invalid parameter!"
                exit 1
            }
            cqm_version=sh(
                script:'''#!/bin/bash
                cmd=`cd /root/efvs&&./efsmt -reg cqm.*fm_version|grep "CQM.0.CQM_FM_VERSION"|awk -F "= " '{print $NF}'`
                echo ${cmd:5:-3}
                ''',
                returnStdout: true
            ).trim()
            currentBuild.description+="<br>CQM_VERSION:${cqm_version}"
        }
    }
}

def build_install_tensorflow(clean_cache='false', String tf_type='tf18', String install='true', String python_ver='2'){
    stage('--build tensorflow--'){
        dir('./tensorflow'){
            // set environment variable
            if (python_ver=='2'){
                env.PYTHON_BIN_PATH='/usr/bin/python'
                env.PYTHON_LIB_PATH='/usr/local/lib/python2.7/dist-packages'
            }
            else if(python_ver=='3'){
                env.PYTHON_BIN_PATH='/usr/bin/python3'
                env.PYTHON_LIB_PATH='/usr/local/lib/python3.5/dist-packages'
            }
            else{
                echo 'not supported python version'
            }
            env.TF_NEED_JEMALLOC=0
            env.TF_NEED_GCP=0
            env.TF_NEED_HDFS=0
            env.TF_NEED_S3=0
            env.TF_NEED_KAFKA=0
            env.TF_ENABLE_XLA=1
            env.TF_NEED_GDR=0
            env.TF_NEED_VERBS=0
            env.TF_NEED_OPENCL_SYCL=0
            env.TF_NEED_CUDA=0
            env.TF_NEED_MPI=0
            env.TF_CUDA_CLANG=0
            env.TF_DOWNLOAD_CLANG=0
            env.CC_OPT_FLAGS='-march=native'
            env.TF_SET_ANDROID_WORKSPACE=0
            // configure tf with environment variables
            sh 'python ./configure.py'
            if(clean_cache=="true"){
                sh"bazelisk clean --expunge"
            }
            sh '''
                bazelisk build //tensorflow/tools/pip_package:build_pip_package
                rm -rf tf_pkg
                mkdir -p tf_pkg
                ./bazel-bin/tensorflow/tools/pip_package/build_pip_package tf_pkg
            '''
        }
        dir('./tensorflow/tf_pkg'){
            archiveArtifacts allowEmptyArchive: true, artifacts: '*'
        }
    }
    if(install=='true'){
        stage('--install '+tf_type+' whl--'){
            dir('./tensorflow'){
                sh'''
                    python -m pip uninstall --yes tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl || true
                    python -m pip uninstall --yes tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl || true
                    python3 -m pip uninstall --yes tf_pkg/tensorflow-1.14.0-cp35-cp35m-linux_x86_64.whl || true
                '''
                if(tf_type=='tf18'){
                    sh'yes | python -m pip install --user tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl'
                }
                else if(tf_type=='tf114'){
                    if (python_ver=='2'){
                        sh'yes | python -m pip install --user tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl'
                    }
                    else if(python_ver=='3'){
                        sh'yes | python3 -m pip install --user tf_pkg/tensorflow-1.14.0-cp35-cp35m-linux_x86_64.whl'
                    }
                }
                else {
                    echo "invalid parameter!"
                }
            }
        }
    }
}

def copy_install_tf_whl(String build_job_name, String build_id, String tf_type='tf18', String python_ver='2'){
    stage('--install '+tf_type+'--'){
        dir('./'){
            if(tf_type=='tf18'){
                copyArtifacts filter: 'tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
                sh'''
                    python -m pip uninstall --yes tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl || true
                    python -m pip uninstall --yes tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl || true
                    yes | python -m pip install --user tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl
                '''
            }
            else if(tf_type=='tf114'){
                if (python_ver=='2'){
                    copyArtifacts filter: 'tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
                    sh'''
                        python -m pip uninstall --yes tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl || true
                        python -m pip uninstall --yes tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl || true
                        yes | python -m pip install --user tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl
                    '''
                }
                else if(python_ver=='3'){
                    copyArtifacts filter: 'tensorflow-1.14.0-cp35-cp35m-linux_x86_64.wh', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
                    sh'''
                        python3 -m pip uninstall --yes tensorflow-1.14.0-cp35-cp35m-linux_x86_64.whl || true
                        yes | python3 -m pip install --user tensorflow-1.14.0-cp35-cp35m-linux_x86_64.whl
                    '''
                }
            }
            else {
                echo "invalid parameter!"
            }
        }
    }
}

def build_install_mock_tensorflow(String tf_type="tf18"){
    stage('--build mock tensorflow--'){
        dir('./tensorflow'){
            // this part is a temp solution for slow download from shanghai git server
            // replace llvm git repo to use beijing one
            // and use develop branch dtu_sdk
            sh '''
                python ../infra/jenkins_tools/TempBackendBzl.py ./tensorflow/compiler/plugin/dtu_backend/backend.bzl llvm git@gitbj.enflame.cn:sw/llvm.git
            '''
            // set environment variable
            env.PYTHON_BIN_PATH='/usr/bin/python'
            env.PYTHON_LIB_PATH='/usr/local/lib/python2.7/dist-packages'
            env.TF_NEED_JEMALLOC=0
            env.TF_NEED_GCP=0
            env.TF_NEED_HDFS=0
            env.TF_NEED_S3=0
            env.TF_NEED_KAFKA=0
            env.TF_ENABLE_XLA=1
            env.TF_NEED_GDR=0
            env.TF_NEED_VERBS=0
            env.TF_NEED_OPENCL_SYCL=0
            env.TF_NEED_CUDA=0
            env.TF_NEED_MPI=0
            env.TF_CUDA_CLANG=0
            env.TF_DOWNLOAD_CLANG=0
            env.CC_OPT_FLAGS='-march=native'
            env.TF_SET_ANDROID_WORKSPACE=0
            // configure tf with environment variables
            sh 'python ./configure.py'
            // build and artifact mock tensorflow
            sh '''
                bazelisk clean --expunge
                bazelisk build --define dtu_mock=true //tensorflow/tools/pip_package:build_pip_package
                rm -rf tf_pkg
                mkdir -p tf_pkg
                ./bazel-bin/tensorflow/tools/pip_package/build_pip_package tf_pkg
            '''
            if(tf_type=="tf18"){
                sh'cp tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.mock.whl'
            }
            else if(tf_type=="tf114"){
                sh'cp tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl.mock.whl'
            }
            else {
                echo "invalid parameter!"
            }
            archiveArtifacts allowEmptyArchive: true, artifacts: 'tf_pkg/**'
        }
    }
    stage('--install mock whl--'){
        dir('./tensorflow'){
            // Install tensorflow
            sh'''
                sudo pip uninstall --yes tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl || true
                sudo pip uninstall --yes tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl || true
            '''
            if(tf_type=="tf18"){
                sh'sudo pip install tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl'
            }
            else if(tf_type=="tf114"){
                sh 'sudo pip install tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl'
            }
            else {
                echo "invalid parameter!"
            }
        }
    }
}

def build_dtu_pp(){
    stage('<--build dtu_pp-->'){
        try {
            dir('./dtu_pp'){
                sh'''
                    sudo update-alternatives --set gcc "/usr/bin/gcc-7"
                    gcc --version
                    bazelisk clean
                    bazelisk build //lib:rel_pkg
                    ./scripts/change_deb_name.sh
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
        archiveArtifacts allowEmptyArchive: true, artifacts: 'dtu_pp/bazel-bin/lib/dtupp*.deb'
    }
}

def copy_install_dtu_pp(String build_job_name, String build_id){
    stage('--install dtu_pp --'){
        dir('./'){
            copyArtifacts filter: '**/dtupp*.deb', fingerprintArtifacts: true, flatten: true, projectName: "${build_job_name}", selector: specific("${build_id}"), target: './'
                sh'''
                    rm -rf /tmp/pp.conf || true
                    sudo dpkg -r dtupp || true
                    sudo dpkg -i dtupp*.deb

                    #disable version match check for dtupp and sdk
                    echo 'checkLibprofileDataVersion = false' >> /tmp/pp.conf
                '''
        }
    }
}

def build_dtu_prometheus_monitor(){
    stage('<--build dtu_prometheus_monitor-->'){
        try {
            dir('./dtu_prometheus_monitor'){
                sh'''
                    sudo ./build_pkg.sh
                '''
            }
        }
        catch(e){
            currentBuild.result = 'FAIL'
            result = "FAIL"
        }
        archiveArtifacts allowEmptyArchive: true, artifacts: 'dtu_prometheus_monitor/dtu_monitor*.deb'
    }
}

def copySampleFile(String path, String dtu_model_ver, String dtu_model_ref){
    dir('./'){
        def target_path="$path/sample_test"
        def op_path="$target_path/op"
        def dtu_models_path="$target_path/dtu_models"
        def distributed_test_path="$target_path/distributed"
        sh """#!/bin/bash
            rm -rf $target_path
            mkdir -p $op_path
            mkdir -p $distributed_test_path

            while IFS= read -r line; do
                line_split=( \$line )
                origin_file=\${line_split[0]}
                target_file=\${line_split[1]}
                file="tensorflow/tensorflow/compiler/plugin/dtu_backend/tests/\$origin_file"
                cp -r \$file $op_path/\$target_file
            done < infra/release/sample_test_list.txt
        """
        // dtu_models part, currently just hardcode clone the repo, and remove git history
        withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
            withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                sh """
                    cd $target_path
                    git clone -b $dtu_model_ver ssh://jenkins@gerrit.enflame.cn:29418/dtu_models
                """
                if (dtu_model_ref) {
                    sh """
                        cd $target_path/dtu_models
                        git fetch ssh://jenkins@gerrit.enflame.cn:29418/dtu_models $dtu_model_ref && git cherry-pick FETCH_HEAD
                    """
                }
                sh """
                    cd $target_path/dtu_models
                    rm -rf .git
                """
            }
        }

        // down, copy environment variable files
        sh """#!/bin/bash
            while IFS= read -r line; do
                line_split=( \$line )
                origin_file=\${line_split[0]}
                target_file=\${line_split[1]}
                file="infra/jenkins_tools/\$origin_file"
                cp -r \$file $target_path/\$target_file
            done < infra/release/env_var_shell_list.txt
        """
        // copy other files
        sh """#!/bin/bash
            while IFS= read -r line; do
                line_split=( \$line )
                origin_file=\${line_split[0]}
                target_file=\${line_split[1]}
                file="\$origin_file"
                cp -r \$file $target_path/\$target_file
            done < infra/release/other_files_list.txt
        """

    }
}

def downloadDockerFile(String path){
    dir("$path"){
        // TODO: remove kmd_util download from here!
        // this is a very temparary solution to put kmd_util.sh into the release package
        sh """
            cp ../dtu_kmd/scripts/install_drv.sh ./
        """
        sh """
            mkdir ubuntu_amd64_16.04_tf_1.14_py2
            cd ubuntu_amd64_16.04_tf_1.14_py2
            wget http://git.enflame.cn/mingting.sun/docker_files/raw/master/ubuntu_amd64_16.04_tf_1.14_py2/Dockerfile
        """
    }
}

def downloadArtifact(String path, String projectName, String buildId, String fileName){
    dir("$path"){
        copyArtifacts filter: "$fileName", flatten: true, fingerprintArtifacts: true, projectName: "$projectName", selector: specific("$buildId")
    }
}

def tarAndArtifact(String path){
    dir('./')
    {
        sh """
            tar -czf ${path}.tar.gz ${path}
        """
        archiveArtifacts artifacts: 'enflame*.tar.gz', fingerprint: true
    }
}

def generatePackage(String version, String projectName, String buildId, String dtu_model_ver="1.1", String dtu_model_ref=""){
    try{
        stage('--generate release package--'){
            dir('./'){
                def tar_path_binary="enflame_binary.$version"
                sh """#!/bin/bash
                    rm -rf ${tar_path_binary}
                    mkdir ${tar_path_binary}
                """
                copySampleFile(tar_path_binary, dtu_model_ver, dtu_model_ref)
                downloadDockerFile(tar_path_binary)
                // download built artifacts
                downloadArtifact(tar_path_binary, projectName, buildId, '**/dtu-*.rpm,**/dtu-*.deb,dtu_sdk*.deb,tensorflow-1.14.0-*.*.whl,**/dtupp*.deb,**/eccl*.deb,**/horovod*.whl,**/dtu_monitor*.deb')
                tarAndArtifact(tar_path_binary)
            }
        }
    }//try
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

return this //required by groovy rule, don't delete.
