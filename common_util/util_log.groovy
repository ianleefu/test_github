def get_log(){
    dir('./infra'){
        sh'''
            ./get_log_all.sh > log_path.txt 2>&1
        '''
        log_path=sh(script: "grep IP log_path.txt", returnStdout: true).trim()
        currentBuild.description+="<br>${log_path}"
        archiveArtifacts allowEmptyArchive: true, artifacts: 'log_path.txt'
    }
}

def mock_log_parse(Boolean is_op_test=false){
    try{
        stage('mock log parse'){
            dir('./infra/jenkins_tools/'){
                if (!is_op_test){
                    sh'''#!/bin/bash
                        cp $WORKSPACE/benchmark/enflame_model_test/logs/training*.log ./
                        python $WORKSPACE/infra/jenkins_tools/parser_log_mock.py -f training*.log
                        if [ \$? -eq 0 ];then
                            echo "mock log parse sucessfully"
                        else
                            echo "mock log parse failed"
                            exit 1
                        fi
                    '''
                }
                else{
                    sh'''#!/bin/bash
                        cp $WORKSPACE/tensorflow/*.zip ./mock_parse_result/
                        mv $WORKSPACE/tensorflow/*.zip ./
                        unzip ./*.zip
                        op_mock_parse_result=0
                        for file in $(find . -name "*.txt")
                        do
                            python $WORKSPACE/infra/jenkins_tools/parser_log_mock.py -f $file
                            if [ \$? -eq 0 ];then
                                echo "mock log parse sucessfully"
                            else
                                echo "$file mock log parse failed"
                                op_mock_parse_result=1
                            fi
                        done
                        if [ \${op_mock_parse_result} -ne 0 ]; then
                            exit 1
                        fi
                    '''
                }
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./infra/jenkins_tools/'){
            sh'''#!/bin/bash
                time=$(date "+%Y%m%d%H%M%S")
                zip ./mock_parse_result/mock_parse_${time}.zip ./*.result.log || true
                rm *.txt || true
                rm *.log || true
                rm *.zip || true
            '''
        }
    }
}

def performance_check(){
    try{
        stage('performance_check'){
            if (GERRIT_PROJECT == "dtu_backend"){
                jira_path="$WORKSPACE/tensorflow/tensorflow/compiler/plugin/dtu_backend"
            }
            else if (GERRIT_PROJECT == "tensorflow_114"){
                jira_path="$WORKSPACE/tensorflow"
            }
            else{
                jira_path="$WORKSPACE/$GERRIT_PROJECT"
            }
            dir(jira_path){
                if (currentBuild.result=='FAILURE'){
                    echo "training failed, won't check performance"
                }
                else{
                    sh'''#!/bin/bash
                        jira_id=`git log --format=oneline -1 | grep -P '\\w+-\\d+' -o`
                        cd $WORKSPACE/infra/jenkins_tools
                        python performance_check.py $jira_id $GERRIT_CHANGE_URL
                    '''
                }
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def profiler_check(){
    try{
        stage('profiler_downgrade_check'){
            sh"mv ./infra/jenkins_tools/performance_profiler_check.py ./benchmark/enflame_model_test"
            dir("./benchmark/enflame_model_test"){
                sh'''#!/bin/bash
                    python performance_profiler_check.py log fps
                    if [ $? -eq 0 ]; then
                        echo "Profiler performance drop within reasonable range"
                    else
                        echo "Profiler performance drop excceeds 15 percents"
                        exit 1
                    fi
                '''
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def convergence_check(String base_event){
    try{
        stage('convergence_auto-detect'){
            dir("./benchmark/enflame_model_test/models"){
                sh"""#!/bin/bash
                    git clone http://git.enflame.cn/heng.shi/converge_event_checker.git
                    file_path=`pwd`/tensorboard/`ls tensorboard/`
                    cd converge_event_checker
                    python converge_checker.py -c=0.9 -a=90 -fd=\$file_path -fg=./baselines/$base_event/
                    if [ \$? -eq 0 ];then
                        echo "converge sucessfully"
                    else
                        echo "converge failed"
                        exit 1
                    fi
                """
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def sv_performamce_check(String sv_layout){
    try{
        stage('sv performance check'){
            dir("./infra/jenkins_tools"){
                sh"""#!/bin/bash
                    path="${WORKSPACE}/benchmark/rnn_test/hok/${sv_layout}/logs"
                    if [ $sv_layout == 'sv' ];then
                        python sv_performance_check.py --path \$path --build_id $BUILD_ID --asic_config 1c8s --node $NODE_NAME --upload_database $upload_database --job_name $JOB_NAME
                    else
                        python sv_performance_check.py --path \$path --build_id $BUILD_ID --asic_config 4c32s --node $NODE_NAME --upload_database $upload_database --job_name $JOB_NAME
                    fi
                    exit \$?
                """
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def get_fps_result(String wkspace){
    dir('./'){
        fps_result=sh(script: "python $wkspace/infra/jenkins_tools/get_fps_statistics.py $wkspace",returnStdout: true).trim()
        currentBuild.description+="<br>fps_result:${fps_result}"
    }
}

def get_submit_time(String commit_id){
    dir('./infra/01_auto_report'){
        submit_time=sh(script: "python3 get_submit_time.py ${commit_id}",returnStdout: true).trim()
        currentBuild.description+="<br>submit_time:${submit_time}"
    }
    return submit_time
}

return this //required by groovy rule, don't delete.
