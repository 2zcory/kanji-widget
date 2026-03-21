# Kanji Placement Test

## Purpose

Define the detailed design baseline for a lightweight Kanji placement test that supports entry into the roadmap.

This document covers:
- placement-test role
- test structure
- result contract
- relationship to the roadmap
- likely integration points

## Scope

In scope for the first slice:
- a short placement or checkpoint test
- question mix of `meaning + reading`
- a result that recommends a JLPT entry point
- `Open roadmap` as the primary result CTA
- `Start recommended batch` as the secondary result CTA

Out of scope for the first slice:
- adaptive difficulty
- official exam simulation
- mastery analytics
- remote sync or accounts
- roadmap progress calculation

## Product Direction

The placement test exists to reduce guesswork about where to enter the roadmap.

It should answer:
- where should the learner start
- whether the learner should remain on the current JLPT stage
- what the next action should be after seeing the result

The feature should stay guidance-oriented rather than diagnostic-heavy.

## Test Structure

Recommended first-slice structure:
- roughly `10-20` questions
- mixed `meaning + reading` recognition
- one compact result screen

The test should feel quick enough that a learner can take it without friction.

## Result Contract

The result should expose:
- recommended JLPT stage
- short rationale
- optional simple confidence label
- supporting Grade metadata when helpful

CTA priority:
1. `Open roadmap`
2. `Start recommended batch`

The result must include a clear note that it is guidance only.

## Relationship To The Roadmap

The placement test supports the roadmap and must stay separate from roadmap progress.

Rules:
- placement result recommends where to enter the roadmap
- roadmap progress still depends on completed Kanji
- retaking the placement test must not silently rewrite roadmap completion history

## Integration Direction

Recommended first-slice integration:
- expose the test from a roadmap entry point or nearby learning-path affordance
- keep the result screen one tap away from the recommended roadmap stage
- avoid turning `MainActivity` into a quiz dashboard

## Data Dependencies

Likely required data:
- a fixed question pool mapped to JLPT-oriented stages
- answer metadata for meaning and reading prompts
- a lightweight result-mapping rule from response pattern to JLPT recommendation

The first slice should avoid depending on:
- long-term personalized study history
- hidden scoring heuristics that cannot be explained in product terms

## Implementation Notes

Likely implementation slices:
1. define the first question pool and mapping rules
2. create a placement-test controller or repository
3. implement test UI
4. implement result UI with roadmap handoff

Keep the result mapping explicit and reviewable.

## Testing Focus

First useful checks:
- question mix stays within `meaning + reading`
- result mapping always returns a valid JLPT recommendation
- primary CTA opens the roadmap path rather than a dead end
- secondary CTA opens the recommended batch only when that batch is available
