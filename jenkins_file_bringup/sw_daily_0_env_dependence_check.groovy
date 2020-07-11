stage('--clean ws--'){
    dir('./'){
        cleanWs()
        withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
            withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                sh"git clone ssh://jenkins@gerrit.enflame.cn:29418/benchmark"
            }
        }
    }
}
stage('--dependence check--'){
    dir('./benchmark'){
        try{
            sh"""#!/bin/bash
                git checkout develop
                tag_old=`git describe`
                git diff develop \$tag_old --stat >log.txt
                grep  -c 'requirements.txt' log.txt
                if [ \$? -eq '0' ]; then
                    echo 'new module need to install for benchmark'
                    grep 'requirements.txt' log.txt >log_1.txt
                    cat log_1.txt | awk -F " " '{print \$1}' >diff.txt
                    ansible –version
                    cat /etc/ansible/hosts
                    host=(beijing_build_servers ubuntu_in_shanghai sse_lab_inspur shanghai_build_machines A1_D)
                    for line in `cat diff.txt`;
                    do
                        echo "pip2 install -r \$line"
                        for host_mc in \${host[@]};
                        do
                            ansible \$host_mc -m copy -a "src=${env.WORKSPACE}/benchmark/\$line dest=/home/jenkins/"
                            ansible \$host_mc -m command -a "sudo pip2 install -r /home/jenkins/\$line"
                        done
                    done
                fi
            """
            dep_change=sh(script: "cat diff.txt",returnStdout: true).trim()
            currentBuild.description+="<br>dependece:${dep_change}"
            emailext (
                subject: "benchmark env dependence need update",
                body: """
                        <p>hi,xaingfeng,junzhe,renhuan</p>
                        <p>benchmark env dependence need update,please help to updatde in docekr file and zebu</p>
                        <p>Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                        <p>Check benchmark env dependence change in job description </p>""",
                to: "sw_qa@enflame-tech.com"
            )
        }
        catch(e){
            echo 'no new module need to install for benchmark or some other error'
            emailext (
                subject: "benchmark env dependence need update",
                body: """
                        <p>hi,Rongrong</p>
                        <p>benchmark env dependence  no need update or some other error occured,plese check</p>
                        <p>Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>""",
                to: "cherry.chen@enflame-tech.com"
            )
        }
        finally{
            println("test pass")
        }
    }
}