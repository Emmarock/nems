# Nitel Estate Residents 2026 — Import Data Quality Report

- **Source:** `Residents 2026 Outstanding.xlsx` (4 of ~55 sheets used — see script docstring)
- **Residents/properties created:** 432
- **Invoices created:** 1581 (1235 fully/partially paid, 346 unpaid)
- **24/2025 Debtors skipped (not imported as a separate invoice):** 184 residents, totaling 28,770,000 — this debt is already carried over into NEW 2026 DEBTORS' own Outstanding Debt figure, so a separate arrears invoice would double-count it.

## Duplicate roster rows skipped (kept first occurrence)
33 rows — mostly "ROAD A" and part of "ROAD B" being re-listed verbatim near the end of the roster sheet.

- row 122 (ROAD E): Mr & Mrs Nike Ogungbeje Aroso
- row 186 (ROAD F): El Shaddai Villa
- row 220 (ROAD I): Alhaji Hammed Adebayo (Abasido)
- row 261 (1ST AVENUE): Alhaja Tolani Raheem
- row 298 (3RD AVENUE): Mr Kamoru Sunday Oluseje
- row 356 (ROAD K): Mr Jamiu Oluwanisola (Germani)
- row 375 (ROAD M): Mr & Mrs Nike Ogungbeje Aroso
- row 384 (ROAD M): Mr & Mrs Nike Ogungbeje Aroso
- row 398 (ROAD N): Alhaja Mariam Adebimpe Bello
- row 431 (ROAD P): Mr Salau Mukail Alabi (Actor)
- row 468 (ROAD A): Mr Udoh
- row 469 (ROAD A): Engineer Taiwo Badaru
- row 470 (ROAD A): Mr Yusuf Adetola Moruf
- row 471 (ROAD A): Mr Kabir Adeleke Gbadebo
- row 472 (ROAD A): Mr Kashimawo Oyinloye Balogun
- row 473 (ROAD A): Mr Olakunle Kuku
- row 474 (ROAD A): Mr Balogun Samuel Abayomi
- row 475 (ROAD A): Mr Yunusa Kosoko (Omo Mushin)
- row 476 (ROAD A): Surveyor Adebayo Babatunde Johnson
- row 477 (ROAD A): Surveyor Tolu
- row 478 (ROAD A): Mr Ibukun Ojo Ezekiel
- row 479 (ROAD A): Mr Azeez Shokunbi
- row 480 (ROAD A): Alh. Abubakar A. Ndako (Alfa Tapa)
- row 481 (ROAD A): Pastor Adebisi Omishakin
- row 482 (ROAD A): Mrs Salami
- row 483 (ROAD A): Mr Taiwo Jamiu Olamilekan
- row 484 (ROAD A): El Shaddai Villa
- row 488 (ROAD B): Mr Abiodun Agboola (Abbey)
- row 489 (ROAD B): Alhaji Hadi Ladan Dogo
- row 490 (ROAD B): Mr Adebero Oyewole
- row 491 (ROAD B): Mr Adewole Ogunkoya
- row 492 (ROAD B): Engr. Aanu Sanni
- row 495 (ROAD B): Mr Tochukwu A. Emmanuel

## Consolidated multi-property residents (amounts summed onto one resident)
Same name recurs under a *different* road on a levy sheet — treated as one person owning multiple plots; each sheet's amounts for them were summed rather than only keeping the first row.

### Dev levy sheet (8)
- **El Shaddai Villa** — rows 20, 186
- **Alhaja Tolani Raheem** — rows 54, 261
- **Mr & Mrs Nike Ogungbeje Aroso** — rows 110, 122, 375, 384
- **Alhaji Hammed Adebayo (Abasido)** — rows 133, 220
- **Mr Kamoru Sunday Oluseje** — rows 173, 298
- **Alhaja Mariam Adebimpe Bello** — rows 210, 398
- **Mr Jamiu Oluwanisola (Germani)** — rows 267, 356
- **Mr Salau Mukail Alabi (Actor)** — rows 335, 431

