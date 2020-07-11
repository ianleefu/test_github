def resnet_train_test(batch_size="4", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2", python_ver="2",String repeat_time="1"){
    //model_name in [resnet, alexnet, googlenet, vgg, inception_v2/v3/v4, resnet_cifar,resnet_official,resnet14, mnist]
    //dataset in [imagenet2, imagenet, imagenet10]
    repeat_time=repeat_time.toInteger()
    stage('--train test--'){
        dir('./benchmark/enflame_model_test'){
            // temporary workaround for LEOSW-5230, need to remove after formal fix
            sh"sudo pip2 install -r requirements.txt"
            if (model_name=="vgg"){
                sh '''
                    sed -i "s#use_resource=True#use_resource=False#g" run.sh
                '''
            }
            while(repeat_time > 0) {
                sh"""#!/bin/bash
                    echo "Current_repeat: $repeat_time"
                    source $WORKSPACE/infra/jenkins_tools/$training_test_env
                    env
                    echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset --python_ver $python_ver
                    bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset --python_ver $python_ver
                    if [ \$? -eq 0 ]; then
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test succeed
                    else
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test failed
                        exit 1
                    fi
                """
                repeat_time=repeat_time-1
            }
            if (model_name=="vgg"){
                sh '''
                    sed -i "s#use_resource=False#use_resource=True#g" run.sh
                '''
            }
        }
    }
}

def object_detection_train_test(batch_size="4", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="yolo_v2", dataset="voc2007", python_ver="2",String repeat_time="1"){
    //model_name in [yolo_v2, yolo_v3]
    //dataset in [voc2007，voc2012]
    repeat_time=repeat_time.toInteger()
    stage('--train test--'){
        dir('./benchmark/enflame_object_detection'){
            while(repeat_time > 0) {
                sh"""#!/bin/bash
                    sudo pip2 install -r requirements.txt
                    echo "Current_repeat: $repeat_time"
                    source $WORKSPACE/infra/jenkins_tools/$training_test_env
                    env
                    echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset --python_ver $python_ver
                    bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset --python_ver $python_ver
                    if [ \$? -eq 0 ]; then
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test succeed
                    else
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test failed
                        exit 1
                    fi
                """
                repeat_time=repeat_time-1
            }
        }
    }
}

def minibenchmark_test(batch_size="32", epoch="4", dtype="bf16", data_format="CHNW", python_ver="2", String training_test_env, device="dtu", net_size="128", String repeat_time="1"){
    repeat_time=repeat_time.toInteger()
    stage('--train test--'){
        dir('./benchmark/minibenchmark'){
            while(repeat_time > 0) {
                sh"""#!/bin/bash
                    echo "Current_repeat: $repeat_time"
                    source $WORKSPACE/infra/jenkins_tools/$training_test_env
                    env
                    echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch  --dtype $dtype --data_format $data_format --python_ver $python_ver --device $device --net_size $net_size
                    bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format  --python_ver $python_ver --device $device --net_size $net_size
                    if [ \$? -eq 0 ]; then
                        echo minibenchmark bs$batch_size epoch$epoch  $data_format $dtype test succeed
                    else
                        echo minibenchmark bs$batch_size epoch$epoch  $data_format $dtype test failed
                        exit 1
                    fi
                """
                repeat_time=repeat_time-1
            }
        }
    }
}

