param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456"
)

$ErrorActionPreference = "Stop"

$baseUrl = $BaseUrl.TrimEnd("/")
$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Detail
    )
    $results.Add([PSCustomObject]@{
        Name = $Name
        Status = if ($Ok) { "PASS" } else { "FAIL" }
        Detail = $Detail
    })
}

function Read-ErrorBody {
    param([object]$Exception)
    try {
        if ($Exception.Response -and $Exception.Response.GetResponseStream()) {
            $stream = $Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $raw = $reader.ReadToEnd()
            if ($raw) { return $raw }
        }
    } catch {}
    return $Exception.Message
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    $url = "$baseUrl$Path"
    try {
        if ($null -ne $Body) {
            $json = if ($Body -is [string]) { $Body } else { ($Body | ConvertTo-Json -Depth 10 -Compress) }
            $resp = Invoke-RestMethod -Method $Method -Uri $url -Headers $Headers -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 45
        } else {
            $resp = Invoke-RestMethod -Method $Method -Uri $url -Headers $Headers -TimeoutSec 45
        }
        return [PSCustomObject]@{ Ok = $true; Resp = $resp; Error = ""; HttpStatus = 200 }
    } catch {
        $raw = Read-ErrorBody $_.Exception
        $resp = $null
        try {
            if ($raw) {
                $resp = $raw | ConvertFrom-Json
            }
        } catch {}
        $httpStatus = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { -1 }
        return [PSCustomObject]@{ Ok = $false; Resp = $resp; Error = $raw; HttpStatus = $httpStatus }
    }
}

function Expect-ResultCode200 {
    param(
        [string]$Name,
        [object]$Call
    )
    if ($Call.Ok -and $Call.Resp.code -eq 200) {
        Add-Result -Name $Name -Ok $true -Detail "code=200"
        return $true
    }
    $detail = if ($Call.Ok) {
        "unexpected: " + ($Call.Resp | ConvertTo-Json -Depth 8 -Compress)
    } else {
        $Call.Error
    }
    Add-Result -Name $Name -Ok $false -Detail $detail
    return $false
}

function Expect-ResultCode {
    param(
        [string]$Name,
        [object]$Call,
        [int]$Code
    )
    if ($null -ne $Call.Resp -and $Call.Resp.code -eq $Code) {
        Add-Result -Name $Name -Ok $true -Detail ("code=" + $Code)
        return $true
    }
    $detail = if ($Call.Ok) {
        "unexpected: " + ($Call.Resp | ConvertTo-Json -Depth 8 -Compress)
    } else {
        $Call.Error
    }
    Add-Result -Name $Name -Ok $false -Detail $detail
    return $false
}

function Test-SseEndpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$PayloadJson = "",
        [string]$Token = ""
    )
    $url = "$baseUrl$Path"
    $args = @("-sS", "-N", "--max-time", "10")
    if ($Token) {
        $args += @("-H", "Authorization: Bearer $Token")
    }
    $args += @("-H", "Content-Type: application/json")
    $tmpPayloadFile = $null
    if ($Method -eq "POST") {
        $args += @("-X", "POST", $url)
        if ($PayloadJson) {
            $tmpPayloadFile = [System.IO.Path]::GetTempFileName()
            Set-Content -Path $tmpPayloadFile -Value $PayloadJson -Encoding UTF8 -NoNewline
            $args += @("--data-binary", "@$tmpPayloadFile")
        }
    } else {
        $args += $url
    }

    $oldPref = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & curl.exe @args 2>&1
    } finally {
        $ErrorActionPreference = $oldPref
        if ($tmpPayloadFile -and (Test-Path $tmpPayloadFile)) {
            Remove-Item $tmpPayloadFile -Force -ErrorAction SilentlyContinue
        }
    }
    $exitCode = $LASTEXITCODE
    $text = [string]::Join("`n", ($output | ForEach-Object { "$_" }))
    $hasSseData = ($text -match "data:|event:|\[DONE\]")
    if (($exitCode -eq 0 -or $exitCode -eq 28) -and $hasSseData) {
        Add-Result -Name $Name -Ok $true -Detail ("sse ok, exit=" + $exitCode)
        return $true
    }
    $short = if ($text.Length -gt 260) { $text.Substring(0, 260) } else { $text }
    Add-Result -Name $Name -Ok $false -Detail ("sse failed, exit=" + $exitCode + ", out=" + $short)
    return $false
}

