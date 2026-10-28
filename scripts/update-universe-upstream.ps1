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
$personalVersionBefore = Get-Content -LiteralPath 'universe-version.properties' -Raw
$env:NUVIO_OFFICIAL_SYNC = '1'
try { git merge --no-edit $upstreamCommit }
finally { Remove-Item Env:NUVIO_OFFICIAL_SYNC -ErrorAction SilentlyContinue }
if ($LASTEXITCODE -ne 0) {
    Write-Host 'The update needs conflict resolution. Your previous branch is unchanged.'
    Write-Host 'Resolve conflicts, or use git merge --abort to cancel.'
    exit 1
}
if ((Get-Content -LiteralPath 'universe-version.properties' -Raw) -ne $personalVersionBefore) {
    throw 'STOP: upstream synchronization changed the personal version ledger. Restore it from the previous branch before continuing.'
}
foreach ($buildFile in @('androidApp/build.gradle.kts', 'composeApp/build.gradle.kts')) {
    if ((Get-Content -LiteralPath $buildFile -Raw) -notmatch 'personalRevision') {
        throw "STOP: restore independent version composition in $buildFile before building."
    }
}
Write-Host "Update prepared on $updateBranch. Personal version preserved. Run the Universe tests and both ARM builds before merging it."
