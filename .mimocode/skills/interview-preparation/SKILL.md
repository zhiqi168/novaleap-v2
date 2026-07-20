---
name: interview-preparation
description: Generate interview-ready project documentation with technical deep-dives, design decisions, and talking points. Use when user asks for "面试", "interview", "项目亮点", "技术细节", or similar.
---

# Interview Preparation Skill

Generate comprehensive interview documentation that explains project architecture, technical decisions, and key talking points.

## When to Use

- User asks for interview preparation materials
- User wants to understand "项目亮点" (project highlights)
- User needs technical deep-dives for specific features
- User asks "面试中怎么介绍" (how to introduce in interview)

## Workflow

### Phase 1: Project Understanding

1. Read project structure and identify core modules
2. Read key technical files:
   - Configuration files (application.yml, .env)
   - Core service implementations
   - Architecture-related code (AI gateway, cache, queue)
3. Identify unique technical decisions

### Phase 2: Technical Deep-Dive Generation

For each major feature, generate:

1. **Problem Statement**: What problem does this solve?
2. **Solution Architecture**: How is it implemented?
3. **Technical Decisions**: Why these choices?
4. **Trade-offs**: What were the alternatives?
5. **Interview Talking Points**: How to explain concisely

### Phase 3: Interview Q&A Generation

Generate common interview questions and answers:

1. **Architecture Questions**
   - "Describe your project architecture"
   - "Why did you choose [technology]?"
   - "How do you handle [specific concern]?"

2. **Technical Deep-Dive Questions**
   - "How does the AI gateway work?"
   - "Explain your caching strategy"
   - "How do you handle concurrent requests?"

3. **Scenario Questions**
   - "What would you do if [problem]?"
   - "How would you scale this system?"

### Phase 4: Presentation Structure

Create a structured narrative:

1. **Elevator Pitch** (30 seconds)
2. **Technical Overview** (2 minutes)
3. **Deep-Dive on Key Feature** (5 minutes)
4. **Q&A Preparation**

## Output Format

```markdown
# Interview Preparation: [Project Name]

## Elevator Pitch
[30-second project description]

## Technical Overview
[2-minute architecture summary]

## Key Features & Technical Decisions

### [Feature 1]
**Problem**: [what problem it solves]
**Solution**: [how it's implemented]
**Why This Approach**: [technical reasoning]
**Interview Talking Point**: [concise explanation]

### [Feature 2]
...

## Common Interview Questions & Answers

### Q: [Question]
**A**: [Detailed answer with technical depth]

### Q: [Question]
**A**: [Detailed answer]

## Technical Highlights

1. [Highlight 1]: [brief description]
2. [Highlight 2]: [brief description]
3. [Highlight 3]: [brief description]

## Demo Talking Points
- [What to show during demo]
- [Key metrics to mention]
- [Technical achievements to highlight]
```

## Tips

- Use concrete numbers where possible (e.g., "handles 1000 QPS")
- Explain "why" not just "what" - interviewers want reasoning
- Prepare for follow-up questions on each topic
- Practice the 2-minute technical overview
- Know the trade-offs of your technical choices