### Electricity sheet (8)
- **El Shaddai Villa** — rows 20, 186
- **Alhaja Tolani Raheem** — rows 54, 261
- **Mr & Mrs Nike Ogungbeje Aroso** — rows 110, 122, 375, 384
- **Alhaji Hammed Adebayo (Abasido)** — rows 133, 220
- **Mr Kamoru Sunday Oluseje** — rows 173, 298
- **Alhaja Mariam Adebimpe Bello** — rows 210, 398
- **Mr Jamiu Oluwanisola (Germani)** — rows 267, 356
- **Mr Salau Mukail Alabi (Actor)** — rows 335, 431

### NEW 2026 DEBTORS sheet (7)
- **El Shaddai Villa** — rows 21, 190
- **Alhaji Hammed Adebayo (Abasido)** — rows 135, 226
- **Mr Kamoru Sunday Oluseje** — rows 176, 304, 491
- **Alhaja Mariam Adebimpe Bello** — rows 214, 408
- **Mr Jamiu Oluwanisola (Germani)** — rows 273, 364
- **Mr Salau Mukail Alabi (Actor)** — rows 342, 440
- **Mr & Mrs Nike Ogungbeje Aroso** — rows 384, 393

## Unmatched rows (present on a levy sheet, not on the roster sheet — skipped entirely)

### Dev levy sheet (0)

### Electricity sheet (0)

### NEW 2026 DEBTORS sheet (42)
- row 20 (ROAD A): Benjamin Olaoluwa Folarin
- row 38 (ROAD B): Mrs Busayo Obasa
- row 55 (ROAD C): Alhaja Tolani Rahim
- row 104 (ROAD D): Ajiboye Olugbenga Emmanuel
- row 112 (ROAD E): Mr & Mrs Nike Ogungbeje Aroso (A)
- row 124 (ROAD E): Mr & Mrs Nike Ogungbeje Aroso (B)
- row 126 (ROAD E): Mr Edris Tunde Afolabi
- row 132 (ROAD E): Mrs Adenike Ogungbeje (Aroso) C
- row 137 (ROAD E): Alhaji Sunmoni Kazeem
- row 152 (ROAD E): Johnson Stephne Ayoni
- row 189 (ROAD F): Olajubu Olalekan Azeez
- row 215 (ROAD H): Alhaji Onipede Saheed 2024 debt
- row 219 (ROAD H): Bayode John Olasehinde
- row 220 (ROAD H): Adebiyi Olusegun
- row 246 (1ST AVENUE): Mr Akerele (Big Hotel) 4plot
- row 250 (1ST AVENUE): Hotel (Jackson)*** 6plot
- row 339 (4TH AVENUE): Olajide Femi Peters
- row 380 (ROAD L): Bola Adeyeni
- row 403 (ROAD M): Azeez Olarewaju Atunde
- row 469 (ROAD Q): Ayodele Saida
- row 474 (ROAD R): Mr Ajala Adekunle
- row 475 (ROAD R): Mr Osilalu Sunday Adekoya
- row 476 (ROAD R): Mr Sanusi Muyideen Damilare (back of DPO)
- row 478 (ROAD S): Mr Adeyemo Damilare Joseph
- row 479 (ROAD S): Mr Onanibosi Onayemi
- row 480 (ROAD S): Alahji Ganiu Shonoiki (Tolani Appt)
- row 481 (ROAD S): Mrs Joy Atuluku
- row 483 (ROAD T): Pastor Paul Salami
- row 484 (ROAD T): Mrs Adebayo Funmilayo adesola
- row 485 (ROAD T): Chief Sunday Ileogben
- row 486 (ROAD T): Chief Ashogbon
- row 488 (ROAD U): Adeoye Babajide Oluwaseun
- row 489 (ROAD U): Pastor Gabriel Adeshola
- row 490 (ROAD U): Prince Woke Niyi Adeyemi
- row 492 (ROAD U): Mr Sunday Ileogben
- row 493 (ROAD U): Col. Adebiyi O. Joseph
- row 494 (ROAD U): Mrs Joseph Funmilayo Janet
- row 495 (ROAD U): Mr Adebayo Olanrewaju Ajise House 2
- row 496 (ROAD U): Mr Maxwell Igharo
- row 498 (UNIDENTIFIED ROADS): Mr Oyenuga Oladokun Lateef
- row 499 (UNIDENTIFIED ROADS): Mr Fowowe Femi Adebayo
- row 500 (UNIDENTIFIED ROADS): Mr Mustapha Oladipupo

