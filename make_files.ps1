# Define the list of files to create (relative to project root)
$files = @(
    "src/main/java/com/smartdorm/backend/entity/UserResponse.java",
    "src/main/java/com/smartdorm/backend/entity/MatchingResult.java",
    "src/main/java/com/smartdorm/backend/repository/UserResponseRepository.java",
    "src/main/java/com/smartdorm/backend/repository/MatchingResultRepository.java",
    "src/main/java/com/smartdorm/backend/dto/StudentDtos.java",
    "src/main/java/com/smartdorm/backend/service/StudentService.java",
    "src/main/java/com/smartdorm/backend/service/AdminAssignmentService.java",
    "src/main/java/com/smartdorm/backend/controller/StudentController.java",
    "src/main/java/com/smartdorm/backend/controller/AdminAssignmentController.java",
    "src/test/java/com/smartdorm/backend/controller/StudentFlowIntegrationTest.java"
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
        New-Item -Path $file -ItemType File -Force | Out-Null
        Write-Host "Created file: $file"
    } else {
        Write-Host "Skipped (already exists): $file"
    }
}

Write-Host "`nFile creation process completed."
