#!/bin/bash

# MyWeb 安全扫描脚本
# 用于本地执行 OWASP 依赖检查和生成安全报告

set -e

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
REPORT_DIR="$PROJECT_DIR/target/dependency-check-report"
LOG_FILE="$PROJECT_DIR/target/security-scan.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

# 显示帮助信息
show_help() {
    cat << EOF
MyWeb 安全扫描脚本

用法: $0 [选项]

选项:
    -h, --help          显示此帮助信息
    -q, --quick         快速扫描（跳过数据库更新）
    -f, --full          完整扫描（包含所有格式报告）
    -s, --summary       仅显示扫描摘要
    -o, --open          扫描完成后打开HTML报告
    -c, --clean         清理之前的扫描报告
    -v, --verbose       详细输出
    --fail-on-high      发现高危漏洞时失败退出
    --skip-test         跳过测试依赖

示例:
    $0                  # 标准扫描
    $0 -f -o           # 完整扫描并打开报告
    $0 -q -s           # 快速扫描并显示摘要
    $0 --clean         # 清理报告目录

EOF
}

# 解析命令行参数
QUICK_SCAN=false
FULL_SCAN=false
SUMMARY_ONLY=false
OPEN_REPORT=false
CLEAN_REPORTS=false
VERBOSE=false
FAIL_ON_HIGH=false
SKIP_TEST=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -q|--quick)
            QUICK_SCAN=true
            shift
            ;;
        -f|--full)
            FULL_SCAN=true
            shift
            ;;
        -s|--summary)
            SUMMARY_ONLY=true
            shift
            ;;
        -o|--open)
            OPEN_REPORT=true
            shift
            ;;
        -c|--clean)
            CLEAN_REPORTS=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --fail-on-high)
            FAIL_ON_HIGH=true
            shift
            ;;
        --skip-test)
            SKIP_TEST=true
            shift
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 清理报告目录
clean_reports() {
    log_info "清理之前的扫描报告..."
    if [ -d "$REPORT_DIR" ]; then
        rm -rf "$REPORT_DIR"
        log_success "报告目录已清理"
    else
        log_info "报告目录不存在，无需清理"
    fi
}

# 检查 Maven 是否可用
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven 未安装或不在 PATH 中"
        exit 1
    fi
    
    log_info "Maven 版本: $(mvn -version | head -n 1)"
}

# 构建 Maven 命令
build_maven_command() {
    local cmd="mvn org.owasp:dependency-check-maven:check"
    
    # 基本配置
    cmd="$cmd -DfailBuildOnCVSS=0.0"  # 不在扫描阶段失败，后续处理
    
    if [ "$QUICK_SCAN" = true ]; then
        cmd="$cmd -DautoUpdate=false"
        log_info "启用快速扫描模式（跳过数据库更新）"
    fi
    
    if [ "$FULL_SCAN" = true ]; then
        cmd="$cmd -Dformats=ALL"
        log_info "启用完整扫描模式（生成所有格式报告）"
    fi
    
    if [ "$SKIP_TEST" = true ]; then
        cmd="$cmd -DskipTestScope=true"
        log_info "跳过测试依赖扫描"
    fi
    
    if [ "$VERBOSE" = true ]; then
        cmd="$cmd -Dverbose=true -X"
        log_info "启用详细输出模式"
    fi
    
    echo "$cmd"
}

# 执行安全扫描
run_security_scan() {
    log_info "开始执行 OWASP 依赖安全扫描..."
    log_info "项目目录: $PROJECT_DIR"
    log_info "报告目录: $REPORT_DIR"
    
    cd "$PROJECT_DIR"
    
    local maven_cmd=$(build_maven_command)
    log_info "执行命令: $maven_cmd"
    
    if [ "$VERBOSE" = true ]; then
        eval "$maven_cmd" 2>&1 | tee -a "$LOG_FILE"
    else
        eval "$maven_cmd" >> "$LOG_FILE" 2>&1
    fi
    
    local exit_code=$?
    if [ $exit_code -eq 0 ]; then
        log_success "安全扫描完成"
    else
        log_warn "安全扫描完成，但可能存在问题（退出码: $exit_code）"
    fi
}