Write-Host ("Running smoke tests against {0} ..." -f $baseUrl)

# Auth flows
$adminLogin = Invoke-Api -Method POST -Path "/api/admin/auth/login" -Body @{ username = $AdminUsername; password = $AdminPassword }
if (-not (Expect-ResultCode200 -Name "Auth.AdminLogin" -Call $adminLogin)) {
    $results | Format-Table -AutoSize
    exit 1
}
$adminToken = $adminLogin.Resp.data.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

$userName = "smoke_" + [Guid]::NewGuid().ToString("N").Substring(0, 8) + "@example.com"
$userCreate = Invoke-Api -Method POST -Path "/api/admin/users" -Headers $adminHeaders -Body @{
    username = $userName
    password = "123456"
    nickname = "smoke_user"
    role = "USER"
}
Expect-ResultCode200 -Name "Admin.UserCreateForSmoke" -Call $userCreate | Out-Null

$userToken = $null
$userLogin = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{ username = $userName; password = "123456" }
if (Expect-ResultCode200 -Name "Auth.UserLogin" -Call $userLogin) {
    $userToken = $userLogin.Resp.data.token
}
if (-not $userToken) {
    $results | Format-Table -AutoSize
    exit 1
}
$userHeaders = @{ Authorization = "Bearer $userToken" }

$guestLogin = Invoke-Api -Method POST -Path "/api/auth/guest"
Expect-ResultCode200 -Name "Auth.GuestLogin" -Call $guestLogin | Out-Null
$guestToken = if ($guestLogin.Ok -and $guestLogin.Resp.code -eq 200) { $guestLogin.Resp.data.token } else { "" }
$guestHeaders = if ($guestToken) { @{ Authorization = "Bearer $guestToken" } } else { @{} }

$profileUser = Invoke-Api -Method GET -Path "/api/auth/profile" -Headers $userHeaders
Expect-ResultCode200 -Name "Auth.ProfileGetUser" -Call $profileUser | Out-Null

$profileGuest = Invoke-Api -Method GET -Path "/api/auth/profile" -Headers $guestHeaders
Expect-ResultCode200 -Name "Auth.ProfileGetGuest" -Call $profileGuest | Out-Null

$profileUpdate = Invoke-Api -Method PUT -Path "/api/auth/profile" -Headers $userHeaders -Body '{"nickname":"smoke_updated","avatar":"\ud83e\udd73"}'
Expect-ResultCode200 -Name "Auth.ProfileUpdate" -Call $profileUpdate | Out-Null

$logoutUser = Invoke-Api -Method POST -Path "/api/auth/logout" -Headers $userHeaders
Expect-ResultCode200 -Name "Auth.LogoutUser" -Call $logoutUser | Out-Null

$userLoginAgain = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{ username = $userName; password = "123456" }
if (Expect-ResultCode200 -Name "Auth.UserReLogin" -Call $userLoginAgain) {
    $userToken = $userLoginAgain.Resp.data.token
    $userHeaders = @{ Authorization = "Bearer $userToken" }
}

# Analytics
$trackVisit = Invoke-Api -Method POST -Path "/api/analytics/visit" -Body @{
    visitorId = ([Guid]::NewGuid().ToString("N").Substring(0, 10))
    path = "/smoke"
}
Expect-ResultCode200 -Name "Analytics.TrackVisit" -Call $trackVisit | Out-Null

# Question and leaderboard
$qList = Invoke-Api -Method GET -Path "/api/questions?page=1&size=10"
Expect-ResultCode200 -Name "Question.List" -Call $qList | Out-Null

