//use manifest file to get repos
def get_repo(String manifest){
    stage('--get repo---'){
        dir('./'){
            sh"""
                repo init -u git@git.enflame.cn:sw/manifest.git -m ${manifest} --repo-url http://git.enflame.cn/licheng.xu/git-repo.git --repo-branch master --no-repo-verify
                repo sync --force-sync
            """
        }
    }
}

//use manifest file to get repos
def get_repo_bj(String manifest){
    stage('--get repo---'){
        dir('./'){
            sh"""
                repo init -u git@gitbj.enflame.cn:sw/manifest.git -m ${manifest} --repo-url http://git.enflame.cn/licheng.xu/git-repo.git --repo-branch master --no-repo-verify
                repo sync --force-sync
            """
        }
    }
}

def update_repo_by_gerrit(){
    stage('--update_'+"${env.GERRIT_PROJECT}"+'_by_gerrit--'){
        withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
            withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                if("${env.GERRIT_PROJECT}"=="dtu_backend"){
                    dir("${WORKSPACE}/tensorflow/tensorflow/compiler/plugin/dtu_backend"){
                        sh"git pull ssh://jenkins@172.16.11.42:29418/${GERRIT_PROJECT} ${GERRIT_REFSPEC}"
                    }
                }
                else if("${env.GERRIT_PROJECT}"=="tensorflow_114"){
                    dir("${WORKSPACE}/tensorflow"){
                        sh"git pull ssh://jenkins@172.16.11.42:29418/${GERRIT_PROJECT} ${GERRIT_REFSPEC}"
                    }
                }
                else{
                    dir("${WORKSPACE}/${GERRIT_PROJECT}"){
                        sh"git pull ssh://jenkins@172.16.11.42:29418/${GERRIT_PROJECT} ${GERRIT_REFSPEC}"
                    }
                }
            }
        }
    }
}

def checkout_by_commit(String repo_name, String commit_id){
    /**
     * @description:    checkout code by param commit_id
     * @param           repo_name: String, repository name
                        commit_id: String, commit id
     * @return:
     */
    if(commit_id){
        stage('--checkout by commit--'){
            switch(repo_name){
                case "dtu_backend":
                    path="${WORKSPACE}/tensorflow/tensorflow/compiler/plugin/dtu_backend"
                    break;
                case "tensorflow_114":
                    path="tensorflow"
                    break;
                default:
                    path=repo_name
                    break;
            }
            dir(path){
                sh"""
                    git fetch
                    git checkout ${commit_id}
                """
            }
        }
    }
    currentBuild.description+="<br>${repo_name}: ${commit_id}"
}

def get_specify_time_commit_id(String repo_name, String date){
    /**
     * @description:    get commit ID before the specified time, also contain the specified time
     * @param           repo_name:  String, repository name
                        date:       String, commit id
     * @return:         commit id
     */
    stage('--get commit--'){
        switch(repo_name){
            case "dtu_backend":
                path="${WORKSPACE}/tensorflow/tensorflow/compiler/plugin/dtu_backend"
                break;
            case "tensorflow_114":
                path="tensorflow"
                break;
            default:
                path=repo_name
                break;
        }
        dir(path){
            commit_id=sh(script: "git log --before='${date}' --pretty=format:'%H' | head -1", returnStdout: true).trim()
        }
    }
    return commit_id
}

def fetch_change_patch(String repo_name, String ref_id){
    if(ref_id){
        stage('--fetch ref--'){
            withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
                withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                    if(repo_name=="dtu_backend"){
                        dir("${WORKSPACE}/tensorflow/tensorflow/compiler/plugin/dtu_backend"){
                            sh"git fetch ssh://jenkins@172.16.11.42:29418/${repo_name} ${ref_id} && git cherry-pick FETCH_HEAD"
                        }
                    }
                    else if(repo_name=="tensorflow_114"){
                        dir('tensorflow'){
                            sh"git fetch ssh://jenkins@172.16.11.42:29418/${repo_name} ${ref_id} && git cherry-pick FETCH_HEAD"
                        }
                    }
                    else{
                        dir(repo_name){
                            sh"git fetch ssh://jenkins@172.16.11.42:29418/${repo_name} ${ref_id} && git cherry-pick FETCH_HEAD"
                        }
                    }
                }
            }
        }
    }
}

def checkout_by_commit_eccl(String commit){
    stage('--get eccl source--'){
        withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
            withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                sh """
                    git clone -b $commit ssh://jenkins@gerrit.enflame.cn:29418/eccl
                """
            }
        }
    }
}

def pull_change_patch(String repo_name, String ref_id){
    if(ref_id){
        stage('--pull ref--'){
            withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
                withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                    if(repo_name=="dtu_backend"){
                        dir("${WORKSPACE}/tensorflow/tensorflow/compiler/plugin/dtu_backend"){
                            sh"git pull ssh://jenkins@172.16.11.42:29418/${repo_name} ${ref_id}"
                        }
                    }
                    else if(repo_name=="tensorflow_114"){
                        dir('tensorflow'){
                            sh"git pull ssh://jenkins@172.16.11.42:29418/${repo_name} ${ref_id}"
                        }
                    }
                    else{
                        dir(repo_name){
                            sh"git pull ssh://jenkins@172.16.11.42:29418/${repo_name} ${ref_id}"
                        }
                    }
                }
            }
        }
    }
}