def convergence_regression_test(batch_size="4", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2"){
    //model_name in [resnet, alexnet, googlenet, vgg, inception_v2/v3/v4, resnet_cifar,resnet_official,resnet14, mnist]
    //dataset in [imagenet2, imagenet, imagenet10]
    try{
        stage('resnet training'){
            dir('./benchmark/enflame_model_test'){
                unstash 'tf_pkg'
                unstash 'sdk_deb'
                sh"""#!/bin/bash
                    sudo pip2 install -r requirements.txt
                    dpkg -r dtu_sdk || true
                    dpkg -i dtu_sdk/dtu_sdk*.deb
                    python -m pip uninstall --yes tensorflow/tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl || true
                    python -m pip uninstall --yes tensorflow/tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl || true
                    python -m pip install tensorflow/tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl

                    source $WORKSPACE/infra/jenkins_tools/$training_test_env
                    env
                    echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    if [ \$? -eq 0 ]; then
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test succeed
                    else
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test failed
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

def resnet_train_test_zebu(batch_size="4", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2"){
    //model_name in [resnet, alexnet, googlenet, vgg, inception_v2/v3/v4, resnet_cifar,resnet_official,resnet14, mnist]
    //dataset in [imagenet2, imagenet, imagenet10]
    try{
        stage('resnet training'){
            dir('./benchmark/enflame_model_test'){
                unstash 'tf_pkg'
                unstash 'sdk_deb'
                sh"""#!/bin/bash
                    sudo pip2 install -r requirements.txt
                    dpkg -r dtu_sdk || true
                    dpkg -i dtu_sdk/dtu_sdk*.deb
                    python -m pip uninstall --yes tensorflow/tf_pkg/tensorflow-1.8.0rc0-cp27-cp27mu-linux_x86_64.whl || true
                    python -m pip uninstall --yes tensorflow/tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl || true
                    python -m pip install tensorflow/tf_pkg/tensorflow-1.14.0-cp27-cp27mu-linux_x86_64.whl

                    export build_id=$BUILD_URL
                    export EF_PLATFORM_ID=LEO
                    source $WORKSPACE/infra/jenkins_tools/$training_test_env
                    env
                    echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    if [ \$? -eq 0 ]; then
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test succeed
                    else
                        echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test failed
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

def mock_resnet_train_test(batch_size="32", epoch="1", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2"){
    //model_name in [resnet, alexnet, googlenet, vgg, inception_v2/v3/v4, resnet_cifar,resnet_official,resnet14, mnist]
    //dataset in [imagenet2, imagenet, imagenet10]
    stage('--mock resnet 50 training test--'){
        dir('./benchmark/enflame_model_test'){
            // run resnet training
            sh """#!/bin/bash
                sudo pip2 install -r requirements.txt
                set -e
                # mock test environment variable
                source $WORKSPACE/infra/jenkins_tools/$training_test_env

                echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                if [ \$? -eq 0 ]; then
                    echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype mock test succeed
                else
                    echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype mock test failed
                    exit 1
                fi
            """
        }
    }//stage
}

def distribute_resnet_test(batch_size="64", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2", String hostfile, np="np"){
    stage('--distribute train test--'){
        dir('./benchmark/enflame_model_test'){
            sh"pip2 install -r requirements.txt"
            if( "${np}" == "8" ){
                sh"""#!/bin/bash
                    cp ../../infra/jenkins_tools/distribute_8card_resnet_test.sh ./
                    echo [test_command]: distribute_8card_resnet_test.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    mpirun -np 8 --allow-run-as-root bash -x ./distribute_8card_resnet_test.sh
                """
            }//if
            else{// only support docker now, as docker network port is 2223
                sh"""#!/bin/bash
                    cp ../../infra/jenkins_tools/distribute_8card_resnet_test.sh ./
                    echo $hostfile > hostfile
                    echo [test_command]: distribute_8card_resnet_test.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    mpirun -hostfile hostfile -np $np --allow-run-as-root -mca plm_rsh_args "-p 2223" -mca btl_tcp_if_exclude docker0 ./distribute_8card_resnet_test.sh
                """
            }
        }
    }//stage
}

def distribute_X2_test(String training_test_env, x2_steps="20", String hostfile, np="np"){
    stage('--distribute train test--'){
        dir("./"){
            sh"""#!/bin/bash
                sed -i 's/"max_steps": 20/"max_steps": "$x2_steps"/g' ./benchmark/rnn_test/hok/sv_horovod_4c/job_param.json
            """
        }
        dir('./benchmark/rnn_test/hok/sv_horovod_4c/'){
            if( "${np}" == "8" ){
                sh"""#!/bin/bash
                    cp ../../../../infra/jenkins_tools/distribute_8card_X2_test.sh ./
                    echo [test_command]: distribute_8card_X2_test.sh
                    mpirun -np 8 --allow-run-as-root bash -x ./distribute_8card_X2_test.sh
                """
            }//if
            else{// only support docker now, as docker network port is 2223
                sh"""#!/bin/bash
                    cp ../../infra/jenkins_tools/distribute_8card_resnet_test.sh ./
                    echo $hostfile > hostfile
                    echo [test_command]: distribute_8card_resnet_test.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    mpirun -hostfile hostfile -np $np --allow-run-as-root -mca plm_rsh_args "-p 2223" -mca btl_tcp_if_exclude docker0 ./distribute_8card_X2_test.sh
                """
            }
        }
    }//stage
}

def resnet_assign_card_test(batch_size="64", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2", cardNum="1", cardId="0"){
    stage('--assign card train test--'){
        dir('./benchmark/enflame_model_test'){
            sh"""#!/bin/bash
                sudo pip2 install -r requirements.txt
                if [ $cardNum -eq 1 ];then
                    source ${WORKSPACE}/infra/jenkins_tools/${training_test_env} $cardId
                    env
                    echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                else
                    for $cardId in {0,7}
                    do
                        source ${WORKSPACE}/infra/jenkins_tools/${training_test_env} ${cardId}
                        env
                        echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                        bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                    done
                fi

                if [ \$? -eq 0 ]; then
                    echo $model_name bs$batch_size epoch$epoch step=$step $data_format $dtype test succeed
                else
                    echo $model_name bs$batch_size epoch$epoch step=$step $data_format $dtype test failed
                    exit 1
                fi
            """
        }
    }
}


def profiler_test(batch_size="32", epoch="4", step="0", dtype="bf16", data_format="CHNW", String training_test_env, model_name="resnet", dataset="imagenet2"){
    //model_name in [resnet, alexnet, googlenet, vgg, inception_v2/v3/v4, resnet_cifar,resnet_official,resnet14, mnist]
    //dataset in [imagenet2, imagenet, imagenet10]
    stage('--proflier test--'){
        dir('benchmark/enflame_model_test'){
            sh"""#!/bin/bash
                sudo pip2 install -r requirements.txt
                source ${WORKSPACE}/infra/jenkins_tools/$training_test_env

                #enable profiler
                #old method, remove when everthing changes to dynamic profiler
                export DTU_PROFILER_FLAGS="--do_dtu_profile=1"
                #new method, add || true for use cases where dtu_pp is not installed
                dtuprofctrl --start || true

                echo [test_command]: run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                bash -x ./run.sh --batch_size $batch_size --epoch $epoch --step $step --dtype $dtype --data_format $data_format --model $model_name --dataset $dataset
                if [ \$? -eq 0 ]; then
                    echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test succeed
                else
                    echo $model_name bs$batch_size epoch$epoch step$step $data_format $dtype test failed
                    exit 1
                fi

                if [ -f libprofile.data ]; then
                    echo "find libprofile.data"
                else
                    echo "failed to find libprofile.data"
                    exit 1
                fi

                #disable profiler
                dtuprofctrl --stop || true
            """
        }
    }
}

def xla_test(String xla_test_env){
    try{
        stage('--xla test--'){
            dir('./tensorflow'){
                sh"""#!/bin/bash
                    source ${WORKSPACE}/infra/jenkins_tools/${xla_test_env}
                    env

                    bazel build  -- //tensorflow/compiler/xla/tests:all || exit 1
                    rm -rf ./bazel-testlogs/tensorflow/compiler/xla || true

                    filter="--gtest_filter=-"
                    for line in `cat tensorflow/compiler/plugin/dtu_backend/tests/error_cases.txt`
                    do
                        filter=\${filter}\${line}':'
                    done

                    bazel test  --cache_test_results=no --local_test_jobs=1 --test_arg="\${filter}"                               \
                                --test_env="ASSIGN_CDMA_PASS=true"                                                                \
                                -- //tensorflow/compiler/xla/tests:all                                                            \
                                    -//tensorflow/compiler/xla/tests:slice_test_dtu                                               \
                                    -//tensorflow/compiler/xla/tests:while_test_dtu                                               \
                                    -//tensorflow/compiler/xla/tests:dot_operation_single_threaded_runtime_test_dtu               \
                                    -//tensorflow/compiler/xla/tests:params_test_dtu                                              \
                                    -//tensorflow/compiler/xla/tests:dot_operation_test_dtu                                       \
                                    -//tensorflow/compiler/xla/tests:broadcast_simple_test_dtu                                    \
                                    -//tensorflow/compiler/xla/tests:dynamic_ops_test_dtu                                         \
                                    -//tensorflow/compiler/xla/tests:half_test_dtu                                                \
                                    -//tensorflow/compiler/xla/tests:reverse_test_dtu                                             \
                                    -//tensorflow/compiler/xla/tests:transfer_manager_test_dtu                                    \
                                    -//tensorflow/compiler/xla/tests:vector_ops_simple_test_dtu                                   \
                                    -//tensorflow/compiler/xla/tests:transpose_test_dtu                                           \
                                    -//tensorflow/compiler/xla/tests:reduce_window_test_dtu || exit 1
                   """
            }
        }
    }//try
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./tensorflow'){
            sh"python ${WORKSPACE}/infra/jenkins_tools/remove_notrun.py bazel-testlogs/tensorflow/compiler/xla/tests/ || true"
            junit healthScaleFactor: 0.0, allowEmptyResults: true, testResults: 'bazel-testlogs/**/xla/tests/**/*.xml'
        }
    }
}

def xla_test_tf114(String test_env=""){
    try{
        stage('--xla test--'){
            dir('./tensorflow'){
                if (test_env==""){
                    test_env="foo=bar"
                }
                sh"""#!/bin/bash
                    bazelisk build  -- //tensorflow/compiler/xla/tests:all || exit 1
                    rm -rf ./bazel-testlogs/tensorflow/compiler/xla || true

                    filter="--gtest_filter=-"
                    for line in `cat tensorflow/compiler/plugin/dtu_backend/tests/error_cases.txt`
                    do
                        filter=\${filter}\${line}':'
                    done

                    bazelisk test  --cache_test_results=no --local_test_jobs=1 --test_arg="\${filter}"                \
                                --test_env="$test_env" --test_env="DTU_OPT_MODE=false" --test_env="BYPASS_ODMA_WORKAROUND=false" \
                                -- //tensorflow/compiler/xla/tests:all                                                \
                                    -//tensorflow/compiler/xla/tests:while_test_dtu     || exit 1
                    bazelisk test  --cache_test_results=no --local_test_jobs=1  --test_arg="\${filter}"               \
                                --test_env="$test_env" --test_env="ENABLE_SDK_STREAM_CACHE=false"                    \
                                -- //tensorflow/compiler/xla/tests:while_test || exit 1
                """
            }
        }
    }//try
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./tensorflow'){
            sh"python ${WORKSPACE}/infra/jenkins_tools/remove_notrun.py bazel-testlogs/tensorflow/compiler/xla/tests/ || true"
            junit healthScaleFactor: 0.0, allowEmptyResults: true, testResults: 'bazel-testlogs/**/xla/tests/**/*.xml'
        }
    }
}

