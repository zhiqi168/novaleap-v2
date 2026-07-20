---
name: project-analysis
description: Systematically analyze entire project structure, backend APIs, frontend pages, and identify implemented features and UI/UX issues. Use when user asks to "analyze the project", "介绍项目", "全面分析", or similar.
---

# Project Analysis Skill

Systematically analyze a full-stack project to produce a comprehensive feature inventory, architecture overview, and UI/UX assessment.

## When to Use

- User asks to analyze/介绍/分析 the entire project
- User wants to understand what features are implemented
- User needs a project overview for documentation or interview prep

## Workflow

### Phase 1: Project Structure Discovery

1. Read the project root directory to identify main modules
2. Identify tech stack from config files:
   - Backend: `pom.xml` (Java) or `package.json` (Node)
   - Frontend: `package.json`, framework config files
   - Database: SQL files, migration scripts
3. Map out directory structure:
   - Backend: controllers, services, entities, mappers, config
   - Frontend: pages, components, API layer, utils

### Phase 2: Backend Analysis

1. **API Inventory**: Read all controller files, extract endpoint definitions
   - Group by module (auth, blog, shop, AI, etc.)
   - Note HTTP methods, paths, parameters
2. **Business Logic**: Read service implementations
   - Identify core workflows (seckill, follow feed, AI chat)
   - Note design patterns (ReAct agent, cache strategies)
3. **Data Model**: Read entity classes and SQL schemas
   - Map entity relationships
   - Identify table structures

### Phase 3: Frontend Analysis

1. **Page Inventory**: Read all page files (`.wxml`, `.vue`, `.jsx`)
   - List each page with its purpose
   - Note navigation flow
2. **Component Analysis**: Read shared components
   - Identify reusable UI elements
   - Note component interactions
3. **API Integration**: Read API layer files
   - Map frontend calls to backend endpoints
   - Note data flow patterns

### Phase 4: Feature Cross-Reference

1. Match backend APIs to frontend pages
2. Identify orphaned/incomplete features
3. List fully implemented vs. partial features

### Phase 5: UI/UX Assessment

1. Review page layouts and styling
2. Identify consistency issues
3. Note accessibility or usability concerns

## Output Format

```markdown
# Project Analysis: [Project Name]

## Tech Stack
- Backend: [framework, language, key libraries]
- Frontend: [framework, UI library]
- Database: [type, ORM]
- Infrastructure: [deployment, containers]

## Architecture Overview
[High-level diagram description]

## Implemented Features

### [Module 1]
- Feature A: [description, status]
- Feature B: [description, status]

### [Module 2]
...

## API Endpoints
| Method | Path | Description | Status |
|--------|------|-------------|--------|
| GET | /api/xxx | ... | ✅ Complete |
| POST | /api/yyy | ... | ⚠️ Partial |

## UI/UX Issues
1. [Issue]: [description]
2. [Issue]: [description]

## Recommendations
- [Suggestion 1]
- [Suggestion 2]
```

## Tips

- Read files in batches to avoid context overflow
- Focus on completeness over depth for initial pass
- Note TODOs and incomplete implementations
- Cross-reference frontend routes with backend endpoints
