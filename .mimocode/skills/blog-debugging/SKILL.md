---
name: blog-debugging
description: Debug blog/content display issues including image path resolution, data not showing, caching problems, and WeChat mini program rendering. Use when user reports blog display bugs, image issues, or "我的笔记" not showing.
---

# Blog Debugging Skill

Systematically debug blog/content display issues in full-stack applications.

## When to Use

- User reports blog posts not displaying
- User reports image path issues
- User reports "我的笔记" (my notes) not showing
- User reports caching problems
- User reports WeChat mini program rendering issues

## Workflow

### Phase 1: Issue Classification

1. **Symptom**: What exactly is broken?
   - Posts not showing at all?
   - Posts showing but with wrong data?
   - Images not loading?
   - Intermittent display issues?

2. **Scope**: Where does the issue occur?
   - Specific page only?
   - Multiple pages?
   - All content?

### Phase 2: Backend Investigation

1. **Database Check**:
   - Query the database directly to verify data exists
   - Check for soft deletes, status flags, user_id mismatches
   - Verify timestamps and ordering

2. **API Response Check**:
   - Read the controller code for the endpoint
   - Verify query logic (filters, pagination, ordering)
   - Check for global filters or interceptors

3. **Entity Check**:
   - Read entity class for @TableLogic, @JsonIgnore, etc.
   - Verify field mappings match database schema
   - Check for MyBatis-Plus global configurations

### Phase 3: Frontend Investigation

1. **API Call Check**:
   - Read the API layer code
   - Verify correct endpoint and parameters
   - Check for caching headers or cache-busting

2. **Data Flow Check**:
   - Read the page/component code
   - Verify data is being received
   - Check for rendering logic issues

3. **Image Path Check**:
   - Verify image URL construction
   - Check for relative vs absolute paths
   - Test image URLs directly in browser

### Phase 4: Caching Investigation

1. **Redis Cache**:
   - Check if data is cached
   - Verify cache keys and TTL
   - Test cache invalidation

2. **Browser/Client Cache**:
   - Check for cache-busting parameters
   - Verify HTTP cache headers
   - Test in incognito mode

### Phase 5: Fix and Verify

1. Implement the fix
2. Test the specific scenario
3. Verify no regression in other pages
4. Document the root cause

## Common Issues & Solutions

### Issue: Posts not showing in "我的笔记"
**Check**:
- Database: `SELECT * FROM tb_blog WHERE user_id = <id>`
- API: Verify query filters by user_id
- Entity: Check for @TableLogic soft delete
- Interceptor: Verify no auth filtering

### Issue: Images not loading
**Check**:
- Path construction: Relative vs absolute URLs
- Base URL configuration
- Static file serving (nginx config)
- CORS headers

### Issue: Intermittent display
**Check**:
- Cache invalidation timing
- Race conditions in async operations
- TTL expiration
- CDN caching

### Issue: Data shows on one page but not another
**Check**:
- Different API endpoints?
- Different query parameters?
- Client-side filtering?
- Component state management?

## Output Format

```markdown
# Blog Debugging Report

## Issue Description
[What the user reported]

## Investigation Findings

### Backend
- Database: [findings]
- API: [findings]
- Entity: [findings]

### Frontend
- API Call: [findings]
- Data Flow: [findings]
- Rendering: [findings]

### Caching
- Redis: [findings]
- Client: [findings]

## Root Cause
[The actual cause of the issue]

## Fix Applied
[What was changed]

## Verification
[How the fix was tested]

## Prevention
[How to avoid this issue in the future]
```

## Tips

- Always verify data exists in the database first
- Check the simplest explanation before complex ones
- Test with cache disabled to isolate caching issues
- Use browser dev tools to inspect network requests
- Check for typos in field names and paths
