param(
    [string]$BaseUrl = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"
$timestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$reportDir = Join-Path (Split-Path -Parent $PSScriptRoot) "test-reports"
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
}
$reportPath = Join-Path $reportDir ("auth-verify-test-report-{0}.md" -f (Get-Date -Format "yyyyMMdd_HHmmss"))

Add-Type -AssemblyName System.Net.Http
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromSeconds(20)

$results = New-Object System.Collections.Generic.List[Object]
$createdUsers = New-Object System.Collections.Generic.List[String]

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST", "PUT", "DELETE")]
        [string]$Method,
        [string]$Path,
        [hashtable]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $requestUri = "{0}{1}" -f $BaseUrl.TrimEnd("/"), $Path
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), $requestUri)
    foreach ($key in $Headers.Keys) {
        $request.Headers.TryAddWithoutValidation($key, [string]$Headers[$key]) | Out-Null
    }
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 10 -Compress
        $request.Content = [System.Net.Http.StringContent]::new($jsonBody, [System.Text.Encoding]::UTF8, "application/json")
    }

    try {
        $response = $httpClient.SendAsync($request).GetAwaiter().GetResult()
        $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $json = $null
        if ($raw) {
            try {
                $json = $raw | ConvertFrom-Json
            } catch {
                $json = $null
            }
        }
        return [PSCustomObject]@{
            status = [int]$response.StatusCode
            raw    = $raw
            json   = $json
        }
    } catch {
        return [PSCustomObject]@{
            status = -1
            raw    = ""
            json   = $null
            error  = $_.Exception.Message
        }
    }
}

function Redis {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    $output = & docker exec local-redis redis-cli --raw @Args
    if ($LASTEXITCODE -ne 0) {
        throw "redis-cli 执行失败: $($Args -join ' ')"
    }
    if ($null -eq $output) {
        return @()
    }
    if ($output -is [string]) {
        return @($output.TrimEnd("`r"))
    }
    return @($output | ForEach-Object { "$_".TrimEnd("`r") })
}

function MySql-Exec {
    param([string]$Sql)
    & docker exec local-mysql mysql -uroot -p123456 -D novaleap -e $Sql | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "mysql 执行失败: $Sql"
    }
}

function Add-Result {
    param(
        [string]$Id,
        [string]$Item,
        [string]$Expected,
        [string]$Actual,
        [bool]$Passed,
        [string]$Evidence
    )

    $results.Add([PSCustomObject]@{
            id       = $Id
            item     = $Item
            expected = $Expected
            actual   = $Actual
            passed   = $Passed
            evidence = $Evidence
        })
}

function Msg-Contains {
    param($Resp, [string]$Keyword)
    if ($null -eq $Resp -or $Resp.status -ne 400 -or $null -eq $Resp.json) {
        return $false
    }
    return ([string]$Resp.json.msg).Contains($Keyword)
}

