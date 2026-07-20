---
name: ai-gateway-explainer
description: Explain AI gateway strategies including circuit breakers, retries, token quotas, fallback models, and rate limiting. Use when user asks about "熔断", "circuit breaker", "fallback", "token配额", "AI网关", or similar.
---

# AI Gateway Explainer Skill

Generate detailed explanations of AI gateway patterns for interview preparation and technical documentation.

## When to Use

- User asks about circuit breakers (熔断)
- User asks about fallback models (备用模型)
- User asks about token quotas (Token配额)
- User asks about AI gateway strategies (AI网关策略)
- User needs interview-ready explanations

## Workflow

### Phase 1: Code Analysis

1. Read AI gateway configuration files:
   - `AiModelGateway.java` - main gateway logic
   - `AiGatewayProperties.java` - configuration properties
   - `application.yml` / `application-prod.yml` - runtime config
2. Identify:
   - Circuit breaker thresholds and behavior
   - Retry logic and counts
   - Token quota limits (per user type)
   - Fallback model configuration
   - Rate limiting rules

### Phase 2: Pattern Explanation Generation

For each pattern, generate:

1. **What it is**: Simple definition
2. **Why it's needed**: Business/technical rationale
3. **How it works**: Technical implementation
4. **When it triggers**: Specific conditions
5. **What happens after**: Behavior post-trigger
6. **Interview explanation**: Concise talking point

### Phase 3: Scenario Documentation

Create scenario-based explanations:

1. **Normal Flow**: Happy path operation
2. **Failure Scenario**: What happens when AI fails
3. **Recovery Flow**: How system recovers
4. **Quota Exhaustion**: What happens when limits hit

## Output Format

```markdown
# AI Gateway Strategy Explanation

## Overview
[High-level description of the AI gateway architecture]

## Key Components

### 1. Circuit Breaker (熔断器)
**What**: [definition]
**Why**: [rationale]
**Triggers**: [conditions]
**Behavior**: [what happens]
**Recovery**: [how it recovers]
**Interview Point**: [concise explanation]

### 2. Retry Mechanism (重试机制)
**What**: [definition]
**Max Retries**: [number]
**Backoff Strategy**: [description]
**Interview Point**: [concise explanation]

### 3. Token Quota (Token配额)
**What**: [definition]
**Limits**:
- Guest: [limit]
- Registered User: [limit]
**Interview Point**: [concise explanation]

### 4. Fallback Model (备用模型)
**What**: [definition]
**Primary Model**: [model name]
**Fallback Model**: [model name]
**Trigger**: [when fallback activates]
**Interview Point**: [concise explanation]

### 5. Rate Limiting (限流)
**What**: [definition]
**Rules**: [specific rules]
**Interview Point**: [concise explanation]

## Flow Diagram
```
[ASCII diagram showing request flow]
```

## Interview Q&A

### Q: 什么时候触发熔断？
A: [Detailed answer]

### Q: 熔断后会做什么？
A: [Detailed answer]

### Q: 备用模型是什么？
A: [Detailed answer]

### Q: Token配额怎么划分？
A: [Detailed answer]
```

## Tips

- Use concrete numbers from the actual configuration
- Explain the "why" behind each design choice
- Prepare for follow-up questions on each component
- Know the difference between circuit breaker and rate limiting
- Understand the recovery mechanism
