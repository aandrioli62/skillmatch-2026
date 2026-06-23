@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup batch script, version 3.3.2

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
@IF NOT "%MAVEN_PROJECTBASEDIR%"=="" @GOTO init
@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%

:init
@SET MAVEN_HOME_DEFAULT=%USERPROFILE%\.m2\wrapper\dists

@SET PROPERTIES_FILE=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties
@IF NOT EXIST "%PROPERTIES_FILE%" (
  @ECHO No .mvn\wrapper\maven-wrapper.properties file found.
  @EXIT /B 1
)

@FOR /F "usebackq tokens=1,2 delims==" %%a IN ("%PROPERTIES_FILE%") DO (
  @IF "%%a"=="distributionUrl" SET "distributionUrl=%%b"
)

@IF "%distributionUrl%"=="" (
  @ECHO distributionUrl is not set in %PROPERTIES_FILE%
  @EXIT /B 1
)

@FOR %%F IN ("%distributionUrl%") DO (
  @SET "DISTRIBUTION_FILE=%%~nxF"
  @SET "DISTRIBUTION_ID=%%~nF"
)

@SET "MAVEN_HOME=%MAVEN_HOME_DEFAULT%\%DISTRIBUTION_ID%\%DISTRIBUTION_FILE%"
@SET "MAVEN_HOME=%MAVEN_HOME:.zip=%"

@IF EXIST "%MAVEN_HOME%\bin\mvn.cmd" @GOTO run

@ECHO Downloading Maven from %distributionUrl%
@WHERE curl > NUL 2>&1
@IF %ERRORLEVEL% EQU 0 (
  curl -fsSL --retry 3 -o "%TEMP%\maven.zip" "%distributionUrl%"
) ELSE (
  powershell -Command "(New-Object Net.WebClient).DownloadFile('%distributionUrl%', '%TEMP%\maven.zip')"
)
@IF %ERRORLEVEL% NEQ 0 (
  @ECHO Failed to download %distributionUrl%
  @EXIT /B 1
)
@MD "%MAVEN_HOME%" 2>NUL
powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%MAVEN_HOME_DEFAULT%\%DISTRIBUTION_ID%' -Force"

:run
@SET "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
@IF NOT EXIST "%MAVEN_CMD%" (
  @SET "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.bat"
)
@"%MAVEN_CMD%" %*