def main_commit_get(){
    stage('--main commit get--'){
        if (fileExists('./benchmark')){
            dir('./benchmark'){
                benchmark=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>benchmark_commit: ${benchmark}"
            }
        }
        if (fileExists('./tensorflow')){
            dir('./tensorflow'){
                tensorflow=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>tensorflow: ${tensorflow}"
            }
        }
        if (fileExists('./tensorflow/tensorflow/compiler/plugin/dtu_backend')){
            dir('./tensorflow/tensorflow/compiler/plugin/dtu_backend'){
                dtu_backend=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>dtu_backend: ${dtu_backend}"
            }
        }
        if (fileExists('./dtu_kmd')){
            dir('./dtu_kmd'){
                dtu_kmd=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>dtu_kmd: ${dtu_kmd}"
            }
        }
        if (fileExists('./dtu_sdk')){
            dir('./dtu_sdk'){
                dtu_sdk=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>dtu_sdk: ${dtu_sdk}"
            }
        }
        if (fileExists('./eccl')){
            dir('./eccl'){
                eccl=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>eccl: ${eccl}"
            }
        }
        dir('./'){
            sh'repo forall -c "pwd;git rev-parse HEAD" > repo_commits.txt'
            archiveArtifacts artifacts: 'repo_commits.txt', fingerprint: true
        }
    }
}

def individual_commit_get(String repo_name){
    stage("--$repo_name commit get--"){
        if (fileExists("./$repo_name")){
            dir("./$repo_name"){
                commit_num=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                currentBuild.description+="<br>$repo_name commit: ${commit_num}"
            }
        }
    }
}

def load_wiwigaga(){
    stage('--wiwigaga--'){
        if (NODE_NAME =~ 'sse_lab_Dell_..._.'){
            sh"""
                rmmod leo || true
                rmmod dtu || true
                rmmod dtu_kcl || true
            """
            dtu_number=dtu_number// pass variable through nodes
            node(host_node){     // host_node is a variable stored on the virtual machine node
                dir('/root/efvs'){
                    sh"./efsmt -pcie reset hot -d ${dtu_number}"
                }
            }
        }
        else{
            dir('/root/efvs'){
                sh"""
                    rmmod leo || true
                    rmmod dtu || true
                    rmmod dtu_kcl || true
                    ./efsmt -pcie reset hot
                """
                switch("${env.NODE_NAME}"){
                    case 'sse_lab_CI_001':
                        break;
                    case 'sse_lab_CI_004':
                        break;
                    case 'sse_lab_CI_009':
                        break;
                    case 'sse_lab_CI_010':
                        break;
                    case 'sse_lab_Dell_003':
                        break;
                    case 'sse_lab_Dell_001':
                        sh"./wiwigaga5.sh 0.95 1.1 1100 1000"
                        break;
                    default:
                        sh"./wiwigaga5.sh ${wwgg}"
                        break;
                }
            }
        }
    }//stage
}

def load_wiwigaga_distributed(){
    stage('--wiwigaga--'){
        dir('/root/efvs'){
            sh"""
                rmmod leo || true
                rmmod dtu || true
                rmmod dtu_kcl || true
                ./efsmt -pcie reset hot -d dtu.*
                ./wiwigaga5.sh ${wwgg} 0
                ./wiwigaga5.sh ${wwgg} 1
                ./wiwigaga5.sh ${wwgg} 2
                ./wiwigaga5.sh ${wwgg} 3
                ./wiwigaga5.sh ${wwgg} 4
                ./wiwigaga5.sh ${wwgg} 5
                ./wiwigaga5.sh ${wwgg} 6
                ./wiwigaga5.sh ${wwgg} 7
            """
        }
    }//stage
}



def bazel_clean(String path){
    stage('--bazel clean--'){
        dir("./$path"){
            sh"bazelisk clean --expunge"
        }
    }
}

// to fix TypeError bug in xmlRunner on python2.7
def update_xmlRunner(){
    stage('--update result.py--'){
        dir('./'){
            sh"cp ./infra/02_infra_maintain/result.py /usr/local/lib/python2.7/dist-packages/xmlrunner"
        }
    }//stage
}

def check_or_load_dataset(String wkspace, String dataset="imagenet2"){
    stage('--check or load dataset --'){
        dir('./benchmark'){
            sh"""#!/bin/bash
                if [ $dataset == 'voc2007' ];then
                    final_path="$wkspace/benchmark/enflame_object_detection"
                    python $WORKSPACE/infra/jenkins_tools/load_imagenet_dataset.py --dataset_name ${dataset} --final_path \${final_path}
                else
                    final_path="$wkspace/benchmark/enflame_model_test"
                    python $WORKSPACE/infra/jenkins_tools/load_imagenet_dataset.py --dataset_name ${dataset} --final_path \${final_path}
                fi
            """
        }
    }
}

def git_tag(String tag_name, String tag_msg){
    stage('--git tag--'){
        dir('./'){
            sh"""
                # get repo name, tagging in turn
                for line in `ls -F | grep "\$"`
                do
                    cd $WORKSPACE/\${line}
                    git tag -a ${tag_name} -m "${tag_msg}"
                    git push origin ${tag_name}
                done

            """
        }
    }
}

def git_clone(String user_name, String repo_name, String branch_name){
    stage('--git clone--'){
        dir('./'){
            sh"git clone ssh://${user_name}@gerrit.enflame.cn:29418/${repo_name} -b ${branch_name}"
        }
    }
}

def update_sv_steps(String max_steps){
    stage("change sv training steps"){
        sh'''#!/bin/bash
            cd ${WORKSPACE}/benchmark/rnn_test/hok/sv
            sed -i 's#"max_steps": 10#"max_steps": '$max_steps'#g' job_param.json
            cd ${WORKSPACE}/benchmark/rnn_test/hok/sv_4c
            sed -i 's#"max_steps": 20#"max_steps": '$max_steps'#g' job_param.json
            echo "change done"
        '''
    }
}


return this //required by groovy rule, don't delete.
