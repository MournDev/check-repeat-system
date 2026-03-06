@echo off
chcp 65001 >nul
echo ========================================
echo   实时推送功能 - 快速启动脚本
echo ========================================
echo.

REM 检查 Node.js 是否安装
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到 Node.js，请先安装 Node.js
    echo 下载地址：https://nodejs.org/
    pause
    exit /b 1
)

echo ✅ Node.js 已安装
echo.

REM 进入前端目录
cd /d "%~dp0frontend"
if %errorlevel% neq 0 (
    echo ❌ 错误：找不到 frontend 目录
    pause
    exit /b 1
)

echo 📦 检查依赖...
echo.

REM 检查 sockjs-client 和 stompjs 是否安装
if not exist "node_modules\sockjs-client" (
    echo ⚠️  未找到 sockjs-client，正在安装...
    call npm install sockjs-client --save
    if %errorlevel% neq 0 (
        echo ❌ 安装 sockjs-client 失败
        pause
        exit /b 1
    )
) else (
    echo ✅ sockjs-client 已安装
)

if not exist "node_modules\stompjs" (
    echo ⚠️  未找到 stompjs，正在安装...
    call npm install stompjs --save
    if %errorlevel% neq 0 (
        echo ❌ 安装 stompjs 失败
        pause
        exit /b 1
    )
) else (
    echo ✅ stompjs 已安装
)

echo.
echo 🚀 启动前端开发服务器...
echo.
echo 💡 提示：按 Ctrl+C 可以停止服务
echo.

start "前端服务" cmd /k "npm run dev"

echo.
echo ✅ 前端服务已启动
echo 🌐 访问地址：http://localhost:5173
echo.
echo ========================================
echo   下一步操作：
echo   1. 确保后端服务已启动（端口 8080）
echo   2. 打开浏览器访问 http://localhost:5173
echo   3. 登录账号（student1 / 123456）
echo   4. 访问"我的论文"页面
echo   5. 点击"开始查重"测试实时推送
echo ========================================
echo.

pause