$questionId = $null
if ($qList.Ok -and $qList.Resp.code -eq 200 -and $qList.Resp.data.records.Count -gt 0) {
    $questionId = [string]$qList.Resp.data.records[0].id
}
if (-not $questionId) {
$qCreateFallback = Invoke-Api -Method POST -Path "/api/admin/questions" -Headers $adminHeaders -Body @{
        title = "smoke_question"
        content = "smoke"
        standardAnswer = "smoke standard answer"
        difficulty = 1
        category = "java"
        tags = "smoke"
        status = 1
    }
    if (Expect-ResultCode200 -Name "Admin.QuestionCreateFallback" -Call $qCreateFallback) {
        $questionId = [string]$qCreateFallback.Resp.data.id
    }
}
if ($questionId) {
    $qDetail = Invoke-Api -Method GET -Path "/api/questions/$questionId"
    Expect-ResultCode200 -Name "Question.Detail" -Call $qDetail | Out-Null
}

$leaderboardGet = Invoke-Api -Method GET -Path "/api/leaderboard"
Expect-ResultCode200 -Name "Leaderboard.Get" -Call $leaderboardGet | Out-Null

if ($questionId) {
    $donePost = Invoke-Api -Method POST -Path "/api/leaderboard/question-done" -Headers $userHeaders -Body @{ questionId = [int64]$questionId }
    Expect-ResultCode200 -Name "Leaderboard.MarkDone" -Call $donePost | Out-Null
}

$doneGet = Invoke-Api -Method GET -Path "/api/leaderboard/question-done" -Headers $userHeaders
Expect-ResultCode200 -Name "Leaderboard.DoneList" -Call $doneGet | Out-Null

$gameScore = Invoke-Api -Method POST -Path "/api/leaderboard/game-score" -Headers $userHeaders -Body @{ score = 333 }
Expect-ResultCode200 -Name "Leaderboard.GameScore" -Call $gameScore | Out-Null

# Wish
$wishList = Invoke-Api -Method GET -Path "/api/wishes"
Expect-ResultCode200 -Name "Wish.List" -Call $wishList | Out-Null

$wishSubmit = Invoke-Api -Method POST -Path "/api/wishes" -Headers $userHeaders -Body @{
    content = "smoke wish " + [Guid]::NewGuid().ToString("N").Substring(0, 6)
    city = "Shanghai"
}
Expect-ResultCode200 -Name "Wish.Submit" -Call $wishSubmit | Out-Null

$approvedWishId = $null
if ($wishList.Ok -and $wishList.Resp.code -eq 200 -and $wishList.Resp.data.Count -gt 0) {
    $approvedWishId = [string]$wishList.Resp.data[0].id
}
if (-not $approvedWishId) {
    $wishCreateFallback = Invoke-Api -Method POST -Path "/api/admin/wishes" -Headers $adminHeaders -Body @{
        content = "smoke approved wish " + [Guid]::NewGuid().ToString("N").Substring(0, 6)
        city = "Shanghai"
        emotion = "hopeful"
        status = 1
    }
    if (Expect-ResultCode200 -Name "Admin.WishCreateFallbackApproved" -Call $wishCreateFallback) {
        $approvedWishId = [string]$wishCreateFallback.Resp.data.id
    }
}

