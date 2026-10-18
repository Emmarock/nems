# 2026 Road-Sheet Reconciliation Report

Source: `Residents 2026 Outstanding (2).xlsx` - UnIdentified + Road A-U + 1st-5th Avenue sheets (per
the estate's own column mapping: C/D/E = dues due/paid/outstanding, F/G/H = electricity
due/paid/outstanding; UnIdentified has no electricity columns).

**Status: applied to the dev database, now on its SECOND corrective pass.** The sections below
describe the first pass in detail (useful history - what was found, what judgment calls were
made) but its numbers are superseded by this summary. The first pass's matching algorithm had a
second, deeper bug beyond the 15 duplicates it already documented: generic name-similarity
scoring doesn't know that this dataset deliberately has same-base-name residents who are
different people (e.g. "...Adeyemi" and "...Adeyemi 2" are two distinct, pre-existing people; so
are "Adepoju Banke" and "Banke Adepoju", a reversed-order pair) - it silently merged a few of
these into the wrong resident. The matching script was fixed (exact-text matches now always win
over subset/reordering coincidences; a trailing "2"/"House N" is now treated as a distinguishing
marker, not noise) and dev was fully rebuilt from the original pre-reconciliation baseline using
the corrected algorithm - not patched further on top of the first pass's output, to avoid
compounding guesses on top of guesses.

## Final summary (after the v2 rebuild)

- **Residents before any of this:** 441 (432 properties)
- **Residents now:** 508 (499 properties) - net +67 genuinely new residents
- **Existing residents corrected:** 680 individual levy corrections across the 441 original
  residents (dues and/or electricity invoice/payment amounts set to their verified-correct value)