def compiler_tests_test(String test_case_list, String test_env){
    try{
        stage('--/compiler/tests/ test--'){
            dir('./tensorflow'){
                sh"""#!/bin/bash
                    compiler_test_result=0
                    bazelisk build  -- //tensorflow/compiler/tests:dynamic_slice_ops_test || exit 1
                    rm -rf ./bazel-testlogs/tensorflow/compiler/tests || true

                    for line in `cat ${WORKSPACE}/infra/test_case_list/$test_case_list`
                    do
                        {
                            echo \${line}' TESTING---'
                            bazelisk test  --cache_test_results=no --local_test_jobs=1     \
                                        --test_env="$test_env"                          \
                                        -- //tensorflow/compiler/tests:\${line}
                        } || {
                            echo \${line}' FAIL'
                            compiler_test_result=1
                            echo \${compiler_test_result}
                        }
                    done
                """
            }
        }
    }//try
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./tensorflow'){
            junit healthScaleFactor: 0.0, allowEmptyResults: true, testResults: 'bazel-testlogs/**/compiler/tests/**/*.xml'
        }
    }
}

def op_python_test(String op_test_env, String test_case_list, String python_ver='2'){
    try{
        stage('--op test--'){
            dir('./tensorflow'){
                python_exec='python'
                if (python_ver=='3'){
                    python_exec='python3'
                }
                echo "python version is $python_exec"
                sh"""#!/bin/bash
                    op_test_result=0
                    source ${WORKSPACE}/infra/jenkins_tools/$op_test_env
                    env

                    for line in `cat ${WORKSPACE}/infra/test_case_list/$test_case_list`
                    do
                        {
                            $python_exec ./tensorflow/compiler/plugin/dtu_backend/tests/\${line} &> \${line}.txt 2>&1
                        } || {
                            echo \${line}' FAIL'
                            op_test_result=1
                            echo FAIL >> ${WORKSPACE}/op_test_result.txt
                        }
                        echo \${line}' PASS'
                        echo PASS >> ${WORKSPACE}/op_test_result.txt
                    done
                    if [ \${op_test_result} -ne 0 ]; then
                        exit 1
                    fi
                """
            }
        }
    }//try
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./'){
            sh"""
                if [ ! -f "summary_of_op_python_result.txt" ]
                then
                    paste ${WORKSPACE}/infra/test_case_list/${test_case_list} ${WORKSPACE}/op_test_result.txt > ${WORKSPACE}/summary_of_op_python_result.txt
                else
                    paste ${WORKSPACE}/infra/test_case_list/${test_case_list} ${WORKSPACE}/op_test_result.txt >> ${WORKSPACE}/summary_of_op_python_result.txt
                    sed -i '/total:/d' summary_of_op_python_result.txt
                    sed -i '/pass:/d' summary_of_op_python_result.txt
                    sed -i '/fail:/d' summary_of_op_python_result.txt
                    sed -i '/not_run:/d' summary_of_op_python_result.txt
                fi
                total_count=`cat summary_of_op_python_result.txt | wc -l`
                pass_count=`awk -F : 'BEGIN {pass_count=0} /PASS/{pass_count++} END{print "",pass_count}' summary_of_op_python_result.txt`
                fail_count=`awk -F : 'BEGIN {fail_count=0} /FAIL/{fail_count++} END{print "",fail_count}' summary_of_op_python_result.txt`
                echo 'total:'\${total_count} >> summary_of_op_python_result.txt
                echo 'pass:'\${pass_count} >> summary_of_op_python_result.txt
                echo 'fail:'\${fail_count} >> summary_of_op_python_result.txt
                echo 'not_run:'`expr \${total_count} - \${pass_count} - \${fail_count}` >> summary_of_op_python_result.txt
                rm op_test_result.txt
            """
            archiveArtifacts allowEmptyArchive: true, artifacts: 'summary_of_op_python_result.txt'
        }
        dir('./tensorflow'){
            sh"""#!/bin/bash
                time=\$(date "+%Y%m%d%H%M%S")
                zip ${WORKSPACE}/tensorflow/op_python_test_log_\${time}.zip *.txt || true
                rm -rf *.py.txt || true
            """
        }
    }
}

