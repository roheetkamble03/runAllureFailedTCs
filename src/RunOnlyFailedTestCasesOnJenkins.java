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
        while(!isZipFileProcessed) {
            try {
                retryCount++;
                if(jobName.equals("regression-core-android-automation")) {
                    command = "curl --location --request GET https://jenkins-digital.rccl.com/job/guest-app/job/MobileAutomation/job/" + jobName + "/" + buildNumber + "/artifact/allure-report.zip  --user 42aac170:d17088ef48af88b3fdb8b53ddd28e3c3";
                }else {
                    command = "curl --location --request GET https://jenkins-digital.rccl.com/job/guest-app/job/automation/job/" + jobName + "/" + buildNumber + "/artifact/allure-report.zip  --user 42aac170:d17088ef48af88b3fdb8b53ddd28e3c3";
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


    private static String processPackageJson(final JSONArray trackJSONArray){
        JSONObject trackObject;

        JSONArray classJSONArray;
        JSONObject classJSONObject;

        JSONArray testCaseJSONArray;
        JSONObject testCaseJSONObject;

        String trackName;
        String className;
        String testCaseName;
        String testCaseStatus;
        String tCtoBeExecute;
        Set<String> testCaseListTobeExecute = new HashSet<>();

        //Track
        for(int trackCount=0;trackCount<trackJSONArray.size();trackCount++){
            trackObject = (JSONObject) trackJSONArray.get(trackCount);
            trackName = trackObject.get("name").toString().trim();
            classJSONArray = (JSONArray) trackObject.getOrDefault("children",new String[0]);
            //Class
            for (int testClassCount = 0;testClassCount<classJSONArray.size();testClassCount++){
                classJSONObject = (JSONObject) classJSONArray.get(testClassCount);
                className = classJSONObject.get("name").toString();

                if(classJSONObject.get("children")!=null){
                    testCaseJSONArray = (JSONArray) classJSONObject.getOrDefault("children", new String[0]);
                    //TestCase
                    for (int testCaseCount = 0; testCaseCount < testCaseJSONArray.size(); testCaseCount++) {
                        testCaseJSONObject = (JSONObject) testCaseJSONArray.get(testCaseCount);
                        testCaseName = testCaseJSONObject.get("name").toString();
                        testCaseStatus = testCaseJSONObject.get("status").toString();
                        if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                            tCtoBeExecute = "--tests " + trackName + "." + className + "." + testCaseName;
                            testCaseListTobeExecute.add(tCtoBeExecute);
                        }
                    }
                }else{
                    testCaseName = classJSONObject.get("name").toString();
                    testCaseStatus = classJSONObject.get("status").toString();
                    if (testCaseStatus.equalsIgnoreCase("broken") || testCaseStatus.equalsIgnoreCase("failed")) {
                        tCtoBeExecute = "--tests " + trackName + "." + testCaseName;
                        testCaseListTobeExecute.add(tCtoBeExecute);
                    }
                }
            }
        }
        return testCaseListTobeExecute.toString().replaceAll(","," ").replaceAll("[\\[\\]]", "").trim();
    }
}
