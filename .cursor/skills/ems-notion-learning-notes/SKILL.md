---
name: ems-notion-learning-notes
description: >-
  Writes detailed Spring Boot learning notes into the Notion Spring Boot Notes
  database and EMS Learning Path. Use when the user asks to create, add, update,
  or capture Notion notes, learning notes, EMS notes, Spring Boot Notes, or the
  learning vault.
---

# EMS Notion learning notes

Chat replies may stay concise. **Notion pages must not.**
Write as a mentor teaching a junior who does not know the term yet.

One **concept per page** (same as the vault). Each page must be a full lesson, not a 15-line stub. Do not copy the short length of existing cards such as `@Service` or `@RequestBody` — those are the old bar.

For Notion workflow (search, fetch, create, link), also follow the workspace knowledge-capture skill.

For a good vs bad length example, see [examples.md](examples.md).

## Do this first

1. Search Notion for **Spring Boot Notes** and **EMS Learning Path**.
2. Fetch **[Template] Concept** for **section headings only** — ignore “2–3 lines.”
3. Search the vault for an existing page on the same concept. If it is thin, **expand that page**. Do not create a second short duplicate.
4. Read the EMS Java files the note is about. Use real class, method, and annotation names from this repo.

## Depth (mandatory)

Each Concept page must include **full paragraphs** (not one-liners) for:

1. Why the concept exists
2. What problem it solves in EMS
3. How Spring Boot solves it (request path, beans, annotations)
4. Where it is used in real projects
5. Internal working (numbered steps)
6. Common mistakes and why they happen
7. Best practices
8. Connection to a concept already in the vault
9. EMS code example from this repo (small, real)
10. Quick self-check — 3 questions **and a short answer under each**

Minimum: you can revise the topic in 2 weeks without reopening chat.
If a section would be one sentence, it is too short — add an example or a counterexample.

**Forbidden:** overview-only pages, bullet-only pages, dumping the chat verbatim, skipping internals or self-check answers.

## After create

- Link the new/updated pages on **EMS Learning Path** (correct module).
- Point prev/next callouts at neighboring notes.
- Set properties: `Type=Concept`, `Source=Chat`, `Related Project=employee-management-system`, `Status=Ready to Revise`, `Next Review` = today + 2 days, `Topic`, `Module`, `Order`, `Tags`.
- Return the Notion URLs in chat.
