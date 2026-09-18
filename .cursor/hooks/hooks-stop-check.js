#!/usr/bin/env node
/**
 * Cursor "stop" hook.
 * Reads .cursor/active-task.md (the checklist the agent maintains per
 * 17-planning.mdc / 08-verification.mdc) and refuses to let the agent
 * stop silently while unchecked items remain, or while the checklist
 * is fully checked but not yet confirmed DONE via real verification.
 *
 * Place at: .cursor/hooks/stop-check.js
 * Registered in: .cursor/hooks.json
 */
const fs = require('fs');
const path = require('path');

const TASK_FILE = path.join(process.cwd(), '.cursor', 'active-task.md');
const MAX_ITERATIONS = 10;

let input = '';
process.stdin.on('data', (chunk) => { input += chunk; });
process.stdin.on('end', () => {
  let payload = {};
  try { payload = JSON.parse(input || '{}'); } catch (e) { /* malformed input, fail open */ }

  const loopCount = payload.loop_count ?? 0;

  // Safety valve: never loop forever. After MAX_ITERATIONS, stop nudging
  // and let the human take over — something is genuinely stuck.
  if (loopCount >= MAX_ITERATIONS) {
    process.stdout.write(JSON.stringify({}));
    return;
  }

  // No active checklist — nothing to enforce, allow normal stop.
  if (!fs.existsSync(TASK_FILE)) {
    process.stdout.write(JSON.stringify({}));
    return;
  }

  const content = fs.readFileSync(TASK_FILE, 'utf-8');
  const hasUnchecked = /-\s\[\s\]/.test(content);
  const isDone = /\bDONE\b/.test(content);

  if (hasUnchecked) {
    process.stdout.write(JSON.stringify({
      followup_message: `[Iteration ${loopCount + 1}/${MAX_ITERATIONS}] .cursor/active-task.md still has unchecked items. Continue working through them — don't stop until every item is checked and verified per 08-verification.mdc.`
    }));
    return;
  }

  if (!isDone) {
    process.stdout.write(JSON.stringify({
      followup_message: `[Iteration ${loopCount + 1}/${MAX_ITERATIONS}] All checklist items in .cursor/active-task.md are checked, but the task hasn't been confirmed DONE yet. Run the final verification pass (build, lint, tests, and a Playwright check for any UI change) and only write DONE at the bottom of the file once you've actually confirmed it — not before.`
    }));
    return;
  }

  // Checklist fully checked AND marked DONE after real verification — allow stop.
  process.stdout.write(JSON.stringify({}));
});
