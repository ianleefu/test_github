def result_convergence_bf16='pass'
def result_convergence_fp32='pass'
def result_convergence_bf16_tlinux='pass'
def result_performance='pass'
def result_operator='pass'
def result_xla='pass'
def result_sdk='pass'
def result_umd='pass'
def result_mock='pass'

parallel(
    "sw_daily_AFC_1_FunctionTest_UMD":{
        try{
            build job: 'JF_sw_daily_AFC_1_FunctionTest_UMD', parameters: [
                string(name: 'asic_config', value: '1c8s'),
                string(name: 'execNode', value: 'sw_stable_4C'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'umd_Commit', value: "$tagName"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'umd_refID', value: "$umd_refID")]
        }
        catch(e){
            result_umd='fail'
        }
    },

    "sw_daily_AFC_2_FunctionTest_SDK":{
        try{
            build job: 'JF_sw_daily_AFC_2_FunctionTest_SDK', parameters: [
                string(name: 'execNode', value: 'sw_stable_4C'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'sdk_Commit', value: "$tagName"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'sdk_refID', value: "$sdk_refID")]
        }
        catch(e){
            result_sdk='fail'
        }
    },

    "sw_daily_AFC_3_FunctionTest_XLA":{
        try{
            build job: 'JF_sw_daily_AFC_3_FunctionTest_XLA', parameters: [
                string(name: 'execNode', value: 'sw_stable_4C'),
                string(name: 'asic_config', value: '1c8s'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'tensorflow_Commit', value: "$tagName"),
                string(name: 'backend_Commit', value: "$tagName"),
                string(name: 'build_job_name', value: "$build_job_name"),
                string(name: 'build_ID', value: "$build_ID"),
                string(name: 'backend_refID', value: "$backend_refID"),
                string(name: 'tensorflow_refID', value: "$tensorflow_refID"),
                string(name: 'kmd_refID', value: "$kmd_refID")]
        }
        catch(e){
            result_xla='fail'
        }
    },

    "sw_daily_AFC_4_FunctionTest_Operator":{
        try{
            build job: 'JF_sw_daily_AFC_4_FunctionTest_Operator', parameters: [
                string(name: 'execNode', value: 'sw_stable_4C'),
                string(name: 'build_job_name', value: "$build_job_name"),
                string(name: 'build_ID', value: "$build_ID"),
                string(name: 'asic_config', value: '1c8s'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'backend_Commit', value: "$tagName"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'backend_refID', value: "$backend_refID")]
        }
        catch(e){
            result_operator='fail'
        }
    },

    "sw_daily_AFC_5_Convergence_bf16":{
        try{
            build job: 'JF_sw_daily_AFC_5_Convergence', parameters: [
                string(name: 'execNode', value: 'sw_stable_4C'),
                string(name: 'build_job_name', value: "$build_job_name"),
                string(name: 'build_ID', value: "$build_ID"),
                string(name: 'asic_config', value: '4c32s'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'benchmark_Commit', value: "$tagName"),
                string(name: 'benchmark_refID', value: "$benchmark_refID"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'batch', value: '32'),
                string(name: 'epoch', value: '128'),
                string(name: 'data_format', value: 'CHNW'),
                string(name: 'dtype', value: 'bf16'),
                string(name: 'training_once', value: 'false'),
                string(name: 'test_env', value: 'env_setup_4c32.sh')]
        }
        catch(e){
            result_convergence_bf16='fail'
        }
    },

    "sw_daily_AFC_5_Convergence_fp32":{
        try{
            build job: 'JF_sw_daily_AFC_5_Convergence', parameters: [
                string(name: 'execNode', value: 'sw_stable_4C'),
                string(name: 'build_job_name', value: "$build_job_name"),
                string(name: 'build_ID', value: "$build_ID"),
                string(name: 'asic_config', value: '4c32s'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'benchmark_Commit', value: "$tagName"),
                string(name: 'benchmark_refID', value: "$benchmark_refID"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'batch', value: '32'),
                string(name: 'epoch', value: '128'),
                string(name: 'data_format', value: 'CHNW'),
                string(name: 'dtype', value: 'fp32'),
                string(name: 'training_once', value: 'false'),
                string(name: 'test_env', value: 'env_setup_4c32.sh')]
        }
        catch(e){
            result_convergence_fp32='fail'
        }
    },

    "sw_daily_AFC_5_Convergence_bf16_tlinux":{
        try{
            build job: 'JF_sw_daily_AFC_8_Convergence_tlinux', parameters: [
                string(name: 'execNode', value: 'sse_lab_asic_155_Tlinux'),
                string(name: 'build_job_name', value: "$build_job_name"),
                string(name: 'build_ID', value: "$build_ID"),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'benchmark_Commit', value: "$tagName"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'benchmark_refID', value: "$benchmark_refID"),
                string(name: 'asic_config', value: '4c32s'),
                string(name: 'batch', value: '32'),
                string(name: 'epoch', value: '128'),
                string(name: 'data_format', value: 'CHNW'),
                string(name: 'dtype', value: 'bf16'),
                string(name: 'training_once', value: 'false'),
                string(name: 'test_env', value: 'env_setup_4c32.sh')]
        }
        catch(e){
            result_convergence_bf16_tlinux='fail'
        }
    },

    "sw_daily_AFC_6_Performance":{
        try{
            build job: 'JF_sw_daily_AFC_6_Performance', parameters: [
                string(name: 'execNode', value: 'sse_lab_asic_128_peng'),
                string(name: 'build_job_name', value: "$build_job_name"),
                string(name: 'build_ID', value: "$build_ID"),
                string(name: 'asic_config', value: 'odma_burst_len=2'),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'benchmark_Commit', value: "$tagName"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'benchmark_refID', value: "$benchmark_refID"),
                string(name: 'data_format', value: 'CHNW'),
                string(name: 'batch', value: '32'),
                string(name: 'epoch', value: '4'),
                string(name: 'dtype', value: 'bf16'),
                string(name: 'test_env', value: 'env_setup_4c32.sh'),
                string(name: 'training_once', value: 'true')]
        }
        catch(e){
            result_performance='fail'
        }
    },

    "sw_daily_AFC_7_Mock":{
        try{
            build job: 'JF_sw_daily_AFC_7_Mock', parameters: [
                string(name: 'sdk_Commit', value: "$tagName"),
                string(name: 'kmd_Commit', value: "$tagName"),
                string(name: 'backend_Commit', value: "$tagName"),
                string(name: 'tensorflow_Commit', value: "$tagName"),
                string(name: 'sdk_refID', value: "$sdk_refID"),
                string(name: 'kmd_refID', value: "$kmd_refID"),
                string(name: 'backend_refID', value: "$backend_refID"),
                string(name: 'tensorflow_refID', value: "$tensorflow_refID")]
        }
        catch(e){
            result_mock='fail'
        }
    }
)

currentBuild.description+="<br>result_convergence_bf16:${result_convergence_bf16}"
currentBuild.description+="<br>result_convergence_fp32:${result_convergence_fp32}"
currentBuild.description+="<br>result_convergence_bf16_tlinux:${result_convergence_bf16_tlinux}"
currentBuild.description+="<br>result_performance:${result_performance}"
currentBuild.description+="<br>result_operator:${result_operator}"
currentBuild.description+="<br>result_xla:${result_xla}"
currentBuild.description+="<br>result_sdk:${result_sdk}"
currentBuild.description+="<br>result_umd:${result_umd}"
currentBuild.description+="<br>result_mock:${result_mock}"