## Flagged cells (20) — unparseable/free-text values, skipped

| Row | Resident | Field | Issue |
|---|---|---|---|
| 86 | Mr Azeez Animashaun | phone | implausible phone 814199080 (9 digits) -> stored as UNKNOWN |
| 302 | Mr Hakeem O. Iyalabani | phone | implausible phone 28861811 (8 digits) -> stored as UNKNOWN |
| 335 | Mr Salau Mukail Alabi (Actor) | phone | implausible phone 812941406 (9 digits) -> stored as UNKNOWN |
| 393 | Mr Sulaimon Olanrewaju Yusuf | phone | implausible phone 912468650 (9 digits) -> stored as UNKNOWN |
| 20 | El Shaddai Villa | dev-levy.entrance | non-numeric value 'waived' — skipped, needs manual entry |
| 47 | Prof & Prof Mrs Adegbehingbe | dev-levy.entrance | non-numeric value 'old member' — skipped, needs manual entry |
| 108 | 4 Blocks Building (Engr. Tunde Abegunde) | dev-levy.debtors_24_25 | non-numeric value '695000+240k' — skipped, needs manual entry |
| 207 | Daddy Adebogun | dev-levy.entrance | non-numeric value 'old member' — skipped, needs manual entry |
| 378 | Mr Akinneye Ilesanmi House 2 | dev-levy.entrance | non-numeric value 'waived' — skipped, needs manual entry |
| 123 | Iya Gbonju | electricity.transformer2 | non-numeric value 'to balance 100k' — skipped, needs manual entry |
| 237 | Mr Ajose Sehindemi | electricity.donation | non-numeric value 'first payment never registered' — skipped, needs manual entry |
| 342 | Mr Kamorudeen Adesiyan | electricity.donation | non-numeric value 'Funmilola Ogidan' — skipped, needs manual entry |
| 378 | Mr Akinneye Ilesanmi House 2 | electricity.transformer2 | non-numeric value 'additional payment july 9, 2025' — skipped, needs manual entry |
| 380 | Mr Musibau Abdulsalam Adebayo | electricity.transformer2 | non-numeric value 'to balance 140k at month end july' — skipped, needs manual entry |
| 383 | Mr Mbam Rapheal Igwe Sparkydon House 2 | electricity.donation | non-numeric value '  ' — skipped, needs manual entry |
| 404 | Mr Oluwole Esan | electricity.donation | non-numeric value '  ' — skipped, needs manual entry |
| 410 | Mr & Mrs Adeyemi A. Febishola | electricity.donation | implausibly large value 16465462630 (likely a misplaced phone number) — skipped |
| 431 | Mr Salau Mukail Alabi (Actor) | electricity.donation | non-numeric value 'reconfirm from Alhaji' — skipped, needs manual entry |
| 455 | Okunubi Olusegun Motunrayo & Bola | electricity.donation | implausibly large value 8028955336 (likely a misplaced phone number) — skipped |
| 278 | Mr Adeshina Adegboyega Ibrahim | new-debtors.paid2026 | non-numeric value 'new' — skipped, needs manual entry |

## Assumptions made (please review)
- Every resident is treated as an **OWNER** (the sheet has no owner/tenant distinction).
- Payment **method** defaulted to **BANK_TRANSFER** and **paid date** to **2026-06-15** (sheets have no per-payment date/method).
- **Registration date** defaulted to **2026-01-01** for every resident.
- **Property type** defaulted to **DETACHED_HOUSE**, occupancy to **OCCUPIED**; house numbers were **synthesized** as `{ROAD}-{seq}` (real plot numbers are almost entirely absent from this workbook, unlike the 2025 one).
- **Security Fee** and **Yearly Devt. Levy** were merged into one levy ('Security Fee + Yearly Devt. Levy') because their sole source of truth, NEW 2026 DEBTORS, only reports them as one combined figure per resident.
- The **donation** column is identical between the dev-levy sheet and the electricity sheet for every row checked — imported once, from the electricity sheet, per the estate's own description of it as electricity-related.