if ($approvedWishId) {
    $wishListWithVisitor = Invoke-Api -Method GET -Path ("/api/wishes?visitorId=smoke_" + [Guid]::NewGuid().ToString("N").Substring(0, 8))
    if (Expect-ResultCode200 -Name "Wish.ListWithVisitor" -Call $wishListWithVisitor) {
        $hasLikeField = $false
        if ($wishListWithVisitor.Resp.data.Count -gt 0) {
            $firstWish = $wishListWithVisitor.Resp.data[0]
            $hasLikeField = ($firstWish.PSObject.Properties.Name -contains "likeCount") -and ($firstWish.PSObject.Properties.Name -contains "commentCount")
        }
        $wishStatsDetail = if ($hasLikeField) { "likeCount/commentCount ok" } else { "missing likeCount/commentCount" }
        Add-Result -Name "Wish.ListContainsStats" -Ok $hasLikeField -Detail $wishStatsDetail
    }

    $wishLikeGuest = Invoke-Api -Method POST -Path "/api/wishes/$approvedWishId/like" -Headers $guestHeaders -Body @{
        visitorId = "smoke_" + [Guid]::NewGuid().ToString("N").Substring(0, 8)
    }
    Expect-ResultCode -Name "Wish.LikeByGuestForbidden" -Call $wishLikeGuest -Code 403 | Out-Null

    $wishCommentsGet = Invoke-Api -Method GET -Path "/api/wishes/$approvedWishId/comments"
    Expect-ResultCode200 -Name "Wish.CommentList" -Call $wishCommentsGet | Out-Null

    $wishCommentGuest = Invoke-Api -Method POST -Path "/api/wishes/$approvedWishId/comments" -Headers $guestHeaders -Body @{
        content = "guest should be blocked"
    }
    Expect-ResultCode -Name "Wish.CommentGuestForbidden" -Call $wishCommentGuest -Code 403 | Out-Null

    $wishCommentUser = Invoke-Api -Method POST -Path "/api/wishes/$approvedWishId/comments" -Headers $userHeaders -Body @{
        content = "smoke comment " + [Guid]::NewGuid().ToString("N").Substring(0, 6)
    }
    Expect-ResultCode200 -Name "Wish.CommentByUser" -Call $wishCommentUser | Out-Null

    $wishCommentsGetAfter = Invoke-Api -Method GET -Path "/api/wishes/$approvedWishId/comments" -Headers $userHeaders
    if (Expect-ResultCode200 -Name "Wish.CommentListAfterCreate" -Call $wishCommentsGetAfter) {
        $hasComments = ($wishCommentsGetAfter.Resp.data.Count -gt 0)
        $wishCommentRowsDetail = if ($hasComments) { "rows>0" } else { "rows=0" }
        Add-Result -Name "Wish.CommentListHasRows" -Ok $hasComments -Detail $wishCommentRowsDetail
    }
}

# Notes
$noteList = Invoke-Api -Method GET -Path "/api/notes?page=1&size=20" -Headers $userHeaders
Expect-ResultCode200 -Name "Note.List" -Call $noteList | Out-Null

$newNote = Invoke-Api -Method POST -Path "/api/notes" -Headers $userHeaders -Body @{
    title = "smoke_note_" + (Get-Date -Format "HHmmss")
    content = "smoke note body"
    category = "smoke"
    emoji = "🧪"
}
$newNoteId = $null
if (Expect-ResultCode200 -Name "Note.Create" -Call $newNote) {
    $newNoteId = [string]$newNote.Resp.data.id
    $isPending = ($newNote.Resp.data.status -eq 0)
    $pendingDetail = if ($isPending) { "status=0" } else { "status=" + $newNote.Resp.data.status }
    Add-Result -Name "Note.CreatePending" -Ok $isPending -Detail $pendingDetail
}
if ($newNoteId) {
    $noteApprove = Invoke-Api -Method PUT -Path "/api/admin/notes/$newNoteId/status?status=1" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.NoteAuditApprove" -Call $noteApprove | Out-Null

    $noteDetail = Invoke-Api -Method GET -Path "/api/notes/$newNoteId" -Headers $userHeaders
    Expect-ResultCode200 -Name "Note.Detail" -Call $noteDetail | Out-Null

    $noteView = Invoke-Api -Method POST -Path "/api/notes/$newNoteId/view" -Headers $userHeaders
    Expect-ResultCode200 -Name "Note.ViewPlusOne" -Call $noteView | Out-Null

    $noteUpdate = Invoke-Api -Method PUT -Path "/api/notes/$newNoteId" -Headers $userHeaders -Body @{
        title = "smoke_note_updated"
        content = "smoke note body updated"
        summary = "smoke summary"
    }
    Expect-ResultCode200 -Name "Note.Update" -Call $noteUpdate | Out-Null

    $noteDelete = Invoke-Api -Method DELETE -Path "/api/notes/$newNoteId" -Headers $userHeaders
    Expect-ResultCode200 -Name "Note.Delete" -Call $noteDelete | Out-Null
}

# AI
$coachHistory = Invoke-Api -Method GET -Path "/api/ai/coach/history?limit=10" -Headers $userHeaders
Expect-ResultCode200 -Name "AI.CoachHistoryGet" -Call $coachHistory | Out-Null

