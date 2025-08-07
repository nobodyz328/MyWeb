@echo off
REM MyWeb 安全扫描脚本 (Windows 版本)
REM 用于本地执行 OWASP 依赖检查和生成安全报告

setlocal enabledelayedexpansion

REM 脚本配置
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set REPORT_DIR=%PROJECT_DIR%\target\dependency-check-report
set LOG_FILE=%PROJECT_DIR%\target\security-scan.log

REM 默认参数
set QUICK_SCAN=false
set FULL_SCAN=false
set SUMMARY_ONLY=false
set OPEN_REPORT=false
set CLEAN_REPORTS=false
set VERBOSE=false
set FAIL_ON_HIGH=false
set SKIP_TEST=false

REM 解析命令行参数
:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="-h" goto :show_help
if "%~1"=="--help" goto :show_help
if "%~1"=="-q" set QUICK_SCAN=true
if "%~1"=="--quick" set QUICK_SCAN=true
if "%~1"=="-f" set FULL_SCAN=true
if "%~1"=="--full" set FULL_SCAN=true
if "%~1"=="-s" set SUMMARY_ONLY=true
if "%~1"=="--summary" set SUMMARY_ONLY=true
if "%~1"=="-o" set OPEN_REPORT=true
if "%~1"=="--open" set OPEN_REPORT=true
if "%~1"=="-c" set CLEAN_REPORTS=true
if "%~1"=="--clean" set CLEAN_REPORTS=true
if "%~1"=="-v" set VERBOSE=true
if "%~1"=="--verbose" set VERBOSE=true
if "%~1"=="--fail-on-high" set FAIL_ON_HIGH=true
if "%~1"=="--skip-test" set SKIP_TEST=true
shift
goto :parse_args

:args_done

echo MyWeb 安全扫描脚本 (Windows)
echo ============================
echo.

REM 创建日志文件目录
if not exist "%PROJECT_DIR%\target" mkdir "%PROJECT_DIR%\target"
echo %date% %time%: 开始安全扫描 > "%LOG_FILE%"

REM 显示帮助信息
if "%1"=="-h" goto :show_help
if "%1"=="--help" goto :show_help

REM 清理报告
if "%CLEAN_REPORTS%"=="true" (
    echo [INFO] 清理之前的扫描报告...
    if exist "%REPORT_DIR%" (
        rmdir /s /q "%REPORT_DIR%"
        echo [SUCCESS] 报告目录已清理
    ) else (
        echo [INFO] 报告目录不存在，无需清理
    )
    if "%SUMMARY_ONLY%"=="true" exit /b 0
)

REM 检查 Maven
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven 未安装或不在 PATH 中
    exit /b 1
)

echo [INFO] Maven 已找到
mvn -version | findstr "Apache Maven" >> "%LOG_FILE%"

REM 构建 Maven 命令
set MAVEN_CMD=mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=0.0

if "%QUICK_SCAN%"=="true" (
    set MAVEN_CMD=!MAVEN_CMD! -DautoUpdate=false
    echo [INFO] 启用快速扫描模式（跳过数据库更新）
)

if "%FULL_SCAN%"=="true" (
    set MAVEN_CMD=!MAVEN_CMD! -Dformats=ALL
    echo [INFO] 启用完整扫描模式（生成所有格式报告）
)

if "%SKIP_TEST%"=="true" (
    set MAVEN_CMD=!MAVEN_CMD! -DskipTestScope=true
    echo [INFO] 跳过测试依赖扫描
)

if "%VERBOSE%"=="true" (
    set MAVEN_CMD=!MAVEN_CMD! -Dverbose=true -X
    echo [INFO] 启用详细输出模式
)

