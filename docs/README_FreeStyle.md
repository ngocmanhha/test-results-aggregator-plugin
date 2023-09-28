# Test Results Aggregator Free Style Job Configuration

1. Test Result Aggregator Plugin can be used as a "Free Style Project".
<img src="screenshots/FreeStyleProject.png" alt="Free Style Project" style="float:right;margin-right:10px;width:600"/>

     
2. Select "Add Post Build" action and scroll to "Aggregate Test Results" action. 
<img src="screenshots/PostBuildAction.png" alt="Post Build Action" style="float:right;margin-right:10px;width:600"/>
     
3. Add Groups/Teams and Jenkins Jobs 
<img src="screenshots/FreeStyleProject_Jobs.png" alt="Jobs Configuration" style="float:right;margin-right:10px;width:600"/>

| Argument | Optional | Description | 
| --- | ----------- | ----------- |
| Group/Team | true | Group Jenkins jobs and results in reports. For example group by teams, products, or testing types. |
| Job Name | false | it's the exact Jenkins job name to get results. In case of a job inside a 'folder' use: folderName/jobName, for multi-folders use folder path for example folder1/folder2/jobName .|
| Job Friendly Name | true |if null or empty then "Job Name" will be used in reports. | 
   
4. Add Recipients List, Before, After Body text, theme, and Sort by option 
<img src="screenshots/ReceipientsList.png" alt="Recipients" style="float:right;margin-right:10px;width: 600"/>

| Argument | Description | 
| --- | ----------- |
| Recipients List | Comma-separated recipients list, ex: nick@some.com,mairy@some.com. if empty no email will be triggered. Supports job variables, for example '${my_parameter_for_mail}'. |
| Recipients List CC | Comma-separated recipients list CC, ex: 'nick@some.com,mairy@some.com' If empty or blank no email will be triggered. Supports job variables. |
| Recipients list Bcc | Comma-separated recipients list Bcc, ex: 'nick@some.com,mairy@some.com' If empty or blank no email will be triggered. Supports job variables. |
| Recipients list for Ignored Jobs | Comma-separated recipients list for ignored jobs Bcc, ex: 'nick@some.com,mairy@some.com' If empty or blank no email will be triggered. Supports job variables. |
| Subject prefix  | Prefix for email's subject. Supports job & env variables. |
| Columns  | Html & email report columns and the order of them, comma separated. Possible columns are : Health, Job, Status, Percentage, Total, Pass, Fail, Skip, Commits, LastRun, Duration, Description, Packages, Files, Classes, Methods, Lines, Conditions, Sonar, Build |
|Text Before body mail |Plain text or html code to add before report table. Supports job & env variables , for example ${WORKSPACE} or ${myVariable} | 
|Text After body mail |Plain text or html code to add after report table. Supports also job & env variables , for example ${WORKSPACE} or ${myVariable} | 
|Mail Theme | |Mail theme with values : Ligth or dark |
|Sort Results By | The report will be sorted accordingly. If there are Groups then sorting refers to jobs inside a group. |
  
5. Outdated results 
<img src="screenshots/OutofDate.png" alt="OutofDate" style="float:right;margin-right:10px;width: 600"/>
     
Jobs with results more than X hours ago will be marked with 'red' color under 'Last Run' column report. Otherwise (if blank) column 'Last Run' will just have the timestamp of job completion.
 
6. Compare with the previous run 
<img src="screenshots/CompareWithPrevious.png" alt="CompareWithPrevious" style="float:center;margin-right:10px;width:600"/>

Compare the next run with the previous regarding job statuses, tests results, and code coverage metrics. If false then no differences are displayed in report, no signs + -
 
7. Ignore Jobs from report by status 
<img src="screenshots/IgnoreJobs.png" alt="IgnoreJobs" style="float:center;margin-right:10px;width:600"/>    

Ignore from report jobs with status: NOT_FOUND, DISABLED, ABORTED or RUNNING.
