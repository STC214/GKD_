param(
    [Parameter(Mandatory = $true)]
    [string]$Path,
    [string]$TargetAppId = "tv.danmaku.bili"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "Diagnostics file not found: $Path"
}

$pattern = [regex]' id=(?<id>\d+) stage=(?<stage>[^ ]+) outcome=(?<outcome>[^ ]+) reason=(?<reason>[^ ]+)'
$coalescedPattern = [regex]' coalesced=(?<count>\d+)'
$targetAppPattern = [regex](" app=" + [regex]::Escape($TargetAppId) + "(?: |$)")
$records = foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
    $match = $pattern.Match($line)
    if (-not $match.Success) { continue }
    $coalescedMatch = $coalescedPattern.Match($line)
    [pscustomobject]@{
        Id        = [long]$match.Groups['id'].Value
        Stage     = $match.Groups['stage'].Value
        Outcome   = $match.Groups['outcome'].Value
        Reason    = $match.Groups['reason'].Value
        TargetApp = $targetAppPattern.IsMatch($line)
        Coalesced = if ($coalescedMatch.Success) {
            [int]$coalescedMatch.Groups['count'].Value
        } else {
            0
        }
        Line      = $line
    }
}

$groups = @($records | Group-Object Id)
$eventGroups = @($groups | Where-Object { $_.Group.Reason -contains 'EventReceived' })
$orphanGroups = @($eventGroups | Where-Object Count -eq 1)
$targetGroups = @($eventGroups | Where-Object { @($_.Group | Where-Object TargetApp).Count -gt 0 })
$targetOrphans = @($targetGroups | Where-Object Count -eq 1)
$coalescedGroups = @($eventGroups | Where-Object {
        ($_.Group | Measure-Object Coalesced -Maximum).Maximum -gt 1
    })
$coalescedOrphans = @($coalescedGroups | Where-Object Count -eq 1)

[pscustomobject]@{
    Records             = $records.Count
    CorrelationIds      = $groups.Count
    EventIds            = $eventGroups.Count
    OrphanEventIds      = $orphanGroups.Count
    TargetAppId         = $TargetAppId
    TargetEventIds      = $targetGroups.Count
    TargetOrphanIds     = $targetOrphans.Count
    CoalescedIds        = $coalescedGroups.Count
    CoalescedOrphanIds  = $coalescedOrphans.Count
    MaxRecordsPerId     = ($groups | Measure-Object Count -Maximum).Maximum
} | Format-List

if ($orphanGroups.Count -gt 0) {
    Write-Error "Found orphan EventReceived correlation IDs: $($orphanGroups.Name -join ', ')"
    exit 1
}
