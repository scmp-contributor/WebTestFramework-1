package com.scmp.framework.testrail.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class TestCaseResult {

    @SerializedName("cases")
    List<TestRunTest> TestRunTestList;
}
