Get-ChildItem -Recurse -File | ForEach-Object {
    "$((Resolve-Path $_.FullName -Relative), ''):`n$(Get-Content $_.FullName -Raw)`n----------"
} | Set-Clipboard
