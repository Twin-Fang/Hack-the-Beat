# When the Judge Is a System, the Spec Is the Requirement

**Winning retrospective — 2026 I/O Extended: Hack the Beat**

> Saturday, August 29, 2026 · GDG Campus Korea · Team 4inQ · 1st place
> First commit 13:17 → last commit 17:25 · 114 commits · 2,663 lines frontend + 1,391 lines backend
> [한국어 버전](./ko.md)

---

## A hackathon that felt like a party

We had just about three hours to build. Everyone showed up in a dress code matching Google's colors, and a DJ played live at the front of the room the entire time we were coding. It felt less like a hackathon and more like an actual party.

The theme, revealed on the day: **"Make the Party Better."**

But the defining feature of this hackathon was that **the judges were not human.** AI agents playing the roles of founder, engineer, and investor each scored three times — nine runs total — and whether the product actually worked was evaluated by **Playwright visiting the deployed link and clicking through it directly.** The final score was `theme gate multiplier × (0.34·A + 0.33·B + 0.33·C)`.

If the judge is a person, it's a problem of persuasion. If the judge is a system, it's a problem of **specification.** So before building anything, we set out to reverse-engineer the grader.

---

## 1. They don't read code — they judge the deployed link

One line in the judging explanation stuck out: the agent does not look at your repository. It **opens the deployed URL you submit and uses it.** Which means anything running beautifully on localhost is worth zero.

So we built the deployment pipeline before we had a product to deploy. Every technical choice was made on one criterion: *what is fastest right now?*

| Area | Choice | Why |
|---|---|---|
| Frontend | React + **GitHub Pages direct deploy** | No hosting setup — a push is a deploy |
| UI | **daisyUI** | No time to build components; classes alone give you buttons, cards, modals |
| Backend | **Spring Boot**, CORS fully open | Reasonable build time and the stack we knew best. No budget for learning curve |
| DB | PostgreSQL spun up on our own server | Reuse infrastructure we already ran |
| Delivery | **CI/CD through Docker Hub** | push → build → image push → container swap → health check, all automated |
| Network | Domain + TLS certificate **up front** | The frontend is HTTPS; an HTTP API gets blocked as mixed content. The judging agent is a browser too |

**By 13:37 — long before the idea was settled — the deployment URL was live.** Last-minute deploys always break, and ours did break twice that day. Because the pipeline already existed, both were fixed in minutes.

---

## 2. Reverse-engineering the grader

We structured everything the organizers had published: the session slides, the transcript of the judging explanation, the officially pre-released rubric, and the actual submission page.

**Effective weights.** Multiplying each of the 12 sub-criteria by its category weight told us where the points actually were.

| Rank | Criterion | Effective weight |
|---|---|---|
| 1 | A1 Core flow completion | **13.6%** |
| 2 | B3 Party-style viral structure | 9.9% |
| 2 | C1 Revenue model & willingness to pay | 9.9% |
| 2 | C3 Defensibility & differentiation | 9.9% |
| 5 | A3 Plan-to-implementation match | 8.5% |

**Hidden ceiling rules.** Buried in the rubric text were constraints that mattered more than the weights themselves.

