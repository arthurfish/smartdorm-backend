<#
.SYNOPSIS
    Creates the necessary empty files and directories for Phase 3 (Cycle & Dimension Management) of the SmartDorm project.
.DESCRIPTION
    This script will:
    - Define a list of required file paths for Phase 3.
    - Loop through each path.
    - Automatically create the parent directory structure if it doesn't exist.
    - Create a new, empty file if it doesn't already exist.
    - Skip any file that already exists to prevent data loss.
    - Provide clear output on what was created and what was skipped.
.NOTES
    Author: Your AI Assistant
    Version: 1.0
    Instructions: Run this script from the project root directory.
#>

# --- List of files to be created for Phase 3 ---
$filesToCreate = @(
    # Backend Controller & Test
    "src/main/java/com/smartdorm/backend/controller/AdminCycleViewController.java",
    "src/test/java/com/smartdorm/backend/controller/AdminCycleViewControllerTest.java",

    # Frontend Thymeleaf Views
    "src/main/resources/templates/admin/cycle/cycles-list.html",
    "src/main/resources/templates/admin/cycle/cycle-form.html",
    "src/main/resources/templates/admin/cycle/dimensions-list.html",
    "src/main/resources/templates/admin/cycle/dimension-form.html",
    "src/main/resources/templates/admin/cycle/_cycle-nav.html"
)

Write-Host "Starting Phase 3 file creation..." -ForegroundColor Cyan
Write-Host "------------------------------------"

# --- Main logic to process each file ---
foreach ($file in $filesToCreate) {
    # Get the parent directory of the file
    $directory = Split-Path -Path $file -Parent

    # Check if the directory exists, if not, create it
    if (-not (Test-Path -Path $directory -PathType Container)) {
        try {
            New-Item -Path $directory -ItemType Directory -Force -ErrorAction Stop | Out-Null
            Write-Host "Created directory:" -ForegroundColor Green -NoNewline
            Write-Host " $directory"
        }
        catch {
            Write-Host "Error creating directory $directory`: $_" -ForegroundColor Red
            # Stop processing if a directory cannot be created
            break
        }
    }

    # Check if the file exists, if not, create it as an empty file
    if (-not (Test-Path -Path $file -PathType Leaf)) {
        try {
            New-Item -Path $file -ItemType File -ErrorAction Stop | Out-Null
            Write-Host "Created file:     " -ForegroundColor Green -NoNewline
            Write-Host " $file"
        }
        catch {
            Write-Host "Error creating file $file`: $_" -ForegroundColor Red
        }
    }
    else {
        # If the file already exists, skip it and notify the user
        Write-Host "Skipped existing: " -ForegroundColor Yellow -NoNewline
        Write-Host " $file"
    }
}

Write-Host "------------------------------------"
Write-Host "Script finished. All required files for Phase 3 should now exist." -ForegroundColor Cyan