- **New residents created:** 67 (61 from the automated match + 6 reclassified from "needs
  review" after confirming via a full-roster fuzzy check that no real match exists for them)
- **Duplicate residents from the first pass's bug:** all removed as part of the full rebuild
  (not merged individually this time - every one of the 441 originals' figures was recomputed
  from scratch against the original baseline)
- **Verified:** a fuzzy name check between all 67 new residents and the 441 originals now turns
  up only one weak (0.80), clearly-unrelated match (different given name, shared surname only) -
  the same false positive already ruled out in the first pass.
- **Unparseable cells:** 6 (5 self-resolved automatically via `due = paid + outstanding`, 1 -
  Mrs Kehinde Adeogun's dues payment - fixed manually and preserved through the rebuild)

## First-pass detail (numbers below are superseded by the summary above)
- **Existing charges left untouched because the new sheet left both paid and outstanding blank:** 7

## Bug found and fixed after applying: 14 duplicate residents

**What happened:** When the same person appears on more than one sheet (a multi-property owner
listed under a second road, or the same sheet listing them twice), the reconciliation script
only let each existing resident be matched *once*. The first sheet row for that person matched
correctly; every later row for the *same* person found that resident already claimed and was
wrongly treated as a new person instead of being added on - creating a duplicate resident with
its own property, invoice and payment rows.

**How it was caught:** A fuzzy name check comparing all 81 newly-created residents against the
441 pre-existing ones (ignoring honorifics like "Mr"/"Mrs"/"Engr") turned up 14 exact-name matches
that should never have been created as new people.

**Fix applied:** each duplicate's dues/electricity invoice and payment amounts were added onto
the correct original resident, then the duplicate's payment, invoice, property and resident rows
were deleted. Verified clean afterward: resident/property counts dropped by exactly 14, and the
same fuzzy check no longer finds any exact-name duplicates.

| Duplicate (now removed) | Merged onto | Road(s) involved |
|---|---|---|
| Alhaja Tolani Raheem | resident 49 | Road C -> 1st Avenue |
| Mr & Mrs Nike Ogungbeje Aroso (x2) | resident 103 | Road E -> Road M (x2) |
| Mr Kamoru Sunday Oluseje (x2) | resident 164 | Road F -> Road U, 3rd Avenue |
| Mr Adebiyi Olusegun | resident 180 | Road G -> Road H |
| Mrs Anota Funmilayo (Abasido) | resident 184 | Road G -> Road I |
| El-Shaddai Appartment | resident 233 | 1st Avenue (two rows for the same owner) |
| Mr Jamiu Oluwanisola (Germani) | resident 250 | 1st Avenue -> Road K |
| Engr Gbolahan Awomodu | resident 289 | 4th Avenue -> Road N (x2, a 3rd/4th property) |
| Mr Salau Mukail Alabi (Actor) | resident 313 | 5th Avenue -> Road P |
| Mr Odigie Joel Ehiagwina | resident 367 | Road N -> Road M |
| Mr Shuaib Dankano Raji | resident 382 | Road N (two rows for the same owner) |
| Mrs Fabowale Olawunmi Felicia | resident 427 | Road R -> Road E |
| Mr Adebayo Olanrewaju Ajise House 2 | resident 64 | Road C -> Road U (confirmed second property, 2026-10-17) |

This last one ("House 2") wasn't caught by the exact-name check above - it genuinely differs by a
"House 2" suffix, so it wasn't a script bug, just a judgment call (same pattern as Engr Awomodu's
3 houses). Confirmed with the estate contact as a second property for the same person and merged
the same way.

## Unparseable cells (6) - resolved

| Sheet | Row | Resident | Field | Issue |
|---|---|---|---|---|
| Road E | 9 | Fabowale | dues_outstanding | negative value -200000 |
| Road K | 4 | Mr Nathaniel Anifowose | elec_outstanding | non-numeric value 'j' |
| Road M | 26 | Alhaji Akinola Lukuman | dues_outstanding | negative value -6000 |
| Road O | 6 | Mrs Kehinde Adeogun | dues_paid | non-numeric value '160000-' |
| Road O | 6 | Mrs Kehinde Adeogun | dues_outstanding | non-numeric value '#VALUE!' |
| 2nd Avenue | 8 | Mr Michael Nosakhare Iyamu | elec_outstanding | negative value -600000 |

4 of these 5 residents (Fabowale, Anifowose, Akinola Lukuman, Iyamu) self-resolved correctly -
the other half of their paid/outstanding pair was valid, and `due = paid + outstanding` filled in
the gap. Only **Mrs Kehinde Adeogun** (resident 388) had a real gap: both halves of her dues pair
failed to parse, leaving her 160,000 dues invoice with no payment recorded. Confirmed she paid in
full - a payment of 160,000 against invoice 1426 was added manually (2026-10-17).

## Existing charges NOT touched - sheet has no data for them this round (7)

These residents already have a real invoice (and a fully-paid matching payment) for this levy,
but the new sheet shows **both** paid and outstanding blank for it. A blank almost certainly means
finance didn't re-enter an already-settled line, not that the charge and payment history should
be erased - so no change was made. Flagging in case any of these actually should be zeroed out
(e.g. a refund or billing correction).

- resident 51, dues invoice 186: kept at 80,000 (sheet shows blank)
- resident 85, electricity invoice 312: kept at 150,000 (sheet shows blank)
- resident 347, electricity invoice 1274: kept at 100,000 (sheet shows blank)
- resident 397, electricity invoice 1464: kept at 300,000 (sheet shows blank)
- resident 226, electricity invoice 796: kept at 300,000 (sheet shows blank)
- resident 302, electricity invoice 1090: kept at 300,000 (sheet shows blank)
- resident 388, dues invoice 1426: resolved above (Mrs Kehinde Adeogun, now paid in full)

## DB residents not found in the new sheets (10) - left untouched

No action taken on these - just surfaced for awareness. Test/demo accounts created during this
session's other feature work (not real residents) have been excluded from this list entirely.

- **Mr Rasheed Adeyinka Sanni Sold** (resident 279, 3rd Avenue)
- **Mr Taiwo Jamiu Olamilekan** (resident 16, Road A)
- **Alhaji Abasido** (resident 42, Road B)
- **Mr Omotayo Festus House 26** (resident 429, Road B)
- **Mrs Omotayo Caroline Folashade House 24** (resident 430, Road B)
- **Pastor Ayoku Benson House 3** (resident 431, Road B)
- **Pastor Ayoku Benson House 4** (resident 432, Road B)
- **Tojoterun Villa** (resident 99, Road E)
- **Mr Awojobi-MABADEJE (Owner Adeyinka Soberu)** (resident 130, Road E)
- **Mr Alimi Benji Olugbenga** (resident 391, Road O)

## New residents created (66)

Phone number is unknown for all of these (the per-road sheets have no phone column) - stored as
`UNKNOWN`, same convention as the original import.

- Abass Saheed Omogbolahun (Road P): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Adedeke Hammed (Road J): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Adeoye Babajide Oluwaseun (Road U): dues 80,000 due / 80,000 paid, electricity 400,000 due / 400,000 paid
- Adewale Idris (Road C): dues 116,000 due / 116,000 paid, electricity 400,000 due / 400,000 paid
- Ajetunmobi Olalekan (Road Q): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Akinola Azeez opeyemi (Road C): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Alahji Ganiu Shonoiki (Tolani Appt) (Road S): dues 80,000 due / 80,000 paid, electricity 300,000 due / 300,000 paid
- Alawode Taiwo (3rd Avenue): dues 116,500 due / 116,500 paid, electricity 100,000 due / 100,000 paid
- Albert Oloyede (Road P): dues 116,500 due / 0 paid, electricity 50,000 due / 50,000 paid
- Alhaja Adeyinka Egbeyemi (Road O): dues 240,000 due / 240,000 paid, electricity 400,000 due / 400,000 paid
- Alhaja Bilikis olayinka (Road N): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Alhaji Owolabi Mogaji (Road B): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Austine Prosper (Road B): dues 116,500 due / 116,500 paid, electricity 200,000 due / 200,000 paid
- Ayodele Saida (Road Q): dues 80,000 due / 80,000 paid, electricity 80,000 due / 80,000 paid
- Azeez Olarewaju Atunde (Road M): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Balogun Niyi (Road F): dues 116,000 due / 116,000 paid, electricity 0 due / 0 paid
- Bayode John Olasehinde (Road H): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Becky House 2 (Road E): dues 115,000 due / 0 paid, electricity 0 due / 0 paid
- Benjamin Olaoluwa Folarin (Road A): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Bola Adeyeni (Road L): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Chief Ashogbon (Road T): dues 160,000 due / 0 paid, electricity 0 due / 0 paid
- Chief Sunday Ileogben (Road T): dues 80,000 due / 0 paid, electricity 0 due / 0 paid
- Col. Adebiyi O. Joseph (Road U): dues 80,000 due / 0 paid, electricity 200,000 due / 200,000 paid
- Come Rain Come shine (Road E): dues 80,000 due / 80,000 paid, electricity 400,000 due / 400,000 paid
- Damola omotayo moruf (Road O): dues 116,500 due / 0 paid, electricity 0 due / 0 paid
- Dan Kano (3rd Avenue): dues 80,000 due / 0 paid, electricity 0 due / 0 paid
- David Benjamin (Road A): dues 116,000 due / 116,000 paid, electricity 400,000 due / 400,000 paid
- Diamond Eminence Oshin (Road D): dues 106,000 due / 106,000 paid, electricity 400,000 due / 400,000 paid
- Durojaiye omotayo kolawole (Road H): dues 0 due / 0 paid, electricity 0 due / 0 paid
- Eng. Ajiboye Olugbenga Emmanuel (Road D): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Idowu Adeleke (Road M): dues 115,000 due / 115,000 paid, electricity 60,000 due / 60,000 paid
- Isiaka Azeez (4th Avenue): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Ismail Abiodun Odunfuwa (4th Avenue): dues 116,000 due / 116,000 paid, electricity 0 due / 0 paid
- Jamiu Sikiru Akinbogun (Road O): dues 116,500 due / 0 paid, electricity 0 due / 0 paid
- Johnson Stephne Ayoni (Road E): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- MR. & Mrs Soyemi (Road L): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Modinat Ajetunmobi Aduni (Road L): dues 116,000 due / 116,000 paid, electricity 0 due / 0 paid
- Mr Adeyemo Damilare Joseph (Road S): dues 80,000 due / 80,000 paid, electricity 200,000 due / 200,000 paid
- Mr Ajala Adekunle (Road R): dues 240,000 due / 0 paid, electricity 400,000 due / 0 paid
- Mr Alimi Olugbenga Benjamin (5th Avenue): dues 80,000 due / 80,000 paid, electricity 400,000 due / 400,000 paid
- Mr Fowowe Femi Adebayo (Unidentified): dues 80,000 due / 0 paid, electricity 0 due / 0 paid
- Mr Maxwell Igharo (Road U): dues 80,000 due / 80,000 paid, electricity 400,000 due / 400,000 paid
- Mr Mustapha Oladipupo (Unidentified): dues 80,000 due / 0 paid, electricity 0 due / 0 paid
- Mr Onanibosi Onayemi (Road S): dues 80,000 due / 80,000 paid, electricity 300,000 due / 300,000 paid
- Mr Osilalu Sunday Adekoya (Road R): dues 80,000 due / 0 paid, electricity 400,000 due / 0 paid
- Mr Oyenuga Oladokun Lateef (Unidentified): dues 80,000 due / 0 paid, electricity 0 due / 0 paid
- Mr Sanusi Muyideen Damilare (back of DPO) (Road R): dues 80,000 due / 80,000 paid, electricity 400,000 due / 400,000 paid
- Mr Sunday Ileogben (Road U): dues 160,000 due / 0 paid, electricity 0 due / 0 paid
- Mr. Ozioma (3rd Avenue): dues 160,000 due / 116,500 paid, electricity 400,000 due / 400,000 paid
- Mr. Thank God (1st Avenue): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Mrs Adebayo Funmilayo adesola (Road T): dues 160,000 due / 0 paid, electricity 300,000 due / 300,000 paid
- Mrs Joseph Funmilayo Janet (Road U): dues 80,000 due / 0 paid, electricity 400,000 due / 400,000 paid
- Mrs Joy Atuluku (Road S): dues 80,000 due / 0 paid, electricity 400,000 due / 400,000 paid
- Oiuwasegun Ogunbanbo (Road P): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Olajide Femi Peters (4th Avenue): dues 80,000 due / 80,000 paid, electricity 0 due / 0 paid
- Olakunle Okikiola Adaraloye (Road H): dues 116,000 due / 116,000 paid, electricity 600,000 due / 600,000 paid
- Olorunfemi Otedola (Road J): dues 116,500 due / 116,500 paid, electricity 0 due / 0 paid
- Oyenuga Baba (5th Avenue): dues 116,500 due / 0 paid, electricity 0 due / 0 paid
- Pastor Akinleye Paul (Road L): dues 80,000 due / 80,000 paid, electricity 40,000 due / 40,000 paid
- Pastor Gabriel Adeshola (Road U): dues 160,000 due / 80,000 paid, electricity 300,000 due / 300,000 paid
- Pastor Paul Salami (Road T): dues 80,000 due / 80,000 paid, electricity 300,000 due / 300,000 paid
- Prince Woke Niyi Adeyemi (Road U): dues 160,000 due / 0 paid, electricity 0 due / 0 paid
- Shoberu (Road E): dues 80,000 due / 80,000 paid, electricity 40,000 due / 40,000 paid
- Thankgod Devine Azuka (Road A): dues 116,500 due / 116,500 paid, electricity 600,000 due / 600,000 paid
- Tijani Dauda (Road O): dues 116,500 due / 0 paid, electricity 0 due / 0 paid
- Yetunde Ajasa (Road C): dues 116,500 due / 116,500 paid, electricity 400,000 due / 400,000 paid