- "We plan to…" counts as intent, not evidence → **capped at 5**
- A capability claimed in the document but not verifiable in the browser → **capped at 6**
- Market size without a bottom-up calculation → **capped at 4** (3 if it's only a cited TAM)

**Cloning the personas.** We wrote three documents capturing how the founder, engineer, and investor each read every criterion — what questions they ask, what they accept as evidence, what triggers a deduction.

**Hedging model uncertainty.** We had no way to know which model would run the actual judging, and optimizing for one model would be overfitting. So our grader ran across several models — from lightweight ones upward — as sub-agents, cross-checking each other. **A finding that survives every model is a real hole.**

---

## 3. Delegating idea selection to the grader

Ideas are half the game in a hackathon. But we saw it differently: **if the judge is a system following fixed criteria, the score each idea will receive is already determined.** Our job wasn't to argue for one in a meeting — it was to measure them.

While I built the grader, I asked the team to write down every idea they had as a document. We collected **seven**, then scored each one **in parallel, in independent sessions**, under the assumption that it would be implemented perfectly to spec. That removed execution ability as a variable and measured only one thing: *what is this idea's ceiling inside the judging structure?*

We ran all seven, including the ones that felt weaker by intuition. The one that came out on top was **Party Passport.**

---

## 4. Party Passport

> **"The people you talk to at a party become the stamps in your passport."**

The problem we wanted to solve: **you go to a party to meet new people, and end up talking only to the friend you came with.** You arrive, you don't know who to approach first, and you go home having spoken to no one new. A crowded room does not automatically make a good party.

- Open the host's link, **type your name, and you're in.** No app install, no signup
- Every time you meet someone new, **tag each other's QR** and your "people met" count goes up
- Collect **six badges** — First Meeting, Icebreaker, Party People, Party Master, Mission Complete, Reunion
- A **1:1 mission partner** is assigned so cliques don't form
- After the party, **secretly pick** who you'd like to meet again — revealed only to pairs who picked each other
- Badges persist in **My Vault** after the party ends

It won because of how it meshed with the rubric. It structurally satisfies "the product must not function without invitation and joint participation" — the 10-point condition for viral structure — and tag → badge → mutual match are **all behaviors verifiable in a browser.**

---

## 5. The user is Playwright

We heard about a team in a previous round whose voting button was labeled "Like" — the agent never registered it as a vote, and they lost the points. Our primary user was not a human but an automated browser.

- **Minimum depth** — core value reached within three steps from the landing page, no login or signup
- **The submitted scenario's wording matches the on-screen text character for character** — "Create party", "Copied", "Joined", "1 person met"
- **The agent can't scan a QR code** → a 4-digit "tag by code" input as a first-class fallback
- Semantic markup + `aria-label` + `data-testid`; no modals, no disabled-button gates
- **Zero console errors** on the completion path, verified on both desktop and mobile

The three-step scenario was itself a design decision. By putting "open the invite link" and "join" into steps 2 and 3, **passing the core-flow criterion (A1) simultaneously unlocked the "verified in browser" ceiling on viral structure (B3).**

---

## 6. The self-scoring loop — starting at 6.44

Once the submission materials existed, we ran an **independent self-assessment** with the three reverse-engineered personas. Average: **6.44.** Good scores don't appear on the first pass. This is where the real work started.

Two frames for reading the results:

- **Zero variance means a real hole.** Implementation match (6·6·6), target specificity (6·6·6), market size (6·6·6) — when three different lenses reach the same verdict, that's a confirmed loss of points
- **High variance means a difference in verification method.** Deployment stability split 9·5·9 because only the engineer persona hit the live API directly and found a 500 error. Since the actual judging drives a real browser, we assumed it would **converge on the engineer's verdict** and calibrated to that

### The business model — the most instructive fix

A convincing revenue model for a party product is hard to invent in a few hours. Our first idea was dating-app style: after the party ends, you express interest and pay to see who expressed interest in you.

But that's a feature **the agent cannot test.** Under the rubric, any criterion resting on a claim not verifiable in the browser hits a ceiling. So we pivoted to a **paid tier for parties above a headcount** and **implemented a mock payment screen that actually gets traversed when a party exceeds 20 people.** Re-running the grader, the score rose meaningfully. The moment the payer, the timing, and the price existed on screen, a "documented claim" became a "verified implementation."

### 15 fixes from this loop (selected)

| Finding | Action |
|---|---|
| Mutual-pick API returning 500 | Resolved schema drift — a NOT NULL column that couldn't be added to a table with existing rows |
| Capacity and payment existed only in the document | Built the screen **and enforced it server-side with a 402** (blocking direct API calls that bypassed the UI) |
| 404 on first hit of the invite link | Static-hosting fallback issue → moved the link to a root query form |
| Mission partner assigned to the inviter | Fixed logic that let the mission complete without a conversation |
| Cost basis off from the code by 400× | Recalculated including polling volume |
| Future-tense claims in the documents | Replaced "we plan to" with secured permissions and confirmed facts |
| Tags accepted with a participant ID from another party | Added per-request party ownership validation |
| The 24-hour deadline was display-only | Enforced server-side rejection after the deadline |

---

## 7. The submission form is the judge's input prompt

We pulled the submission page's HTML and read it. Team info, service URL, the three-step core flow, the plan (8,000 characters), the presentation script (4,000 characters) — **the text you type into that form becomes the judging agent's input prompt, verbatim.**

We rewrote everything with that in mind. Not a "well-written proposal" for a human panel, but **the form an agent parses best**: rubric criterion names used verbatim as section headers, every claim attached to a number or proper noun, only facts confirmable on screen, and plain, logical sentences arguing that our project is the right answer to this theme.

---

## 8. And the real result

While judging ran, there was a session where teams tried each other's services and talked. **29 people used ours for real** in that room.

Strangers scanning each other's QR codes, collecting badges. The exact scene we had set out to create was happening in front of us. More than the score, that was the real result of the day.

**First place.**

---

## What we learned

1. **When the evaluator is a system, the system's spec is the requirement document.** Converting the rubric's weights, ceilings, and gates into numbers decided our time allocation for us
2. **Idea selection is measurable too.** Facing deterministic scoring, each idea's score is already fixed. We ran seven in parallel and took the argmax — we didn't pick by intuition
3. **Clone the evaluator, and hedge model uncertainty with diversity.** A finding that multiple models agree on is a real hole
4. **When the score won't move, debrief from the grader's point of view.** Turning "a feature the agent can't test" into "a mock the agent can walk through" moved the number by itself
5. **The screen and the document are both prompts.** Just as the UX user was Playwright, the reader of the plan was an LLM
6. **Infrastructure first makes pivots cheap.** Finishing the deploy URL, server, and CI/CD before the idea was locked is what made a three-hour build possible

---

## Team

**4inQ** — Saechan Suh · Euimin Hong · Jihun Baek · Yeram Lee

Thanks to the GDG Campus Korea organizers for designing a genuinely new format, and to everyone who built the AI agent judging system. Publishing the rubric in advance let the whole team reason about the product against a shared standard.

🔗 https://twin-fang.github.io/Hack-the-Beat/
