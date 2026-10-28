# Run from the repository root. Creates a review branch; never overwrites your current branch.
$ErrorActionPreference = 'Stop'
if (git status --porcelain) { throw 'Commit or stash your changes before updating.' }
$upstreamUrl = 'https://github.com/NuvioMedia/NuvioMobile.git'
git -c http.sslBackend=openssl fetch $upstreamUrl cmp-rewrite
if ($LASTEXITCODE -ne 0) { throw 'Could not fetch the latest Nuvio source.' }
$upstreamCommit = git rev-parse FETCH_HEAD
$updateBranch = 'update/nuvio-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
git switch -c $updateBranch
if ($LASTEXITCODE -ne 0) { throw 'Could not create the update branch.' }
git merge --no-edit $upstreamCommit
if ($LASTEXITCODE -ne 0) {
    Write-Host 'The update needs conflict resolution. Your previous branch is unchanged.'
    Write-Host 'Resolve conflicts, or use git merge --abort to cancel.'
    exit 1
}
Write-Host "Update prepared on $updateBranch. Run the Universe tests and ARM64 build before merging it."