try {
    Redis DEL "nova:auth:verify:send:global:1m" | Out-Null

    $health = Invoke-Api -Method "GET" -Path "/api/leaderboard"
    $envPass = ($health.status -eq 200 -and $health.json.code -eq 200)
    Add-Result -Id "ENV-01" -Item "服务健康检查" -Expected "GET /api/leaderboard 返回 200/成功" `
        -Actual ("HTTP={0}, code={1}, msg={2}" -f $health.status, $health.json.code, $health.json.msg) `
        -Passed $envPass -Evidence $health.raw

    $coolEmail = "qa_t_cool_{0}@example.com" -f $timestamp
    $coolHeaders = @{ "X-Forwarded-For" = "10.88.1.1" }
    $coolReq = @{ email = $coolEmail; type = "login" }
    $coolResp1 = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" -Body $coolReq -Headers $coolHeaders
    $casePass = ($coolResp1.status -eq 200 -and $coolResp1.json.code -eq 200)
    Add-Result -Id "SEND-01" -Item "基础发送（非存在账号 + login）" -Expected "发送成功，返回统一成功响应" `
        -Actual ("HTTP={0}, code={1}, msg={2}" -f $coolResp1.status, $coolResp1.json.code, $coolResp1.json.msg) `
        -Passed $casePass -Evidence $coolResp1.raw

    $coolResp2 = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" -Body $coolReq -Headers $coolHeaders
    $casePass = Msg-Contains -Resp $coolResp2 -Keyword "验证码发送过于频繁"
    Add-Result -Id "SEND-02" -Item "同邮箱 60 秒冷却" -Expected "第二次请求命中冷却并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $coolResp2.status, $coolResp2.json.msg) `
        -Passed $casePass -Evidence $coolResp2.raw

    $ipMinuteIp = "10.88.2.1"
    $ipMinuteOutcomes = @()
    for ($i = 1; $i -le 4; $i++) {
        $resp = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
            -Body @{ email = ("qa_t_ipm_{0}_{1}@example.com" -f $timestamp, $i); type = "login" } `
            -Headers @{ "X-Forwarded-For" = $ipMinuteIp }
        $ipMinuteOutcomes += ("#{0}:{1}/{2}" -f $i, $resp.status, $resp.json.msg)
    }
    $ipMinuteLast = $ipMinuteOutcomes[-1]
    $casePass = ($ipMinuteOutcomes[0] -like "*200/成功*" -and $ipMinuteOutcomes[1] -like "*200/成功*" -and $ipMinuteOutcomes[2] -like "*200/成功*" -and $ipMinuteLast -like "*400/*")
    Add-Result -Id "SEND-03" -Item "同 IP 60 秒 3 次限制" -Expected "前 3 次成功，第 4 次被限流" -Actual ($ipMinuteOutcomes -join " / ") -Passed $casePass -Evidence ($ipMinuteOutcomes -join "; ")

    $emailHour = "qa_t_eh_{0}@example.com" -f $timestamp
    Redis SET ("nova:auth:verify:send:email:login:1h:{0}" -f $emailHour) "5" EX "3600" | Out-Null
    $respEmailHour = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = $emailHour; type = "login" } -Headers @{ "X-Forwarded-For" = "10.88.3.1" }
    $casePass = Msg-Contains -Resp $respEmailHour -Keyword "该邮箱发送次数过多"
    Add-Result -Id "SEND-04" -Item "同邮箱 1 小时上限" -Expected "命中 1h 限制并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $respEmailHour.status, $respEmailHour.json.msg) `
        -Passed $casePass -Evidence $respEmailHour.raw

    $emailDay = "qa_t_ed_{0}@example.com" -f $timestamp
    Redis DEL ("nova:auth:verify:send:email:login:1h:{0}" -f $emailDay) | Out-Null
    Redis SET ("nova:auth:verify:send:email:login:24h:{0}" -f $emailDay) "10" EX "86400" | Out-Null
    $respEmailDay = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = $emailDay; type = "login" } -Headers @{ "X-Forwarded-For" = "10.88.3.2" }
    $casePass = Msg-Contains -Resp $respEmailDay -Keyword "该邮箱今日发送次数已达上限"
    Add-Result -Id "SEND-05" -Item "同邮箱 24 小时上限" -Expected "命中 24h 限制并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $respEmailDay.status, $respEmailDay.json.msg) `
        -Passed $casePass -Evidence $respEmailDay.raw

    $ipHour = "10.88.4.1"
    Redis SET ("nova:auth:verify:send:ip:1h:{0}" -f $ipHour) "20" EX "3600" | Out-Null
    $respIpHour = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = ("qa_t_iph_{0}@example.com" -f $timestamp); type = "login" } -Headers @{ "X-Forwarded-For" = $ipHour }
    $casePass = Msg-Contains -Resp $respIpHour -Keyword "当前网络请求过于频繁"
    Add-Result -Id "SEND-06" -Item "同 IP 1 小时上限" -Expected "命中 IP 1h 限流并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $respIpHour.status, $respIpHour.json.msg) `
        -Passed $casePass -Evidence $respIpHour.raw

    $ipDay = "10.88.5.1"
    Redis SET ("nova:auth:verify:send:ip:24h:{0}" -f $ipDay) "100" EX "86400" | Out-Null
    $respIpDay = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = ("qa_t_ipd_{0}@example.com" -f $timestamp); type = "login" } -Headers @{ "X-Forwarded-For" = $ipDay }
    $casePass = Msg-Contains -Resp $respIpDay -Keyword "当前网络今日请求次数已达上限"
    Add-Result -Id "SEND-07" -Item "同 IP 24 小时上限" -Expected "命中 IP 24h 限流并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $respIpDay.status, $respIpDay.json.msg) `
        -Passed $casePass -Evidence $respIpDay.raw

    $pairEmail = "qa_t_pair_{0}@example.com" -f $timestamp
    $pairIp = "10.88.6.1"
    Redis SET ("nova:auth:verify:send:pair:login:10m:{0}:{1}" -f $pairIp, $pairEmail) "3" EX "600" | Out-Null
    $respPair = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = $pairEmail; type = "login" } -Headers @{ "X-Forwarded-For" = $pairIp }
    $casePass = Msg-Contains -Resp $respPair -Keyword "当前操作过于频繁"
    Add-Result -Id "SEND-08" -Item "同 IP+邮箱 10 分钟上限" -Expected "命中组合限流并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $respPair.status, $respPair.json.msg) `
        -Passed $casePass -Evidence $respPair.raw

    Redis SET "nova:auth:verify:send:global:1m" "200" EX "60" | Out-Null
    $respGlobal = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = ("qa_t_global_{0}@example.com" -f $timestamp); type = "login" } -Headers @{ "X-Forwarded-For" = "10.88.7.1" }
    $casePass = Msg-Contains -Resp $respGlobal -Keyword "请求过于频繁，请稍后重试"
    Add-Result -Id "SEND-09" -Item "全局 1 分钟上限" -Expected "命中全局限流并返回 400" `
        -Actual ("HTTP={0}, msg={1}" -f $respGlobal.status, $respGlobal.json.msg) `
        -Passed $casePass -Evidence $respGlobal.raw
    Redis DEL "nova:auth:verify:send:global:1m" | Out-Null

    $existingEmail = "qa_existing_{0}@example.com" -f $timestamp
    MySql-Exec ("INSERT INTO users(username,password,nickname,role) VALUES ('{0}','x','qa-existing-{1}','USER');" -f $existingEmail, $timestamp)
    $createdUsers.Add($existingEmail) | Out-Null
    $nonExistingEmail = "qa_not_exist_{0}@example.com" -f $timestamp
    $respExist = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = $existingEmail; type = "login" } -Headers @{ "X-Forwarded-For" = "10.88.8.1" }
    $respNonExist = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" `
        -Body @{ email = $nonExistingEmail; type = "login" } -Headers @{ "X-Forwarded-For" = "10.88.8.2" }
    $sameResp = ($respExist.status -eq 200 -and $respNonExist.status -eq 200 -and $respExist.raw -eq $respNonExist.raw)
    $existCode = Redis GET ("nova:auth:verify:code:login:{0}" -f $existingEmail)
    $nonExistCode = Redis GET ("nova:auth:verify:code:login:{0}" -f $nonExistingEmail)
    $evidence = "exist={0}; nonExist={1}; existCodeLen={2}; nonExistCodeLen={3}" -f $respExist.raw, $respNonExist.raw, (($existCode -join "").Length), (($nonExistCode -join "").Length)
    Add-Result -Id "SEND-10" -Item "防枚举（登录场景）" -Expected "存在/不存在邮箱均返回同样成功文案，不暴露账号存在性" `
        -Actual ("existHTTP={0}, nonExistHTTP={1}, bodyEqual={2}" -f $respExist.status, $respNonExist.status, $sameResp) `
        -Passed $sameResp -Evidence $evidence

    $freezeEmail = "qa_t_freeze_{0}@example.com" -f $timestamp
    $freezeKey = "nova:auth:verify:code:register:{0}" -f $freezeEmail
    Redis SET $freezeKey "123456" EX "300" | Out-Null
    $freezeMsgs = @()
    for ($i = 1; $i -le 5; $i++) {
        $resp = Invoke-Api -Method "POST" -Path "/api/auth/register" -Body @{
            username     = $freezeEmail
            password     = "abc12345"
            confirmPassword = "abc12345"
            emailCode    = "000000"
            nickname     = "qa"
            consent      = $true
            turnstileToken = ""
        } -Headers @{ "X-Forwarded-For" = "10.88.9.1" }
        $freezeMsgs += ("#{0}:{1}/{2}" -f $i, $resp.status, $resp.json.msg)
    }
    $respFreeze6 = Invoke-Api -Method "POST" -Path "/api/auth/register" -Body @{
        username     = $freezeEmail
        password     = "abc12345"
        confirmPassword = "abc12345"
        emailCode    = "123456"
        nickname     = "qa"
        consent      = $true
        turnstileToken = ""
    } -Headers @{ "X-Forwarded-For" = "10.88.9.1" }
    $freezeTtl = Redis TTL ("nova:auth:verify:freeze:register:{0}" -f $freezeEmail)
    $codeAfterFreeze = Redis GET $freezeKey
    $casePass = (
        $freezeMsgs[0] -like "*400/验证码错误或已过期*" -and
        $freezeMsgs[1] -like "*400/验证码错误或已过期*" -and
        $freezeMsgs[2] -like "*400/验证码错误或已过期*" -and
        $freezeMsgs[3] -like "*400/验证码错误或已过期*" -and
        $freezeMsgs[4] -like "*400/验证码错误次数过多*" -and
        $respFreeze6.status -eq 400 -and
        ([string]$respFreeze6.json.msg).Contains("验证码错误次数过多")
    )
    Add-Result -Id "VERIFY-01" -Item "验证码错误 5 次冻结 10 分钟" -Expected "前4次普通失败，第5次冻结，第6次直接锁定" `
        -Actual (($freezeMsgs + ("#6:{0}/{1}" -f $respFreeze6.status, $respFreeze6.json.msg)) -join " | ") `
        -Passed $casePass -Evidence ("freezeTTL={0}; codeAfterFreezeLen={1}" -f ($freezeTtl -join ""), (($codeAfterFreeze -join "").Length))

    $expireEmail = "qa_t_expire_{0}@example.com" -f $timestamp
    $expireKey = "nova:auth:verify:code:register:{0}" -f $expireEmail
    Redis SET $expireKey "654321" EX "1" | Out-Null
    Start-Sleep -Seconds 2
    $respExpire = Invoke-Api -Method "POST" -Path "/api/auth/register" -Body @{
        username     = $expireEmail
        password     = "abc12345"
        confirmPassword = "abc12345"
        emailCode    = "654321"
        nickname     = "qa"
        consent      = $true
        turnstileToken = ""
    } -Headers @{ "X-Forwarded-For" = "10.88.10.1" }
    $casePass = Msg-Contains -Resp $respExpire -Keyword "验证码错误或已过期"
    Add-Result -Id "VERIFY-02" -Item "验证码过期失效" -Expected "过期后提交验证码返回无效" `
        -Actual ("HTTP={0}, msg={1}" -f $respExpire.status, $respExpire.json.msg) `
        -Passed $casePass -Evidence $respExpire.raw

    $rotateEmail = "qa_rotate_{0}@example.com" -f $timestamp
    MySql-Exec ("INSERT INTO users(username,password,nickname,role) VALUES ('{0}','x','qa-rotate-{1}','USER');" -f $rotateEmail, $timestamp)
    $createdUsers.Add($rotateEmail) | Out-Null
    $rotateKey = "nova:auth:verify:code:login:{0}" -f $rotateEmail
    Redis SET $rotateKey "111111" EX "300" | Out-Null
    Redis SET $rotateKey "222222" EX "300" | Out-Null
    $respOldCode = Invoke-Api -Method "POST" -Path "/api/auth/login" -Body @{
        username = $rotateEmail
        loginType = "code"
        emailCode = "111111"
        turnstileToken = ""
    } -Headers @{ "X-Forwarded-For" = "10.88.11.1" }
    $respNewCode = Invoke-Api -Method "POST" -Path "/api/auth/login" -Body @{
        username = $rotateEmail
        loginType = "code"
        emailCode = "222222"
        turnstileToken = ""
    } -Headers @{ "X-Forwarded-For" = "10.88.11.1" }
    $codeAfterConsume = Redis GET $rotateKey
    $casePass = (
        Msg-Contains -Resp $respOldCode -Keyword "验证码错误或已过期"
    ) -and ($respNewCode.status -eq 200 -and $respNewCode.json.code -eq 200 -and [string]::IsNullOrWhiteSpace(($codeAfterConsume -join "")))
    Add-Result -Id "VERIFY-03" -Item "新验证码覆盖旧验证码 + 成功后消费" -Expected "旧码无效，新码可登录，登录后验证码被删除" `
        -Actual ("old={0}/{1}; new={2}/{3}; keyLenAfterConsume={4}" -f $respOldCode.status, $respOldCode.json.msg, $respNewCode.status, $respNewCode.json.msg, (($codeAfterConsume -join "").Length)) `
        -Passed $casePass -Evidence ("newResp={0}" -f $respNewCode.raw)

    $turnstileEnabled = Invoke-Api -Method "POST" -Path "/api/auth/email/send-code" -Body @{
        email = ("qa_t_turnstile_{0}@example.com" -f $timestamp)
        type = "login"
        turnstileToken = ""
    } -Headers @{ "X-Forwarded-For" = "10.88.12.1" }
    $casePass = ($turnstileEnabled.status -eq 200)
    Add-Result -Id "RISK-01" -Item "风控验证器开关状态" -Expected "当前环境 Turnstile 默认关闭，不传 token 仍可通行" `
        -Actual ("HTTP={0}, msg={1}" -f $turnstileEnabled.status, $turnstileEnabled.json.msg) `
        -Passed $casePass -Evidence $turnstileEnabled.raw

} finally {
    foreach ($user in $createdUsers) {
        try {
            MySql-Exec ("DELETE FROM users WHERE username = '{0}';" -f $user)
        } catch {
        }
    }
    try { Redis DEL "nova:auth:verify:send:global:1m" | Out-Null } catch {}
}

