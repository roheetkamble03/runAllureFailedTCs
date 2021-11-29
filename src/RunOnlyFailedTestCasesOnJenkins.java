import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RunOnlyFailedTestCasesOnJenkins {

    public static void main(String[] args) throws Exception {
        boolean isZipFileProcessed = false;
        JSONObject packageJSONObject = new JSONObject();
        int retryCount = 0;
        String command = null;
        File tempAllureReportZipFile = null;
        String jobName = args[0];
        String buildNumber = args[1];
        boolean isTwoDotO = Boolean.parseBoolean(args[2]);
        while(!isZipFileProcessed) {
            try {
                retryCount++;
                if(isTwoDotO) {
                    command = "curl --location --request GET https://jenkins-digital.rccl.com/" + jobName + "/" + buildNumber + "/artifact/allure-report.zip  --user 42aac170:d17088ef48af88b3fdb8b53ddd28e3c3";
                }else{
                    if (jobName.equals("regression-core-android-automation")) {
                        command = "curl --location --request GET https://jenkins-digital.rccl.com/job/guest-app/job/MobileAutomation/job/" + jobName + "/" + buildNumber + "/artifact/allure-report.zip  --user 42aac170:d17088ef48af88b3fdb8b53ddd28e3c3";
                    } else {
                        command = "curl --location --request GET https://jenkins-digital.rccl.com/job/guest-app/job/automation/job/" + jobName + "/" + buildNumber + "/artifact/allure-report.zip  --user 42aac170:d17088ef48af88b3fdb8b53ddd28e3c3";
                    }
                }
                ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
                processBuilder.directory(new File("/home/"));
                Process process = processBuilder.start();
                InputStream inputStream = process.getInputStream();

                tempAllureReportZipFile = new File("tempAllureReportZipFile.zip");
                FileUtils.copyInputStreamToFile(inputStream,tempAllureReportZipFile);
                process.destroy();

                ZipFile zipFile = new ZipFile(tempAllureReportZipFile);
                ZipEntry entry = zipFile.getEntry("allure-report/data/packages.json");

                packageJSONObject = (JSONObject) new JSONParser().parse(new InputStreamReader(zipFile.getInputStream(entry)));
                isZipFileProcessed = true;
            }catch (Exception e){
                if(retryCount == 4)
                    throw new Exception("Processing of Allure report zip file is failed, for curl \n"+ command);
            }
        }

        JSONArray trackJSONArray = (JSONArray) packageJSONObject.getOrDefault("children",new String[0]);
        String suiteQuery = processPackageJson(trackJSONArray);
        System.out.println(suiteQuery);
        tempAllureReportZipFile.deleteOnExit();
    }


    private static String processPackageJson(final JSONArray grandLevelArrayJsonObject){
        JSONObject levelOneParentObject;
        JSONObject levelOneObject;
        JSONArray levelOneChildrenArray;

        JSONArray levelOneChildObjectArray;
        JSONArray levelTwoChildObjectArray;
        JSONArray levelThreeChildObjectArray;
        JSONObject levelOneChildObject;
        JSONObject levelTwoChildObject;
        JSONObject levelThreeObject;

        String testCasePath = null;
        String testCaseStatus;
        String tCtoBeExecute;
        Set<String> testCaseListTobeExecute = new HashSet<>();

        //Track
        for(int trackCount=0;trackCount<grandLevelArrayJsonObject.size();trackCount++){
            levelOneParentObject = (JSONObject) grandLevelArrayJsonObject.get(trackCount);
            levelOneChildrenArray = (JSONArray) levelOneParentObject.getOrDefault("children",new String[0]);

            //Class
            for (int levelOneChildCount = 0;levelOneChildCount<levelOneChildrenArray.size();levelOneChildCount++){
                levelOneObject = (JSONObject) levelOneChildrenArray.get(levelOneChildCount);

                if(levelOneObject.get("children")!=null) {
                    levelOneChildObjectArray = (JSONArray) levelOneObject.getOrDefault("children", new String[0]);
                    //TestCase
                    for (int levelOneTestCaseCount = 0; levelOneTestCaseCount < levelOneChildObjectArray.size(); levelOneTestCaseCount++) {
                        levelOneChildObject = (JSONObject) levelOneChildObjectArray.get(levelOneTestCaseCount);
                        if (levelOneChildObject.get("status") != null) {
                            testCaseStatus = levelOneChildObject.get("status").toString();
                            if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                                testCasePath = levelOneParentObject.get("name").toString().trim() + ".";
                                testCasePath = testCasePath + levelOneObject.get("name").toString() + ".";
                                testCasePath = testCasePath + levelOneChildObject.get("name").toString();
                                tCtoBeExecute = "--tests " + testCasePath;
                                testCaseListTobeExecute.add(tCtoBeExecute);
                            }
                        } else {
                                levelTwoChildObjectArray = (JSONArray) levelOneChildObject.getOrDefault("children", new String[0]);
                                for (int levelTwoChildCount = 0; levelTwoChildCount < levelTwoChildObjectArray.size(); levelTwoChildCount++) {
                                    levelTwoChildObject = (JSONObject) levelTwoChildObjectArray.get(levelTwoChildCount);
                                    if (levelTwoChildObject.get("children") != null) {
                                        if (levelTwoChildObject.get("status") != null) {
                                            testCaseStatus = levelTwoChildObject.get("status").toString();
                                            if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                                                testCasePath = levelOneParentObject.get("name").toString().trim() + ".";
                                                testCasePath = testCasePath + levelOneObject.get("name").toString() + ".";
                                                testCasePath = testCasePath + levelOneChildObject.get("name").toString()+ ".";
                                                testCasePath = testCasePath + levelTwoChildObject.get("name").toString();
                                                tCtoBeExecute = "--tests " + testCasePath;
                                                testCaseListTobeExecute.add(tCtoBeExecute);
                                            }
                                        } else {
                                            levelThreeChildObjectArray = (JSONArray) levelTwoChildObject.getOrDefault("children", new String[0]);
                                            for (int levelThreeChildCount = 0; levelThreeChildCount < levelThreeChildObjectArray.size(); levelThreeChildCount++) {
                                                levelThreeObject = (JSONObject) levelThreeChildObjectArray.get(levelThreeChildCount);
                                                if (levelThreeObject.get("status") != null) {
                                                    testCaseStatus = levelThreeObject.get("status").toString();
                                                    if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                                                        testCasePath = levelOneParentObject.get("name").toString().trim() + ".";
                                                        testCasePath = testCasePath + levelOneObject.get("name").toString() + ".";
                                                        testCasePath = testCasePath + levelOneChildObject.get("name").toString()+ ".";
                                                        testCasePath = testCasePath + levelTwoChildObject.get("name").toString() + ".";
                                                        testCasePath = testCasePath + levelThreeObject.get("name").toString();
                                                        tCtoBeExecute = "--tests " + testCasePath;
                                                        testCaseListTobeExecute.add(tCtoBeExecute);
                                                    }
                                                } else {

                                                }
                                            }
                                        }
                                    }else {
                                            testCaseStatus = levelTwoChildObject.get("status").toString();
                                            if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                                                testCasePath = levelOneParentObject.get("name").toString().trim() + ".";
                                                testCasePath = testCasePath + levelOneObject.get("name").toString() + ".";
                                                testCasePath = testCasePath + levelOneChildObject.get("name").toString()+ ".";
                                                testCasePath = testCasePath + levelTwoChildObject.get("name").toString();
                                                tCtoBeExecute = "--tests " + testCasePath;
                                                testCaseListTobeExecute.add(tCtoBeExecute);
                                            }
                                        }
                                    }
                                }
                        }
                }else{
                    testCaseStatus = levelOneParentObject.get("status").toString();
                    if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                        testCasePath = levelOneParentObject.get("name").toString() + ".";
                        testCasePath = testCasePath + levelOneObject.get("name").toString() + ".";
                        testCasePath = testCasePath + levelOneObject.get("name").toString();
                        tCtoBeExecute = "--tests " + testCasePath;
                        testCaseListTobeExecute.add(tCtoBeExecute);
                    }
                }
            }
        }
        return testCaseListTobeExecute.toString().replaceAll(","," ").replaceAll("[\\[\\]]", "").trim();
    }
}