Test-SseEndpoint -Name "AI.CoachChatSSE" -Method "POST" -Path "/api/ai/coach/chat" -PayloadJson '{"message":"give me java tips","topic":"java backend"}' -Token $userToken | Out-Null
if ($questionId) {
    Test-SseEndpoint -Name "AI.QuestionExplainSSE" -Method "GET" -Path "/api/ai/question/$questionId/explain" -Token "" | Out-Null
}
Test-SseEndpoint -Name "AI.ResumeAnalyzeSSE" -Method "POST" -Path "/api/ai/resume/analyze" -PayloadJson '{"resumeText":"Java backend 2 years","targetRole":"Java backend"}' -Token $userToken | Out-Null

$coachClear = Invoke-Api -Method DELETE -Path "/api/ai/coach/history" -Headers $userHeaders
Expect-ResultCode200 -Name "AI.CoachHistoryClear" -Call $coachClear | Out-Null

# Admin dashboard pages
$adminDash = Invoke-Api -Method GET -Path "/api/admin/dashboard" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.Dashboard" -Call $adminDash | Out-Null

$adminMonitor = Invoke-Api -Method GET -Path "/api/admin/system-monitor" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.SystemMonitor" -Call $adminMonitor | Out-Null

$adminVisitor = Invoke-Api -Method GET -Path "/api/admin/visitor-records?page=1&size=10" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.VisitorRecords" -Call $adminVisitor | Out-Null

# Admin user crud
$adminUsers = Invoke-Api -Method GET -Path "/api/admin/users?page=1&size=10" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.UserList" -Call $adminUsers | Out-Null

$tmpUserName = "admin_smoke_" + [Guid]::NewGuid().ToString("N").Substring(0, 6)
$adminUserCreate = Invoke-Api -Method POST -Path "/api/admin/users" -Headers $adminHeaders -Body @{
    username = $tmpUserName
    password = "123456"
    nickname = "tmp_user"
    role = "USER"
}
$tmpUserId = $null
if (Expect-ResultCode200 -Name "Admin.UserCreate" -Call $adminUserCreate) {
    $tmpUserId = [string]$adminUserCreate.Resp.data.id
}
if ($tmpUserId) {
    $adminUserDetail = Invoke-Api -Method GET -Path "/api/admin/users/$tmpUserId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.UserDetail" -Call $adminUserDetail | Out-Null

    $adminUserUpdate = Invoke-Api -Method PUT -Path "/api/admin/users/$tmpUserId" -Headers $adminHeaders -Body @{ nickname = "tmp_user_updated" }
    Expect-ResultCode200 -Name "Admin.UserUpdate" -Call $adminUserUpdate | Out-Null

    $adminUserDelete = Invoke-Api -Method DELETE -Path "/api/admin/users/$tmpUserId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.UserDelete" -Call $adminUserDelete | Out-Null
}

# Admin question crud
$adminQuestionList = Invoke-Api -Method GET -Path "/api/admin/questions?page=1&size=10" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.QuestionList" -Call $adminQuestionList | Out-Null

$adminQuestionCreate = Invoke-Api -Method POST -Path "/api/admin/questions" -Headers $adminHeaders -Body @{
    title = "admin_smoke_q_" + (Get-Date -Format "HHmmss")
    content = "admin smoke question"
    standardAnswer = "admin smoke standard answer"
    category = "java"
    difficulty = 1
    tags = "smoke"
    status = 1
}
$tmpQuestionId = $null
if (Expect-ResultCode200 -Name "Admin.QuestionCreate" -Call $adminQuestionCreate) {
    $tmpQuestionId = [string]$adminQuestionCreate.Resp.data.id
}
if ($tmpQuestionId) {
    $adminQuestionDetail = Invoke-Api -Method GET -Path "/api/admin/questions/$tmpQuestionId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.QuestionDetail" -Call $adminQuestionDetail | Out-Null

    $adminQuestionUpdate = Invoke-Api -Method PUT -Path "/api/admin/questions/$tmpQuestionId" -Headers $adminHeaders -Body @{
        title = "admin_smoke_q_updated"
        standardAnswer = "admin smoke standard answer updated"
        status = 1
    }
    Expect-ResultCode200 -Name "Admin.QuestionUpdate" -Call $adminQuestionUpdate | Out-Null

    $adminQuestionDelete = Invoke-Api -Method DELETE -Path "/api/admin/questions/$tmpQuestionId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.QuestionDelete" -Call $adminQuestionDelete | Out-Null
}

