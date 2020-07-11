try{
    timeout(20){util_make.load_kmd_new('4c32s')}
    timeout(30){util_test.sv_model_test("sv_4c")}
}
catch(e){
    currentBuild.result = 'FAIL'
    result = "FAIL"
}