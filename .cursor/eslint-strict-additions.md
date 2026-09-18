# Strict lint additions — merge into existing eslint config

This is **the real enforcement layer** for `27-linting.mdc`. The Cursor rule
guides the agent's behavior; this config is what actually fails `npm run lint`
if the agent (or anyone) breaks one of these. Merge the relevant block into
each project's existing `eslint.config.js` (flat config) — backend, Next.js,
and Vite frontends each get the shared block; only frontends get the UI block.

## 1. Install (once per project)

```bash
npm i -D eslint-plugin-unused-imports eslint-plugin-import eslint-plugin-sonarjs jscpd
```

## 2. Shared block — add to every project (backend + both frontends)

```js
import unusedImports from 'eslint-plugin-unused-imports';
import importPlugin from 'eslint-plugin-import';
import sonarjs from 'eslint-plugin-sonarjs';

export default [
  // ...existing config...
  {
    plugins: { 'unused-imports': unusedImports, import: importPlugin, sonarjs },
    rules: {
      // turn off the default (warn-only, misses unused imports) in favor of the stricter plugin
      '@typescript-eslint/no-unused-vars': 'off',
      'unused-imports/no-unused-imports': 'error',
      'unused-imports/no-unused-vars': [
        'error',
        { vars: 'all', args: 'after-used', ignoreRestSiblings: true },
      ],
      'import/no-unresolved': 'error',
      'import/no-duplicates': 'error',
      'import/order': ['error', { 'newlines-between': 'always' }],
      'sonarjs/no-identical-functions': 'warn', // promote to 'error' once the codebase is clean
    },
  },
];
```

## 3. Frontend-only block — Vite and Next.js projects (not the NestJS backend)

Enforces "always import Button/Input from `src/components/ui`, never a raw
native element" at lint time, not just by convention.

```js
export default [
  // ...existing config + shared block above...
  {
    files: ['src/**/*.tsx', 'app/**/*.tsx'],
    ignores: ['src/components/ui/**'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: "JSXOpeningElement[name.name='button']",
          message: 'Use <Button> from src/components/ui instead of a raw <button>.',
        },
        {
          selector: "JSXOpeningElement[name.name='input']",
          message: 'Use <Input> from src/components/ui instead of a raw <input>.',
        },
      ],
    },
  },
];
```

Extend the same pattern (add a `selector`) for any other primitive you want
centralized — `select`, `textarea`, etc.

## 4. Cross-file duplicate detection (jscpd)

`sonarjs/no-identical-functions` mostly catches duplication within a file.
For genuine cross-file copy-paste, run `jscpd` as part of verification
(08-verification) — add a script:

```json
// package.json
{
  "scripts": {
    "check:duplicates": "jscpd src --min-lines 5 --min-tokens 50 --threshold 1"
  }
}
```

`--threshold 1` fails the command if more than 1% of the codebase is
duplicated — tune this per project once you have a baseline. Run it as part
of the same checklist item as lint in `.cursor/active-task.md`, not as a
separate forgotten step.

## 5. Rollout note

If an existing project has a lot of current violations, don't flip every
rule to `error` in one commit — that turns one task into an unrelated
codebase-wide cleanup (conflicts with 11-refactoring's scope-control rule).
Either: (a) set new rules to `error` for new/changed files only via an
`overrides`/`ignorePatterns` scoped to the files actually being touched, or
(b) do the cleanup as its own explicitly-scoped task first, then flip the
rule to `error` globally.