$passCount = ($results | Where-Object { $_.passed }).Count
$totalCount = $results.Count
$failed = $results | Where-Object { -not $_.passed }

$lines = New-Object System.Collections.Generic.List[String]
$lines.Add("# NovaLeap 验证码风控测试报告")
$lines.Add("")
$lines.Add("- 生成时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$lines.Add("- 测试目标: 验证码发送限流 + 校验限流 + 防枚举 + 验证码生命周期")
$lines.Add("- 接口地址: $BaseUrl")
$lines.Add("- 测试结果: **$passCount / $totalCount 通过**")
$lines.Add("")
$lines.Add("## 用例总览")
$lines.Add("")
$lines.Add("| 用例ID | 项目 | 结果 | 期望 | 实际 |")
$lines.Add("|---|---|---|---|---|")
foreach ($r in $results) {
    $flag = if ($r.passed) { "PASS" } else { "FAIL" }
    $actual = ($r.actual -replace "\|", "¦")
    $expected = ($r.expected -replace "\|", "¦")
    $item = ($r.item -replace "\|", "¦")
    $lines.Add("| $($r.id) | $item | $flag | $expected | $actual |")
}
$lines.Add("")
$lines.Add("## 关键证据")
$lines.Add("")
foreach ($r in $results) {
    $lines.Add("### $($r.id) $($r.item)")
    $lines.Add("- 期望: $($r.expected)")
    $lines.Add("- 实际: $($r.actual)")
    $lines.Add("- 判定: $(if ($r.passed) { 'PASS' } else { 'FAIL' })")
    $lines.Add("- 证据: $($r.evidence)")
    $lines.Add("")
}

if ($failed.Count -gt 0) {
    $lines.Add("## 未通过项")
    $lines.Add("")
    foreach ($f in $failed) {
        $lines.Add("- $($f.id) $($f.item): $($f.actual)")
    }
    $lines.Add("")
}

$lines | Set-Content -Encoding UTF8 $reportPath

[PSCustomObject]@{
    ReportPath = $reportPath
    Passed     = $passCount
    Total      = $totalCount
    FailedIds  = @($failed | ForEach-Object { $_.id })
} | ConvertTo-Json -Depth 5



