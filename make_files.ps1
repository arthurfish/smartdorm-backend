<#
.SYNOPSIS
Creates blank files for the P5 support features in the project root directory.
Does not overwrite existing files.
#>

# List of all files to create (relative to project root)
$files = @(
    "src/main/java/com/smartdorm/backend/entity/Feedback.java",
    "src/main/java/com/smartdorm/backend/entity/SwapRequest.java",
    "src/main/java/com/smartdorm/backend/entity/ContentArticle.java",
    "src/main/java/com/smartdorm/backend/entity/Notification.java",
    "src/main/java/com/smartdorm/backend/repository/FeedbackRepository.java",
    "src/main/java/com/smartdorm/backend/repository/SwapRequestRepository.java",
    "src/main/java/com/smartdorm/backend/repository/ContentArticleRepository.java",
    "src/main/java/com/smartdorm/backend/repository/NotificationRepository.java",
    "src/main/java/com/smartdorm/backend/dto/SupportDtos.java",
    "src/main/java/com/smartdorm/backend/service/SupportService.java",
    "src/main/java/com/smartdorm/backend/controller/StudentSupportController.java",
    "src/main/java/com/smartdorm/backend/controller/AdminSupportController.java",
    "src/test/java/com/smartdorm/backend/controller/SupportFeaturesIntegrationTest.java"
)

# Process each file
foreach ($file in $files) {
    # Get parent directory path
    $directory = Split-Path -Path $file -Parent

    # Create directory if it doesn't exist
    if (-not (Test-Path -Path $directory -PathType Container)) {
        New-Item -Path $directory -ItemType Directory -Force | Out-Null
        Write-Host "Created directory: $directory"
    }

    # Create file if it doesn't exist
    if (-not (Test-Path -Path $file -PathType Leaf)) {
        New-Item -Path $file -ItemType File | Out-Null
        Write-Host "Created file: $file"
    } else {
        Write-Host "Skipped existing file: $file"
    }
}

Write-Host "`nFile creation completed. Check the output for details."
