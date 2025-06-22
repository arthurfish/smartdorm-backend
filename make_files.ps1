<#
.SYNOPSIS
创建匹配周期与问卷管理功能所需的空白Java文件（仅当文件不存在时）。

.DESCRIPTION
该脚本会在项目根目录下创建指定的Java文件及其父目录结构，不会覆盖已存在的文件。
#>

# 定义需要创建的文件路径列表（相对于项目根目录）
$targetFiles = @(
    "src/main/java/com/smartdorm/backend/entity/MatchingCycle.java",
    "src/main/java/com/smartdorm/backend/entity/SurveyDimension.java",
    "src/main/java/com/smartdorm/backend/entity/DimensionOption.java",
    "src/main/java/com/smartdorm/backend/repository/MatchingCycleRepository.java",
    "src/main/java/com/smartdorm/backend/repository/SurveyDimensionRepository.java",
    "src/main/java/com/smartdorm/backend/dto/CycleDtos.java",
    "src/main/java/com/smartdorm/backend/mapper/CycleMapper.java",
    "src/main/java/com/smartdorm/backend/service/CycleManagementService.java",
    "src/main/java/com/smartdorm/backend/controller/CycleController.java",
    "src/test/java/com/smartdorm/backend/controller/CycleControllerIntegrationTest.java"
)

# 循环处理每个目标文件
foreach ($filePath in $targetFiles) {
    # 检查文件是否已存在
    if (-not (Test-Path -Path $filePath -PathType Leaf)) {
        # 创建父目录（如果不存在）
        $parentDir = Split-Path -Path $filePath -Parent
        if (-not (Test-Path -Path $parentDir -PathType Container)) {
            New-Item -Path $parentDir -ItemType Directory -Force | Out-Null
            Write-Host "创建目录: $parentDir"
        }

        # 创建空白文件
        New-Item -Path $filePath -ItemType File | Out-Null
        Write-Host "创建文件: $filePath"
    } else {
        Write-Host "跳过已存在的文件: $filePath"
    }
}

Write-Host "done"
