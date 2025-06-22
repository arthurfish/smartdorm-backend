# 1. 配置项目路径和输出文件（根据实际情况修改）
$projectRoot = "."  # 项目根目录（必须修改！）
$outputFile = ".\docs\all_the_project.txt"  # 输出文件路径

# 2. 递归获取所有文件，排除指定文件夹
Get-ChildItem -Path $projectRoot -Recurse -File |
Where-Object {
    # 过滤逻辑：排除路径中包含\target\、\.idea\、\docs\的文件（任意层级都生效）
    $_.FullName -notmatch '\\(target|\.idea|docs)\\'
} |
# 3. 生成合并内容（相对路径+文件内容+分隔符）
ForEach-Object {
    # 获取相对于项目根目录的相对路径（更准确）
    $relativePath = Resolve-Path -Path $_.FullName -Relative
    # 拼接内容：相对路径+冒号+文件内容+分隔符（保留原始换行）
    "${relativePath}:`n$(Get-Content $_.FullName -Raw)`n----------"
} |
# 4. 输出到文件（指定UTF-8编码避免乱码）
Out-File -FilePath $outputFile -Encoding UTF8

# 提示完成
Write-Host "所有文件已合并到：$outputFile" -ForegroundColor Green
