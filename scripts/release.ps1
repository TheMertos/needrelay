#Requires -Version 5.1
<#
.SYNOPSIS
  Commits and pushes the current branch, then tags latest and the next vX.Y.Z.

.DESCRIPTION
  Run with no parameters:
    .\scripts\release.ps1

  1. git add -A and commit (skipped if the working tree is already clean)
  2. git push (branch first — GitHub Actions publishes Docker Hub :latest from main)
  3. Annotated tags: moving "latest" and the next patch "vMAJOR.MINOR.PATCH"
  4. Push those tags (vX.Y.Z is immutable; latest is force-updated)

  Starts at v0.0.1 when no v* tags exist.
#>
# Prefer exit-code checks over treating git stderr as terminating errors.
$ErrorActionPreference = "Continue"

# Writes an error and exits with code 1.
# Inputs: message (string). Outputs: none (exits).
function Fail([string]$message) {
	Write-Host "ERROR: $message" -ForegroundColor Red
	exit 1
}

# Runs git and returns trimmed stdout; fails if exit code is non-zero.
# Inputs: git argument list. Outputs: stdout string.
function Invoke-Git([Parameter(Mandatory = $true)][string[]]$GitArgs) {
	$output = & git @GitArgs 2>&1
	$code = $LASTEXITCODE
	$text = ($output | Out-String).Trim()
	if ($code -ne 0) {
		Fail ("git " + ($GitArgs -join " ") + " failed: " + $text)
	}
	return $text
}

# Returns the absolute git repository root, or fails if not in a work tree.
# Inputs: none. Outputs: root path string.
function Get-GitRoot {
	$output = & git rev-parse --show-toplevel 2>&1
	if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace(($output | Out-String))) {
		Fail "Not inside a git repository."
	}
	return ($output | Out-String).Trim()
}

# Computes the next patch version tag from existing v* tags.
# Inputs: none. Outputs: tag string such as v1.2.4.
function Get-NextVersionTag {
	$rawTags = @(git tag --list "v*")
	$parsed = @()
	foreach ($tag in $rawTags) {
		$t = $tag.Trim()
		if ($t -match '^v(\d+)\.(\d+)\.(\d+)$') {
			$parsed += [pscustomobject]@{
				Tag   = $t
				Major = [int]$Matches[1]
				Minor = [int]$Matches[2]
				Patch = [int]$Matches[3]
			}
		}
	}

	if ($parsed.Count -eq 0) {
		return "v0.0.1"
	}

	$latest = $parsed | Sort-Object Major, Minor, Patch | Select-Object -Last 1
	$nextPatch = $latest.Patch + 1
	return "v$($latest.Major).$($latest.Minor).$nextPatch"
}

$repoRoot = Get-GitRoot
Set-Location $repoRoot

$branch = (& git rev-parse --abbrev-ref HEAD 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branch) -or $branch -eq "HEAD") {
	Fail "Detached HEAD is not supported. Check out a branch first."
}

$upstream = (& git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($upstream)) {
	Fail "Current branch '$branch' has no upstream. Set upstream with: git push -u origin $branch"
}

$versionTag = Get-NextVersionTag
$existing = (& git tag --list $versionTag 2>&1 | Out-String).Trim()
if (-not [string]::IsNullOrWhiteSpace($existing)) {
	Fail "Tag $versionTag already exists."
}

$message = "Release $versionTag"

Write-Host "Releasing $versionTag on branch $branch ..."
if ($branch -ne "main") {
	Write-Host "Warning: Docker Hub :latest is published from main only. You are on '$branch'." -ForegroundColor Yellow
}

Write-Host "1/4 git add + commit"
Invoke-Git @("add", "-A") | Out-Null

$status = (& git status --porcelain 2>&1 | Out-String).Trim()
if (-not [string]::IsNullOrWhiteSpace($status)) {
	Invoke-Git @("commit", "-m", $message) | Out-Null
} else {
	Write-Host "Working tree clean; pushing current HEAD."
}

Write-Host "2/4 git push $branch"
Invoke-Git @("push") | Out-Null

Write-Host "3/4 tag latest and $versionTag"
Invoke-Git @("tag", "-a", $versionTag, "-m", $message) | Out-Null
Invoke-Git @("tag", "-fa", "latest", "-m", "Latest ($versionTag)") | Out-Null

Write-Host "4/4 push tags"
Invoke-Git @("push", "origin", $versionTag) | Out-Null
# Moving pointer only — does not rewrite the branch.
Invoke-Git @("push", "--force", "origin", "refs/tags/latest") | Out-Null

Write-Host "Done. Pushed $branch, tags latest and $versionTag"
Write-Host "Docker Hub: :latest from main, :$versionTag from the version tag."
