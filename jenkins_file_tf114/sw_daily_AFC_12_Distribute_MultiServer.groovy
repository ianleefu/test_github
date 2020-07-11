
        currentBuild.description="${env.NODE_NAME}; ${model_name}; ${dataset};step: ${step}; <br>kmd_Commit: ${kmd_Commit}"
        stage('--clean ws--'){
            dir('./'){
                cleanWs()
                withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
                    withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                        sh"git clone ssh://jenkins@gerrit.enflame.cn:29418/jenkinsfile"
                    }
                }
            }
        }

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

        stage('--install pkg for multi server--'){
            def String[] dist_node_list         // for execution node
            def branches = [:]                  // paralle dict

            dist_node_list = dist_node.split('\n')
            for( String values : dist_node_list ){
                println(values)
                println "dist_node_list size ${dist_node_list.size()}"
            }

            println("---install parallel on exectuion node---")
            // dist_node_list[0] is consider as master node in distribute node, the master node will start by docker.inside
            for (int i = 1; i < dist_node_list.size(); i++) {
                j = 0

                branches["install_branch${i}"] = {
                    j++
                    println"run on dist_node: ${dist_node_list[j]}"

                    node("${dist_node_list[j]}") {
                        stage('--clean ws--'){
                            dir('./'){
                                cleanWs()
                                withCredentials([sshUserPrivateKey(credentialsId: 'gerrit_user_jenkins', keyFileVariable: 'PKEY')]) {
                                    withEnv(["GIT_SSH_COMMAND=ssh -i $PKEY"]){
                                        sh"git clone ssh://jenkins@gerrit.enflame.cn:29418/jenkinsfile"
                                    }
                                }
                            }
                        }//stage

                        stage('--load common util--'){
                        	util_init = load "jenkinsfile/common_util/util_init.groovy"
                        	util_make = load "jenkinsfile/common_util/util_make.groovy"
                        	util_test = load "jenkinsfile/common_util/util_test.groovy"
                        	util_log = load "jenkinsfile/common_util/util_log.groovy"
                            util_distribute = load "jenkinsfile/integration_util/util_distribute.groovy"
                        }
                        util_distribute.distribute_install("535862119440")
                    }//node

                }//branch

            }//for
            parallel branches
        }//stage