# Admin note crud
$adminNoteList = Invoke-Api -Method GET -Path "/api/admin/notes?page=1&size=10" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.NoteList" -Call $adminNoteList | Out-Null

$adminNoteCreate = Invoke-Api -Method POST -Path "/api/admin/notes" -Headers $adminHeaders -Body @{
    title = "admin_smoke_note_" + (Get-Date -Format "HHmmss")
    content = "admin smoke note"
    category = "smoke"
    emoji = "🧪"
    author = "admin"
    status = 1
}
$tmpAdminNoteId = $null
if (Expect-ResultCode200 -Name "Admin.NoteCreate" -Call $adminNoteCreate) {
    $tmpAdminNoteId = [string]$adminNoteCreate.Resp.data.id
}
if ($tmpAdminNoteId) {
    $adminNoteDetail = Invoke-Api -Method GET -Path "/api/admin/notes/$tmpAdminNoteId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.NoteDetail" -Call $adminNoteDetail | Out-Null

    $adminNoteUpdate = Invoke-Api -Method PUT -Path "/api/admin/notes/$tmpAdminNoteId" -Headers $adminHeaders -Body @{
        title = "admin_smoke_note_updated"
        content = "admin smoke note updated"
        summary = "admin smoke summary"
        status = 1
    }
    Expect-ResultCode200 -Name "Admin.NoteUpdate" -Call $adminNoteUpdate | Out-Null

    $adminNoteDelete = Invoke-Api -Method DELETE -Path "/api/admin/notes/$tmpAdminNoteId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.NoteDelete" -Call $adminNoteDelete | Out-Null
}

# Admin wish crud + audit
$adminWishList = Invoke-Api -Method GET -Path "/api/admin/wishes?page=1&size=10" -Headers $adminHeaders
Expect-ResultCode200 -Name "Admin.WishList" -Call $adminWishList | Out-Null

$adminWishCreate = Invoke-Api -Method POST -Path "/api/admin/wishes" -Headers $adminHeaders -Body @{
    content = "admin smoke wish " + (Get-Date -Format "HHmmss")
    city = "Shanghai"
    emotion = "hopeful"
    status = 0
}
$tmpWishId = $null
if (Expect-ResultCode200 -Name "Admin.WishCreate" -Call $adminWishCreate) {
    $tmpWishId = [string]$adminWishCreate.Resp.data.id
}
if ($tmpWishId) {
    $adminWishDetail = Invoke-Api -Method GET -Path "/api/admin/wishes/$tmpWishId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.WishDetail" -Call $adminWishDetail | Out-Null

    $adminWishUpdate = Invoke-Api -Method PUT -Path "/api/admin/wishes/$tmpWishId" -Headers $adminHeaders -Body @{ content = "admin smoke wish updated"; status = 0 }
    Expect-ResultCode200 -Name "Admin.WishUpdate" -Call $adminWishUpdate | Out-Null

    $adminWishAudit = Invoke-Api -Method PUT -Path "/api/admin/wishes/$tmpWishId/status?status=1" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.WishAuditStatus" -Call $adminWishAudit | Out-Null

    $adminWishDelete = Invoke-Api -Method DELETE -Path "/api/admin/wishes/$tmpWishId" -Headers $adminHeaders
    Expect-ResultCode200 -Name "Admin.WishDelete" -Call $adminWishDelete | Out-Null
}

$adminLogout = Invoke-Api -Method POST -Path "/api/auth/logout" -Headers $adminHeaders
Expect-ResultCode200 -Name "Auth.LogoutAdmin" -Call $adminLogout | Out-Null

Write-Host ""
Write-Host "Smoke test results:"
$results | Sort-Object Name | Format-Table -AutoSize

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Host ""
Write-Host ("PASS: {0}  FAIL: {1}" -f $passCount, $failCount)

if ($failCount -gt 0) {
    Write-Host ""
    Write-Host "Failed cases:"
    $results | Where-Object { $_.Status -eq "FAIL" } | Format-List
    exit 1
}

exit 0