def sdk_binary_test(String fail_case="false"){
    stage('--SDK sample build--'){
        dir('./dtu_sdk/doc'){
            sh'''#!/bin/bash
                set -o xtrace
                doxygen Doxyfile
            '''
        }
        dir('./dtu_sdk'){
            sh '''
                ###bazel clean --expunge
                bazelisk build //tests:all
                rm -rf ./bazel-testlogs/*
            '''
        }
    }
    try{
        stage('--sdk binary test--'){
            dir('./dtu_sdk'){
                sh '''#!/bin/bash
                    . ${WORKSPACE}/infra/test_case_list/sdk_test_case_list.sh
                    echo ${sdk_test_bin_pass_list_1c8s}
                    rm -rf logs_1c8s
                    mkdir logs_1c8s
                    for cc in ${sdk_test_bin_pass_list_1c8s}; do
                      ./bazel-bin/tests/${cc}  --gtest_output=xml:logs_1c8s/ > logs_1c8s/${cc}.log 2>&1
                    done
                '''
                if(fail_case=="true"){
                    sh'''#!/bin/bash
                        . ${WORKSPACE}/infra/test_case_list/sdk_test_case_list.sh
                        for cc in ${sdk_test_bin_fail_list_1c8s}; do
                              ./bazel-bin/tests/${cc}  --gtest_output=xml:logs_1c8s/ > logs_1c8s/${cc}.log 2>&1
                        done
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
        dir('./dtu_sdk'){
            junit allowEmptyResults: true, testResults: 'logs_1c8s/*.xml'
        }
    }
}

def sdk_bazel_24_test(String sdk_test_plan="LST-1353"){
    /**
    * @description:
            use bazelisk to build and run SDK test case, available to TF1.14 and TF1.8
            publish the junit test report
    * @param na
    * @return: currentBuild.result
    */
    try{
        stage('--sdk bazel test--'){
            dir('./dtu_sdk/doc'){
                sh'''#!/bin/bash
                    set -o xtrace
                    doxygen Doxyfile
                '''
            }
            dir('./dtu_sdk'){
                sh '''
                    bazelisk build //tests:dtu_tools_test || true
                    rm -rf ./bazel-testlogs/* || true
                    bazelisk test //tests:all --cache_test_results=no --local_test_jobs=1 --   \
                        -//tests:executor_performance_test                                     \
                        -//tests:command_packet_predictor_test                                 \
                        -//tests:factory_test

                    #sdk topsrider API tests
                    bazelisk test //tests/tops:all --cache_test_results=no --local_test_jobs=1
                '''
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./dtu_sdk'){
            junit allowEmptyResults: true, testResults: 'bazel-testlogs/**/*.xml'
            archiveArtifacts artifacts: 'bazel-testlogs/**/**/*.xml', defaultExcludes: false, fingerprint: true
            // step([$class: 'XrayImportBuilder',
            //         endpointName: '/junit',
            //         importFilePath: 'bazel-testlogs/**/*.xml',
            //         importToSameExecution: 'true',
            //         projectKey: 'LST',
            //         revision: '${sdk_Commit}',
            //         serverInstance: '5f72c330-fa39-4158-9a5f-c40abe2529f3',
            //         testPlanKey: sdk_test_plan])
        }
    }
}

def umd_sample_test(){
    try{
        stage('--umd sample build & test--'){
            if (fileExists('./dtu_umd')){
                dir('./dtu_umd'){
                    dtu_umd=sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    currentBuild.description+="<br>dtu_umd: ${dtu_umd}"
                }
            }
            dir('./dtu_umd'){
                sh '''
                    rm -rf umd_test_logs
                    mkdir umd_test_logs
                    bazelisk clean --expunge
                    bazelisk build //sample:vdk_test
                    ./bazel-bin/sample/vdk_test -a > umd_test_logs/vdk_test.log 2>&1
                '''
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./dtu_umd'){
            archiveArtifacts artifacts: 'umd_test_logs/*.log', fingerprint: true
        }
    }
}

def op_mock_test(){
    stage('tensorflow mock operator test'){
        dir('./tensorflow'){
        // environment variable is already set in previous stage
        // bazel test with mock, run both xla and dtu tests
            sh '''#!/bin/bash
                bazelisk test --test_timeout=5000 --define dtu_mock=true --cache_test_results=no --jobs 64 --local_test_jobs=8 -- tensorflow/compiler/plugin/dtu_backend/tests:all || true
            '''
            // junit healthScaleFactor: 0.0, allowEmptyResults: true, testResults: 'bazel-testlogs/**/xla/tests/**/*.xml'
            junit allowEmptyResults: true, testResults: 'bazel-testlogs/**/dtu_backend/tests/**/*.xml'
        }
    }
}

def transform_test(){
    // transform test has tow part: dtu_backend/tests/transform and dtu_sdk/tests/dsopt
    try{
        stage('transform test'){
            try{
                dir('./tensorflow'){
                    sh'''
                        bazelisk build tensorflow/compiler/plugin/dtu_backend/tests/transform:all
                        rm -rf ./bazel-testlogs/* || true
                        bazelisk test --local_test_jobs=1   \
                          --test_env="CLUSTER_AS_DEVICE=false"  \
                           -- //tensorflow/compiler/plugin/dtu_backend/tests/transform:all  \
                             -//tensorflow/compiler/plugin/dtu_backend/tests/transform:operator_validate_test
                    '''
                }
            }catch(e){
                currentBuild.result = 'FAIL'
                result = "FAIL"
            }
            dir('./dtu_sdk'){
                sh'''
                    bazelisk build //tests/dsopt:all
                    rm -rf ./bazel-testlogs/* || true
                    bazelisk test --local_test_jobs=1 //tests/dsopt:all
                '''
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./tensorflow'){
            junit allowEmptyResults: true, testResults: 'bazel-testlogs/**/plugin/dtu_backend/tests/transform/**/*.xml'
        }
        dir('./dtu_sdk'){
            junit allowEmptyResults: true, testResults: 'bazel-testlogs/tests/dsopt/**/*.xml'
        }
    }
}

def apd_test(String apd_layout){
    stage('--run apd test--'){
        dir('./benchmark/rnn_test/hok/static'){
            sh"""#!/bin/bash
                if [ $apd_layout == "1c8s" ]; then
                    source ${WORKSPACE}/infra/jenkins_tools/hok/env_1c.sh
                else
                    source ${WORKSPACE}/infra/jenkins_tools/hok/env_4c.sh
                fi
                mkdir log
                {
                    python apd_benchmark.py &> ./log/apd_benchmark.txt
                } || {
                    echo 'apd_benchmark.py test fail'
                    exit 1
                }
            """
        }
    }
}

def sv_model_test(String sv_layout, String repeat_time="1"){
    try{
        stage("$sv_layout test"){
            dir("./benchmark/rnn_test/hok/$sv_layout"){
                repeat_time=repeat_time.toInteger()
                error_flag=0
                while(repeat_time > 0) {
                    try{
                        sh"""#!/bin/bash
                            if [ $sv_layout == "sv" ]; then
                                source ${WORKSPACE}/infra/jenkins_tools/hok/sv_env_1c.sh
                            else
                                source ${WORKSPACE}/infra/jenkins_tools/hok/sv_env_4c.sh
                            fi
                            bash ./train.sh
                            if [ \$? -eq 0 ];then
                                echo "The $repeat_time test is done"
                                mv logs/2020*.log logs/${sv_layout}_${repeat_time}.log
                            else
                                echo "THe $repeat_time test failed"
                                mv logs/2020*.log logs/${sv_layout}_${repeat_time}.log
                                exit 1
                            fi
                        """
                    }
                    catch(e){
                        echo "sv test failed"
                        error_flag=1
                    }
                    repeat_time=repeat_time-1
                }
                echo "checking error flag......"
                if (error_flag==1){
                    error "sv test fail"
                }
                else{
                    echo "All good"
                }
            }
        }
    }
    catch(e){
        echo "model test failed at least once, please check log message for detail."
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def primo_test(String test_scope, String cqm_repeat_flag="false"){
    try{
        stage('Primo doxygen') {
            dir('./Primo') {
                sh'''#!/bin/bash
                    set -o xtrace
                    doxygen Doxyfile
                '''
            }
        }

        stage('Primo test'){
            dir('./Primo'){
                // build codes and check/install pytest lib
                sh'''#!/bin/bash
                    pip install pytest
                    bazelisk clean --expunge
                    bazelisk build tests:all
                    bazelisk build py_tests:all
                    rm -rf ./bazel-testlogs/* || true
                    echo "Test Scope: ${test_scope},  cqm_repeat: ${cqm_repeat_flag}"
                  '''
                if (test_scope == "all"){
                    sh'''
                        ./run_test.py  -m  ${cqm_repeat_flag}
                     '''}
                //tests-->C++ dir, py_tests-->python dir performance-->performance check
                else if (test_scope in ["tests", "py_tests", "performance"]){
                    sh'''
                        ./run_test.py -t ${test_scope} -m  ${cqm_repeat_flag}
                     '''}
                else {
                    echo "No matching test type found!!"
                }
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./Primo'){
            junit healthScaleFactor: 0.0, allowEmptyResults: true, testResults: 'bazel-testlogs/**/*.xml'
        }
    }
}

// primo sdk test only
def primo_sdk_test(){
    try{
        stage('Primo OP test'){
            dir('./tensorflow/tensorflow/compiler/plugin/dtu_backend/primo_tests'){
            //add op test in sdk
                sh"""#!/bin/bash
                    export CONFIG_PATH=${WORKSPACE}/dtu_sdk/lib/hlo
                    export XLA_FLAGS="--xla_backend_extra_options='xla_primo_cfg_dir=\$CONFIG_PATH,xla_dtu_mixed_precision=true'"
                    echo \$XLA_FLAGS
                    #bazelisk test //tensorflow/compiler/plugin/dtu_backend/primo_tests:all --local_test_jobs=1 --cache_test_results=no
                    op_test_result=0
                    time=\$(date "+%Y%m%d%H%M%S")
                    for file in `ls`
                    do
                        if [[ \$file =~ \\.py\$ ]];then
                            {
                                python \$file >\${file%.*}.log 2>&1
                            } || {
                               op_test_result=1
                            }
                            tail -n 50 \${file%.*}.log
                        fi
                    done
                    echo \${op_test_result}
                    zip ${WORKSPACE}/tensorflow/primo_sdk_test_log\${time}.zip *.log || true
                    if [[ \${op_test_result} -ne 0 ]];then
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

def hw_access_api_test(){
    try{
        stage('hw access api test'){
            dir('./hw_access_api'){
                sh"""#!/bin/bash
                    bazelisk clean --expunge
                    bazelisk build //lib:hw_access_api
                    bazelisk test -s //tests:all --define dtu_instr=true
                """
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
    finally{
        dir('./hw_access_api'){
            junit healthScaleFactor: 0.0, allowEmptyResults: true, testResults: 'bazel-testlogs/**/*.xml'
        }
    }
}

def dtu_pp_run_test(){
    try{
        stage('dtu_pp_run_test'){
            dir('./benchmark/enflame_model_test'){
                sh"""#!/bin/bash
                    rm -rf /tmp/*.db
                    export EXT=true
                    dtupp --db CI --table profiler_test --prof_trace libprofile.data --output test --summary --op --timeline --quiet --cleanargs=false
                """
                if (!findFiles(glob: 'test*.csv')){
                    echo "csv file not generated"
                    error("csv file not generated")
                }
                if (!findFiles(glob: 'test*.json')){
                    echo "json file not generated"
                    error("json file not generated")
                }
            }
        }
    }
    catch(e){
        currentBuild.result = 'FAIL'
        result = "FAIL"
    }
}

def update_primo_path(){
    stage('change primo path in sdk'){
        sh'''#!/bin/bash
            pushd ${WORKSPACE}/dtu_sdk
                PRIMO_DIR="${WORKSPACE}/Primo"
                sed -i '8a local_repository(name="Primo",path="'\${PRIMO_DIR}'",)' WORKSPACE
                sed -i '48,54s/^/#/g' sdk.bzl
            popd
        '''
    }
}
return this //required by groovy rule, don't delete.