REM 执行安全扫描
if not "%SUMMARY_ONLY%"=="true" (
    echo [INFO] 开始执行 OWASP 依赖安全扫描...
    echo [INFO] 项目目录: %PROJECT_DIR%
    echo [INFO] 报告目录: %REPORT_DIR%
    echo [INFO] 执行命令: !MAVEN_CMD!
    
    cd /d "%PROJECT_DIR%"
    
    if "%VERBOSE%"=="true" (
        !MAVEN_CMD! 2>&1 | tee -a "%LOG_FILE%"
    ) else (
        !MAVEN_CMD! >> "%LOG_FILE%" 2>&1
    )
    
    if errorlevel 1 (
        echo [WARN] 安全扫描完成，但可能存在问题（退出码: !errorlevel!）
    ) else (
        echo [SUCCESS] 安全扫描完成
    )
)

REM 解析扫描结果
set JSON_REPORT=%REPORT_DIR%\dependency-check-report.json
if not exist "%JSON_REPORT%" (
    echo [ERROR] 未找到扫描报告文件: %JSON_REPORT%
    exit /b 1
)

echo [INFO] 解析扫描结果...

REM 简单的结果解析（Windows 版本使用 PowerShell）
powershell -Command "& {
    $report = Get-Content '%JSON_REPORT%' | ConvertFrom-Json;
    $totalDeps = $report.dependencies.Count;
    $vulnerableDeps = ($report.dependencies | Where-Object { $_.vulnerabilities }).Count;
    $highVulns = ($report.dependencies.vulnerabilities | Where-Object { $_.severity -eq 'HIGH' }).Count;
    $mediumVulns = ($report.dependencies.vulnerabilities | Where-Object { $_.severity -eq 'MEDIUM' }).Count;
    $lowVulns = ($report.dependencies.vulnerabilities | Where-Object { $_.severity -eq 'LOW' }).Count;
    
    Write-Host '';
    Write-Host '[INFO] === 扫描结果摘要 ===';
    Write-Host '总依赖数量:' $totalDeps;
    Write-Host '存在漏洞的依赖:' $vulnerableDeps;
    Write-Host '高危漏洞:' $highVulns;
    Write-Host '中危漏洞:' $mediumVulns;
    Write-Host '低危漏洞:' $lowVulns;
    Write-Host '';
    
    if ($highVulns -gt 0) {
        Write-Host '[ERROR] 发现' $highVulns '个高危漏洞！' -ForegroundColor Red;
        if ('%FAIL_ON_HIGH%' -eq 'true') {
            Write-Host '[ERROR] 由于存在高危漏洞，脚本退出' -ForegroundColor Red;
            exit 1;
        }
    } else {
        Write-Host '[SUCCESS] 未发现高危漏洞' -ForegroundColor Green;
    }
}"

REM 打开 HTML 报告
if "%OPEN_REPORT%"=="true" (
    set HTML_REPORT=%REPORT_DIR%\dependency-check-report.html
    if exist "!HTML_REPORT!" (
        echo [INFO] 打开 HTML 报告...
        start "" "!HTML_REPORT!"
    ) else (
        echo [ERROR] 未找到 HTML 报告文件: !HTML_REPORT!
    )
)

echo [INFO] 扫描完成！详细日志请查看: %LOG_FILE%
echo [INFO] HTML 报告位置: %REPORT_DIR%\dependency-check-report.html

goto :eof

:show_help
echo MyWeb 安全扫描脚本 (Windows)
echo.
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo     -h, --help          显示此帮助信息
echo     -q, --quick         快速扫描（跳过数据库更新）
echo     -f, --full          完整扫描（包含所有格式报告）
echo     -s, --summary       仅显示扫描摘要
echo     -o, --open          扫描完成后打开HTML报告
echo     -c, --clean         清理之前的扫描报告
echo     -v, --verbose       详细输出
echo     --fail-on-high      发现高危漏洞时失败退出
echo     --skip-test         跳过测试依赖
echo.
echo 示例:
echo     %~nx0                  # 标准扫描
echo     %~nx0 -f -o           # 完整扫描并打开报告
echo     %~nx0 -q -s           # 快速扫描并显示摘要
echo     %~nx0 --clean         # 清理报告目录
echo.
exit /b 0