# 解析扫描结果
parse_scan_results() {
    local json_report="$REPORT_DIR/dependency-check-report.json"
    
    if [ ! -f "$json_report" ]; then
        log_error "未找到扫描报告文件: $json_report"
        return 1
    fi
    
    log_info "解析扫描结果..."
    
    # 使用 jq 解析 JSON 报告（如果可用）
    if command -v jq &> /dev/null; then
        local total_deps=$(jq '.dependencies | length' "$json_report")
        local vulnerable_deps=$(jq '[.dependencies[] | select(.vulnerabilities)] | length' "$json_report")
        local high_vulns=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "HIGH")] | length' "$json_report")
        local medium_vulns=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "MEDIUM")] | length' "$json_report")
        local low_vulns=$(jq '[.dependencies[].vulnerabilities[]? | select(.severity == "LOW")] | length' "$json_report")
        
        echo ""
        log_info "=== 扫描结果摘要 ==="
        echo "总依赖数量: $total_deps"
        echo "存在漏洞的依赖: $vulnerable_deps"
        echo "高危漏洞: $high_vulns"
        echo "中危漏洞: $medium_vulns"
        echo "低危漏洞: $low_vulns"
        echo ""
        
        if [ "$high_vulns" -gt 0 ]; then
            log_error "发现 $high_vulns 个高危漏洞！"
            
            if [ "$FAIL_ON_HIGH" = true ]; then
                log_error "由于存在高危漏洞，脚本退出"
                exit 1
            fi
        else
            log_success "未发现高危漏洞"
        fi
        
        # 显示高危漏洞详情
        if [ "$high_vulns" -gt 0 ] && [ "$SUMMARY_ONLY" = false ]; then
            echo ""
            log_warn "高危漏洞详情:"
            jq -r '.dependencies[].vulnerabilities[]? | select(.severity == "HIGH") | "- " + .name + ": " + .description' "$json_report"
        fi
        
    else
        log_warn "jq 未安装，无法解析详细结果"
        log_info "请安装 jq 以获得更好的结果解析体验"
    fi
}

# 打开 HTML 报告
open_html_report() {
    local html_report="$REPORT_DIR/dependency-check-report.html"
    
    if [ ! -f "$html_report" ]; then
        log_error "未找到 HTML 报告文件: $html_report"
        return 1
    fi
    
    log_info "打开 HTML 报告..."
    
    # 根据操作系统选择合适的命令
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        open "$html_report"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        if command -v xdg-open &> /dev/null; then
            xdg-open "$html_report"
        elif command -v firefox &> /dev/null; then
            firefox "$html_report"
        else
            log_warn "无法自动打开浏览器，请手动打开: $html_report"
        fi
    elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
        # Windows
        start "$html_report"
    else
        log_warn "未知操作系统，请手动打开: $html_report"
    fi
}

# 主函数
main() {
    echo "MyWeb 安全扫描脚本"
    echo "===================="
    echo ""
    
    # 创建日志文件
    mkdir -p "$(dirname "$LOG_FILE")"
    echo "$(date): 开始安全扫描" > "$LOG_FILE"
    
    # 清理报告（如果需要）
    if [ "$CLEAN_REPORTS" = true ]; then
        clean_reports
        if [ "$SUMMARY_ONLY" = true ]; then
            exit 0
        fi
    fi
    
    # 检查环境
    check_maven
    
    # 执行扫描
    if [ "$SUMMARY_ONLY" = false ]; then
        run_security_scan
    fi
    
    # 解析结果
    parse_scan_results
    
    # 打开报告（如果需要）
    if [ "$OPEN_REPORT" = true ]; then
        open_html_report
    fi
    
    log_info "扫描完成！详细日志请查看: $LOG_FILE"
    log_info "HTML 报告位置: $REPORT_DIR/dependency-check-report.html"
}

# 执行主函数
main "$@"